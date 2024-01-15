/*
 * Copyright 2024 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelegateParametersIndex extends Index {
	private final Map<LocalVariableEntry, LocalVariableEntry> linkedParameters = new HashMap<>();
	private final Map<LocalVariableEntry, String> parameterNames = new HashMap<>();

	public DelegateParametersIndex() {
		super(Arguments.DISABLE_DELEGATE_PARAMS);
	}

	@Override
	public void visitClassNode(ClassProvider classProvider, ClassNode node) {
		for (var method : node.methods) {
			try {
				this.visitMethodNode(classProvider, node, method);
			} catch (Exception e) {
				Logger.error(e, "Error visiting method " + method.name + method.desc + " in class " + node.name);
				throw new RuntimeException(e);
			}
		}
	}

	public void visitMethodNode(ClassProvider classProvider, ClassNode classNode, MethodNode node) throws AnalyzerException {
		var methodEntry = MethodEntry.parse(classNode.name, node.name, node.desc);

		var frames = new Analyzer<>(new LocalVariableInterpreter()).analyze(classNode.name, node);
		var instructions = node.instructions;

		for (int i = 0; i < instructions.size(); i++) {
			var insn = instructions.get(i);

			// Check INVOKE* instructions, excluding INVOKEDYNAMICs
			if (insn instanceof MethodInsnNode methodInsn) {
				var entry = MethodEntry.parse(methodInsn.owner, methodInsn.name, methodInsn.desc);
				var frame = frames[i];
				var isStatic = methodInsn.getOpcode() == INVOKESTATIC;

				var desc = Type.getMethodType(methodInsn.desc);
				var local = desc.getArgumentsAndReturnSizes() >> 2;
				local -= isStatic ? 1 : 0;

				// Check each of the arguments passed to the invocation
				for (int j = desc.getArgumentCount() - 1; j >= 0; j--) {
					var value = frame.pop();
					local -= value.getSize();

					// If one of the passed arguments is a parameter of the original method, save it
					if (value.parameter) {
						// Logger.info("{}{} {} -> {}{} {}", node.name, node.desc, value.local, methodInsn.name, methodInsn.desc, local);
						var paramEntry = new LocalVariableEntry(methodEntry, value.local);
						var targetEntry = new LocalVariableEntry(entry, local);
						this.linkedParameters.put(paramEntry, targetEntry);

						// If the argument passed was also used somewhere else, invalidate both
						// TODO

						// Try to load a variable name directly from a class file
						if (!methodInsn.owner.startsWith(classNode.name) && !classNode.name.startsWith(methodInsn.owner)) {
							var targetClass = classProvider.get(methodInsn.owner);
							if (targetClass != null) {
								var targetMethod = AsmUtil.getMethod(targetClass, methodInsn.name, methodInsn.desc);
								if (targetMethod.isEmpty() || targetMethod.get().localVariables == null) {
									continue;
								}

								var localVariables = targetMethod.get().localVariables;
								for (var localVar : localVariables) {
									if (localVar.index == local) {
										if (localVar.name != null) {
											this.parameterNames.put(paramEntry, localVar.name);
										}

										break;
									}
								}
							}
						}
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

	public String getName(LocalVariableEntry entry) {
		return this.parameterNames.get(entry);
	}

	public record LocalVariableValue(int size, boolean parameter, int local) implements Value {
		public LocalVariableValue(int size) {
			this(size, false, -1);
		}

		public LocalVariableValue(int size, LocalVariableValue value) {
			this(size, value.parameter, value.local);
		}

		@Override
		public int getSize() {
			return this.size;
		}
	}

	/**
	 * Track values as {@link LocalVariableValue local variables}.
	 */
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
				case I2L, I2D, L2D, F2D -> new LocalVariableValue(2, value); // Widening casts (automatic) should keep the variable they came from
				case I2F, L2F -> new LocalVariableValue(1, value);

				case LNEG, DNEG, F2L, D2L -> new LocalVariableValue(2);
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