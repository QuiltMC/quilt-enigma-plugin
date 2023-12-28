/*
 * Copyright 2022 QuiltMC
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

import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.quiltmc.enigma_plugin.util.CasingUtil;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodecIndex implements Index {
	private static final List<MethodInfo> CODEC_FIELD_METHODS = List.of(
			new MethodInfo("fieldOf", "(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;")
	);
	private static final List<MethodInfo> CODEC_OPTIONAL_FIELD_METHODS = List.of(
			new MethodInfo("optionalFieldOf", "(Ljava/lang/String;)Lcom/mojang/serialization/MapCodec;"),
			new MethodInfo("optionalFieldOf", "(Ljava/lang/String;Ljava/lang/Object;)Lcom/mojang/serialization/MapCodec;")
	);
	private static final List<String> BUILTIN_CODEC_CLASSES = List.of(
			"com/mojang/serialization/codecs/BaseMapCodec",
			"com/mojang/serialization/codecs/CompoundListCodec",
			"com/mojang/serialization/codecs/EitherCodec",
			"com/mojang/serialization/codecs/KeyDispatchCodec",
			"com/mojang/serialization/codecs/ListCodec",
			"com/mojang/serialization/codecs/OptionalFieldCodec",
			"com/mojang/serialization/codecs/PairCodec",
			"com/mojang/serialization/codecs/PairMapCodec",
			"com/mojang/serialization/codecs/PrimitiveCodec",
			"com/mojang/serialization/codecs/SimpleMapCodec",
			"com/mojang/serialization/codecs/UnboundedMapCodec",
			"com/mojang/serialization/Codec",
			"com/mojang/serialization/MapCodec"
	);
	private static final MethodInfo FOR_GETTER_METHOD = new MethodInfo("forGetter", "(Ljava/util/function/Function;)Lcom/mojang/serialization/codecs/RecordCodecBuilder;");
	private static final String FOR_GETTER_METHOD_OWNER = "com/mojang/serialization/MapCodec";
	private final Analyzer<SourceValue> analyzer;
	private final Set<String> customCodecClasses = new HashSet<>();

	private final Map<FieldEntry, String> fieldNames = new HashMap<>();
	private final Map<MethodEntry, String> methodNames = new HashMap<>();

	public CodecIndex() {
		this.analyzer = new Analyzer<>(new SourceInterpreter());
	}

	public void addCustomCodecs(List<String> customCodecClasses) {
		this.customCodecClasses.addAll(customCodecClasses);
	}

	private boolean isCodecClass(String className) {
		return BUILTIN_CODEC_CLASSES.contains(className) || this.customCodecClasses.contains(className);
	}

	private boolean isCodecFieldMethod(MethodInsnNode mInsn) {
		return this.isCodecClass(mInsn.owner)
				&& (CODEC_FIELD_METHODS.stream().anyMatch(m -> m.matches(mInsn)) || CODEC_OPTIONAL_FIELD_METHODS.stream().anyMatch(m -> m.matches(mInsn)));
	}

	@Override
	public void visitClassNode(ClassNode node) {
		for (MethodNode method : node.methods) {
			try {
				this.visitMethodNode(node, method);
			} catch (Exception e) {
				Logger.error(e, "Error visiting method " + method.name + method.desc + " in class " + node.name);
				throw new RuntimeException(e);
			}
		}
	}

	private void visitMethodNode(ClassNode parent, MethodNode node) throws AnalyzerException {
		Frame<SourceValue>[] frames = this.analyzer.analyze(parent.name, node);
		InsnList instructions = node.instructions;

		for (int i = 1; i < instructions.size() && i < frames.length - 1; i++) {
			AbstractInsnNode insn = instructions.get(i);
			if (insn instanceof MethodInsnNode mInsn && this.isCodecFieldMethod(mInsn)) {
				// Logger.info(mInsn.getOpcode() + " " + mInsn.owner + "." + mInsn.name + " " + mInsn.desc + " in " + parent.name + "." + node.name + " " + node.desc + " (" + i + ")");
				Frame<SourceValue> frame = frames[i];

				// Find the field name in the stack
				String name = null;
				for (int j = frame.getStackSize() - 1; j >= 0 && name == null; j--) { // Start searching from the top of the stack
					SourceValue value = frame.getStack(j);
					for (AbstractInsnNode insn2 : value.insns) {
						if (insn2 instanceof LdcInsnNode ldcInsn && ldcInsn.cst instanceof String s && !s.isBlank()) {
							name = s;
							// Logger.info("Found name \"" + name + "\"");
							break;
						}
					}
				}

				if (name == null || name.isEmpty()) {
					continue;
				}

				MethodInsnNode codecFieldInsn = mInsn;
				// Find a forGetter call
				for (int j = i + 1; j < instructions.size(); j++) {
					// Make sure the codec field is still in the stack
					Frame<SourceValue> frame2 = frames[j];
					if (frame2 == null) {
						continue;
					}

					int fieldIndex = -1;
					for (int k = 0; k < frame2.getStackSize(); k++) {
						SourceValue value = frame2.getStack(k);
						for (AbstractInsnNode insn2 : value.insns) {
							if (insn2 == codecFieldInsn) {
								fieldIndex = k;
								break;
							}
						}
					}

					if (fieldIndex == -1) {
						break;
					}

					AbstractInsnNode insn2 = instructions.get(j);
					if (insn2 instanceof MethodInsnNode mInsn2) {
						if (mInsn2.owner.equals(FOR_GETTER_METHOD_OWNER) && FOR_GETTER_METHOD.matches(mInsn2)) {
							// Logger.info("Found forGetter call " + mInsn2.getOpcode() + " (" + j + ")");
							AbstractInsnNode getterInsn = instructions.get(j - 1);
							if (!(getterInsn instanceof InvokeDynamicInsnNode getterInvokeInsn)) {
								continue;
							}

							// Logger.info("Found getter call " + getterInvokeInsn.bsm + " (" + (j - 1) + ")");
							this.visitGetterInvokeDynamicInsn(parent, getterInvokeInsn, name);
							break;
						} else {
							// Update the codec field insn if needed
							Type type = Type.getMethodType(mInsn2.desc);
							Type ret = type.getReturnType();
							if (ret.getSort() != Type.OBJECT || !this.isCodecClass(ret.getInternalName())) {
								continue;
							}

							boolean hasThis = mInsn2.getOpcode() != INVOKESTATIC;
							int argsSize = type.getArgumentsAndReturnSizes() >> 2;
							// Skip the first argument (automatically-added 'this' pointer) if the method doesn't have one (is static)
							int offset = (hasThis ? 0 : 1) + frame2.getStackSize() - argsSize;

							// The first argument 'this' may be our field
							if (hasThis && fieldIndex == offset) {
								// Logger.info("Found codec field call " + mInsn2.getOpcode() + " (" + j + ")");
								codecFieldInsn = mInsn2;
								continue;
							}

							Type[] args = type.getArgumentTypes();
							for (int k = 0; k < args.length; k++) {
								if (args[k].getSort() != Type.OBJECT || !this.isCodecClass(args[k].getInternalName())) {
									continue;
								}

								// Offset by one slot if there's a 'this' pointer
								if (fieldIndex == offset + k + (hasThis ? 1 : 0)) {
									// Logger.info("Found codec field call " + mInsn2.getOpcode() + " (" + j + ")");
									codecFieldInsn = mInsn2;
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	private void visitGetterInvokeDynamicInsn(ClassNode parent, InvokeDynamicInsnNode insn, String name) {
		if (insn.bsmArgs.length != 3) {
			return;
		}

		Handle getterHandle = (Handle) insn.bsmArgs[1];
		if (!getterHandle.getOwner().equals(parent.name)) {
			return;
		}

		var parentEntry = new ClassEntry(parent.name);
		String camelCaseName = CasingUtil.toCamelCase(name);
		String getterName = "get" + camelCaseName.substring(0, 1).toUpperCase() + camelCaseName.substring(1);
		if (getterHandle.getTag() == H_INVOKEVIRTUAL) {
			var entry = new MethodEntry(parentEntry, getterHandle.getName(), new MethodDescriptor(getterHandle.getDesc()));
			this.methodNames.put(entry, getterName);

			AsmUtil.getMethod(parent, getterHandle.getName(), getterHandle.getDesc())
					.flatMap(m -> AsmUtil.getFieldFromGetter(parent, m))
					.ifPresent(f -> {
						var fieldEntry = new FieldEntry(parentEntry, f.name, new TypeDescriptor(f.desc));
						this.fieldNames.put(fieldEntry, camelCaseName);
					});
		} else if (getterHandle.getTag() == H_INVOKESTATIC) {
			MethodNode method = null;
			for (MethodNode m : parent.methods) {
				if (m.name.equals(getterHandle.getName()) && m.desc.equals(getterHandle.getDesc())) {
					method = m;
					break;
				}
			}

			if (method == null) {
				return;
			}

			FieldInsnNode fieldInsn = null;
			MethodInsnNode methodInsn = null;
			InsnList instructions = method.instructions;
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn2 = instructions.get(i);
				if (insn2 instanceof FieldInsnNode fInsn && fInsn.owner.equals(parent.name)) {
					fieldInsn = fieldInsn == null ? (FieldInsnNode) insn2 : null;
				} else if (insn2 instanceof MethodInsnNode mInsn && mInsn.owner.equals(parent.name)) {
					methodInsn = methodInsn == null ? mInsn : null;
				}
			}

			if (fieldInsn != null) {
				var entry = new FieldEntry(parentEntry, fieldInsn.name, new TypeDescriptor(fieldInsn.desc));
				this.fieldNames.put(entry, camelCaseName);
			} else if (methodInsn != null) {
				var entry = new MethodEntry(parentEntry, methodInsn.name, new MethodDescriptor(methodInsn.desc));
				this.methodNames.put(entry, getterName);

				AsmUtil.getMethod(parent, methodInsn.name, methodInsn.desc)
						.flatMap(m -> AsmUtil.getFieldFromGetter(parent, m))
						.ifPresent(f -> {
							var fieldEntry = new FieldEntry(parentEntry, f.name, new TypeDescriptor(f.desc));
							this.fieldNames.put(fieldEntry, camelCaseName);
						});
			}
		}
	}

	public boolean hasField(FieldEntry field) {
		return this.fieldNames.containsKey(field);
	}

	public boolean hasMethod(MethodEntry method) {
		return this.methodNames.containsKey(method);
	}

	public String getFieldName(FieldEntry field) {
		return this.fieldNames.get(field);
	}

	public String getMethodName(MethodEntry method) {
		return this.methodNames.get(method);
	}

	public Set<FieldEntry> getFields() {
		return this.fieldNames.keySet();
	}

	public Set<MethodEntry> getMethods() {
		return this.methodNames.keySet();
	}

	@TestOnly
	protected Map<FieldEntry, String> getFieldNames() {
		return this.fieldNames;
	}

	@TestOnly
	protected Map<MethodEntry, String> getMethodNames() {
		return this.methodNames;
	}

	@TestOnly
	protected boolean hasCustomCodecs() {
		return !this.customCodecClasses.isEmpty();
	}

	@TestOnly
	protected Set<String> getCustomCodecs() {
		return this.customCodecClasses;
	}

	record MethodInfo(String name, String desc) {
		public boolean matches(MethodInsnNode insn) {
			return insn.name.equals(this.name) && insn.desc.equals(this.desc);
		}
	}
}
