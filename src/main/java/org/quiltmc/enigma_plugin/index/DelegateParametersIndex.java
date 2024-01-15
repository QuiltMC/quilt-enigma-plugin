package org.quiltmc.enigma_plugin.index;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelegateParametersIndex extends Index {
	private final Map<LocalVariableEntry, LocalVariableEntry> linkedParameters = new HashMap<>();

	public DelegateParametersIndex() {
		super(Arguments.DISABLE_DELEGATE_PARAMS);
	}

	@Override
	public void visitClassNode(ClassProvider classProvider, ClassNode node) {
		for (var method : node.methods) {
			try {
				this.visitMethodNode(node, method);
			} catch (Exception e) {
				Logger.error(e, "Error visiting method " + method.name + method.desc + " in class " + node.name);
				throw new RuntimeException(e);
			}
		}
	}

	public void visitMethodNode(ClassNode classNode, MethodNode node) throws AnalyzerException {
		var methodEntry = MethodEntry.parse(classNode.name, node.name, node.desc);

		var frames = new Analyzer<>(new LocalVariableInterpreter()).analyze(classNode.name, node);
		var instructions = node.instructions;

		for (int i = 0; i < instructions.size(); i++) {
			var insn = instructions.get(i);

			if (insn instanceof MethodInsnNode methodInsn) {
				var entry = MethodEntry.parse(methodInsn.owner, methodInsn.name, methodInsn.desc);
				var frame = frames[i];
				var isStatic = methodInsn.getOpcode() == INVOKESTATIC;

				for (int j = Type.getArgumentCount(methodInsn.desc) - 1; j >= 0; j--) {
					var value = frame.pop();
					var index = j + (isStatic ? 0 : 1);

					if (value.parameter) {
						this.linkedParameters.put(new LocalVariableEntry(methodEntry, value.local), new LocalVariableEntry(entry, index));
					}
				}
			}
		}
	}

	@Override
	public void reset() {
		this.linkedParameters.clear();
	}

	public Set<LocalVariableEntry> getKeys() {
		return this.linkedParameters.keySet();
	}

	public LocalVariableEntry get(LocalVariableEntry entry) {
		return this.linkedParameters.get(entry);
	}

	public record LocalVariableValue(int size, boolean parameter, int local) implements Value {
		public LocalVariableValue(int size) {
			this(size, false, -1);
		}

		public LocalVariableValue(int size, LocalVariableValue value) {
			this(size, value.parameter, value.local);
		}

		public boolean isEmpty() {
			return this.local == -1;
		}

		@Override
		public int getSize() {
			return this.size;
		}
	}

	public static class LocalVariableInterpreter extends Interpreter<LocalVariableValue> {
		public LocalVariableInterpreter() {
			super(ASM9);
		}

		@Override
		public LocalVariableValue newValue(Type type) {
			if (type == Type.VOID_TYPE) {
				return null; // Only used in returns, must be null for void
			}

			return new LocalVariableValue(type == null ? 1 : type.getSize());
		}

		@Override
		public LocalVariableValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
			return new LocalVariableValue(type.getSize(), !isInstanceMethod || local > 0, local);
		}

		@Override
		public LocalVariableValue newEmptyValue(int local) {
			return new LocalVariableValue(1, false, local);
		}

		@Override
		public LocalVariableValue newOperation(AbstractInsnNode insn) {
			return new LocalVariableValue(switch (insn.getOpcode()) {
				case LCONST_0, LCONST_1, DCONST_0, DCONST_1 -> 2;
				case LDC -> {
					var value = ((LdcInsnNode) insn).cst;
					yield value instanceof Double || value instanceof Long ? 2 : 1;
				}
				case GETSTATIC -> Type.getType(((FieldInsnNode) insn).desc).getSize();
				default -> 1;
			});
		}

		@Override
		public LocalVariableValue copyOperation(AbstractInsnNode insn, LocalVariableValue value) {
			return value;
		}

		@Override
		public LocalVariableValue unaryOperation(AbstractInsnNode insn, LocalVariableValue value) {
			return switch (insn.getOpcode()) {
				case I2L, I2D, L2D, F2L, F2D, D2L -> new LocalVariableValue(2, value); // Casts should keep the variable they came from
				case I2F, L2I, L2F, F2I, D2I, D2F, I2B, I2C, I2S, CHECKCAST -> new LocalVariableValue(1, value);

				case LNEG, DNEG -> new LocalVariableValue(2);
				case GETFIELD -> new LocalVariableValue(Type.getType(((FieldInsnNode) insn).desc).getSize());
				default -> new LocalVariableValue(1);
			};
		}

		@Override
		public LocalVariableValue binaryOperation(AbstractInsnNode insn, LocalVariableValue value1, LocalVariableValue value2) {
			return switch (insn.getOpcode()) {
				case LALOAD, DALOAD, LADD, DADD, LSUB, DSUB, LMUL, DMUL, LDIV, DDIV, LREM, DREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> new LocalVariableValue(2);
				default -> new LocalVariableValue(1);
			};
		}

		@Override
		public LocalVariableValue ternaryOperation(AbstractInsnNode insn, LocalVariableValue value1, LocalVariableValue value2, LocalVariableValue value3) {
			return new LocalVariableValue(1);
		}

		@Override
		public LocalVariableValue naryOperation(AbstractInsnNode insn, List<? extends LocalVariableValue> values) {
			return new LocalVariableValue(switch (insn.getOpcode()) {
				case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE -> Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
				case INVOKEDYNAMIC -> Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
				default -> 1;
			});
		}

		@Override
		public void returnOperation(AbstractInsnNode insn, LocalVariableValue value, LocalVariableValue expected) {
		}

		@Override
		public LocalVariableValue merge(LocalVariableValue value1, LocalVariableValue value2) {
			return new LocalVariableValue(Math.min(value1.size, value2.size));
		}
	}
}
