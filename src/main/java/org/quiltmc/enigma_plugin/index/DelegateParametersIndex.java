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
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelegateParametersIndex extends Index {
	private final Map<LocalVariableEntry, LocalVariableEntry> linkedParameters = new HashMap<>();
	private final Map<LocalVariableEntry, Set<LocalVariableEntry>> parameterLinks = new HashMap<>();
	private final Map<LocalVariableEntry, String> parameterNames = new HashMap<>();
	private final Set<LocalVariableEntry> invalidParameters = new HashSet<>(); // Parameters used more than once

	private Set<String> classes;
	private JarIndex jarIndex;
	private EntryResolver entryResolver;

	public DelegateParametersIndex() {
		super(Arguments.DISABLE_DELEGATE_PARAMS);
	}

	private static boolean isSameMethod(ClassNode owner, MethodNode node, MethodInsnNode methodInsn) {
		return node.name.equals(methodInsn.name) && node.desc.equals(methodInsn.desc) && owner.name.equals(methodInsn.owner);
	}

	@Override
	public void setIndexingContext(Set<String> classes, JarIndex jarIndex) {
		this.classes = classes;
		this.jarIndex = jarIndex;
		this.entryResolver = new IndexEntryResolver(jarIndex);
	}

	@Override
	public void visitClassNode(ClassProvider classProvider, ClassNode node) {
		if (node.outerClass != null) {
			return; // Skip anonymous/local classes
		}

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
		if (AsmUtil.matchAccess(node, ACC_SYNTHETIC) || AsmUtil.matchAccess(node, ACC_BRIDGE)) {
			return;
		}

		// Only index root methods
		var methodEntry = MethodEntry.parse(classNode.name, node.name, node.desc);
		var resolved = this.entryResolver.resolveEntry(methodEntry, ResolutionStrategy.RESOLVE_ROOT);
		if (resolved.size() > 1 || !resolved.contains(methodEntry)) {
			return;
		}

		var hasParameterInfo = node.parameters != null && !node.parameters.isEmpty();
		var paramsByTarget = new HashMap<LocalVariableEntry, LocalVariableEntry>();

		var frames = new Analyzer<>(new LocalVariableInterpreter()).analyze(classNode.name, node);
		var instructions = node.instructions;

		for (int i = 0; i < instructions.size(); i++) {
			var insn = instructions.get(i);

			// Check INVOKE* instructions, excluding INVOKEDYNAMICs and recursive invocations
			if (insn instanceof MethodInsnNode invokedMethod && !isSameMethod(classNode, node, invokedMethod)) {
				var invokedEntry = MethodEntry.parse(invokedMethod.owner, invokedMethod.name, invokedMethod.desc);
				var frame = frames[i];
				var isStatic = invokedMethod.getOpcode() == INVOKESTATIC;

				var invokedDesc = Type.getMethodType(invokedMethod.desc);
				var local = invokedDesc.getArgumentsAndReturnSizes() >> 2;
				local -= isStatic ? 1 : 0;

				// Check each of the arguments passed to the invocation
				for (int j = invokedDesc.getArgumentCount() - 1; j >= 0; j--) {
					var value = frame.pop();
					local -= value.getSize();

					// If one of the passed arguments is a parameter of the original method, save it
					if (value.parameter) {
						// Logger.info("{}{} {} -> {}{} {}", node.name, node.desc, value.local, invokedMethod.name, invokedMethod.desc, local);
						// Skip synthetic parameters
						if (hasParameterInfo) {
							var index = AsmUtil.getLocalIndex(node, value.local);
							if (AsmUtil.matchAccess(node.parameters.get(index), ACC_SYNTHETIC)) {
								continue;
							}
						}

						// Skip invalid parameters
						var paramEntry = new LocalVariableEntry(methodEntry, value.local);
						if (this.invalidParameters.contains(paramEntry)) {
							continue;
						}

						// If another entry was linked to the same one inside this method, remove it and skip this one
						var targetEntry = new LocalVariableEntry(invokedEntry, local);
						if (paramsByTarget.containsKey(targetEntry)) {
							var otherParam = paramsByTarget.get(targetEntry);

							if (otherParam != null && !paramEntry.equals(otherParam)) {
								paramsByTarget.put(targetEntry, null);
								this.remove(otherParam);
							}

							continue;
						}

						if (this.tryLink(paramEntry, targetEntry)) {
							paramsByTarget.put(targetEntry, paramEntry);
						} else {
							continue;
						}

						// Try to load a variable name directly from an external class file
						if (!this.classes.contains(invokedMethod.owner)) {
							var targetClass = classProvider.get(invokedMethod.owner);

							if (targetClass != null) {
								var targetMethod = AsmUtil.getMethod(targetClass, invokedMethod.name, invokedMethod.desc);
								if (targetMethod.isEmpty() || targetMethod.get().localVariables == null) {
									continue;
								} else if (AsmUtil.matchAccess(targetMethod.get(), ACC_SYNTHETIC) || AsmUtil.matchAccess(targetMethod.get(), ACC_BRIDGE)) {
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

	private boolean tryLink(LocalVariableEntry paramEntry, LocalVariableEntry targetEntry) {
		if (paramEntry.equals(targetEntry)) {
			throw new IllegalArgumentException("Can't link a parameter to itself!");
		}

		if (this.linkedParameters.containsKey(paramEntry)) {
			// If the argument passed was already used somewhere else, invalidate it
			if (this.linkedParameters.get(paramEntry) != targetEntry) {
				this.invalidate(paramEntry);
			}

			return false;
		}

		this.linkedParameters.put(paramEntry, targetEntry);
		this.parameterLinks.computeIfAbsent(targetEntry, e -> new HashSet<>()).add(paramEntry);
		return true;
	}

	private void invalidate(LocalVariableEntry paramEntry) {
		this.invalidParameters.add(paramEntry);

		this.remove(paramEntry);
	}

	private void remove(LocalVariableEntry paramEntry) {
		this.parameterNames.remove(paramEntry);

		var target = this.linkedParameters.remove(paramEntry);
		if (target != null) {
			var targetLinks = this.parameterLinks.get(target);
			if (!targetLinks.isEmpty()) {
				targetLinks.remove(paramEntry);
			}
		}
	}

	@Override
	public void onIndexingEnded() {
		this.classes = null;
		this.jarIndex = null;
		this.entryResolver = null;
	}

	@Override
	public void reset() {
		this.linkedParameters.clear();
		this.parameterNames.clear();
		this.invalidParameters.clear();
	}

	public Set<LocalVariableEntry> getKeys() {
		return this.linkedParameters.keySet();
	}

	public LocalVariableEntry get(LocalVariableEntry entry) {
		return this.linkedParameters.get(entry);
	}

	public Set<LocalVariableEntry> getLinks(LocalVariableEntry entry) {
		return this.parameterLinks.getOrDefault(entry, Set.of());
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
			return new LocalVariableValue(
					switch (insn.getOpcode()) {
						case LCONST_0, LCONST_1, DCONST_0, DCONST_1 -> 2;
						case LDC -> {
							var value = ((LdcInsnNode) insn).cst;
							yield value instanceof Double || value instanceof Long ? 2 : 1;
						}
						case GETSTATIC -> Type.getType(((FieldInsnNode) insn).desc).getSize();
						default -> 1;
					}
			);
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
			return new LocalVariableValue(
					switch (insn.getOpcode()) {
						case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE -> Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
						case INVOKEDYNAMIC -> Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
						default -> 1;
					}
			);
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
