/*
 * Copyright 2016, 2017, 2018, 2019 FabricMC
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

package org.quiltmc.enigmaplugin.index.enumfields;

import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.tinylog.Logger;

import java.util.*;

public class FieldNameFinder implements Opcodes {
	private static boolean isClassPutStatic(String owner, AbstractInsnNode insn) {
		return insn.getOpcode() == PUTSTATIC && ((FieldInsnNode) insn).owner.equals(owner);
	}

	private static boolean isInit(AbstractInsnNode insn) {
		return insn.getOpcode() == INVOKESPECIAL && ((MethodInsnNode) insn).name.equals("<init>");
	}

	private static boolean isCharacterUsable(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
	}

	private static FieldEntry followFieldLink(FieldEntry field, Map<FieldEntry, FieldEntry> linkedFields, Map<FieldEntry, String> names) {
		if (names.containsKey(field)) {
			return field;
		} else if (linkedFields.containsKey(field)) {
			return followFieldLink(linkedFields.get(field), linkedFields, names);
		}

		return null;
	}

	public Map<FieldEntry, String> findNames(EnumFieldsIndex enumIndex) throws Exception {
		Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
		Map<FieldEntry, String> fieldNames = new HashMap<>();
		Map<String, Set<String>> usedFieldNames = new HashMap<>();
		Map<String, Set<String>> duplicatedFieldNames = new HashMap<>();
		Map<FieldEntry, FieldEntry> linkedFields = new HashMap<>();

		for (Map.Entry<String, List<MethodNode>> entry : enumIndex.getStaticInitializers().entrySet()) {
			String owner = entry.getKey();
			Set<String> enumFields = enumIndex.getEnumFields().getOrDefault(owner, Collections.emptySet());

			for (MethodNode staticInitializer : entry.getValue()) {
				Frame<SourceValue>[] frames = analyzer.analyze(owner, staticInitializer);
				InsnList instructions = staticInitializer.instructions;

				for (int i = 1; i < instructions.size(); i++) {
					AbstractInsnNode insn1 = instructions.get(i - 1);
					AbstractInsnNode insn2 = instructions.get(i);

					if (!isClassPutStatic(owner, insn2)) {
						continue;
					}

					FieldInsnNode fInsn2 = (FieldInsnNode) insn2;
					if (!(insn1 instanceof MethodInsnNode mInsn1 && mInsn1.owner.equals(owner) || enumFields.contains(fInsn2.name + ":" + fInsn2.desc))
							|| !(insn1.getOpcode() == INVOKESTATIC || isInit(insn1))) {
						continue;
					}

					// Search for a name within the frame
					Frame<SourceValue> frame = frames[i - 1];
					String name = null;
					for (int j = 0; j < frame.getStackSize() && name == null; j++) {
						SourceValue value = frame.getStack(j);
						for (AbstractInsnNode insn : value.insns) {
							if (insn instanceof LdcInsnNode ldcInsn && ldcInsn.cst instanceof String cst && !cst.isBlank()) {
								name = cst;
								break;
							}
						}
					}

					if (name == null) {
						// Try to link this field to another one
						FieldInsnNode usedFieldInsn = null;
						for (int j = 0; j < frame.getStackSize() && usedFieldInsn == null; j++) {
							SourceValue value = frame.getStack(j);
							AbstractInsnNode stackInsn = null;
							for (AbstractInsnNode insn : value.insns) {
								stackInsn = insn;
								if (insn instanceof FieldInsnNode fInsn && fInsn.getOpcode() == GETSTATIC && !owner.equals(fInsn.owner)) {
									usedFieldInsn = fInsn;
									break;
								}
							}

							/* Search between the last stack instruction and the INVOKESPECIAL, useful for parameters passed to a constructor
							NEW com/example/Clazz // Last instruction in the stack
							DUP
							GETSTATIC com/example/Test.FIELD : I // Instruction we are looking for
							INVOKESPECIAL com/example/Clazz.<init> (I)V
							INVOKESPECIAL com/example/Test.foo (Lcom/example/Clazz;)Lcom/example/Test; // End INVOKESPECIAL
							*/
							if (usedFieldInsn == null && stackInsn != null) {
								while (stackInsn.getNext() != null && stackInsn.getNext() != insn1) {
									stackInsn = stackInsn.getNext();
									if (stackInsn instanceof FieldInsnNode fInsn && fInsn.getOpcode() == GETSTATIC && !owner.equals(fInsn.owner)) {
										usedFieldInsn = fInsn;
										break;
									}
								}
							}
						}

						if (usedFieldInsn != null) {
							linkedFields.put(fieldFromInsn(fInsn2), fieldFromInsn(usedFieldInsn));
						}

						continue;
					}

					// Remove identifier namespace
					if (name.contains(":")) {
						name = name.substring(name.lastIndexOf(":") + 1);
					}

					// Process a path
					if (name.contains("/")) {
						int separator = name.indexOf("/");
						String first = name.substring(0, separator);
						String last;

						if (name.contains(".") && name.indexOf(".") > separator) {
							last = name.substring(separator + 1, name.indexOf("."));
						} else {
							last = name.substring(separator + 1);
						}

						if (first.endsWith("s")) {
							first = first.substring(0, first.length() - 1);
						}

						name = last + "_" + first;
					}

					// Check if the name is usable, replace invalid characters, convert camel case to snake case
					StringBuilder usableName = new StringBuilder();
					boolean hasText = false;
					for (int j = 0; j < name.length(); j++) {
						char c = name.charAt(j);

						if (isCharacterUsable(c)) {
							if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
								hasText = true;
							}

							if (j > 0 && Character.isUpperCase(c) && Character.isLowerCase(usableName.charAt(usableName.length() - 1))) {
								usableName.append('_');
								usableName.append(Character.toLowerCase(c));
							} else {
								usableName.append(c);
							}
						} else {
							usableName.append('_');
						}
					}

					if (!hasText || usableName.isEmpty()) {
						continue;
					}

					String fieldName = usableName.toString().toUpperCase(Locale.ROOT);

					Set<String> usedNames = usedFieldNames.computeIfAbsent(owner, k -> new HashSet<>());
					Set<String> duplicatedNames = duplicatedFieldNames.computeIfAbsent(owner, k -> new HashSet<>());

					if (!duplicatedNames.contains(fieldName)) {
						if (!usedNames.add(fieldName)) {
							Logger.warn("Duplicate key: " + fieldName + " (" + name + ") in " + owner);
							duplicatedNames.add(fieldName);
							usedNames.remove(fieldName);
						}
					}

					if (usedNames.contains(fieldName)) {
						fieldNames.put(fieldFromInsn(fInsn2), fieldName);
					}
				}
			}
		}

		// Insert linked names
		for (FieldEntry linked : linkedFields.keySet()) {
			FieldEntry target = followFieldLink(linked, linkedFields, fieldNames);
			String name = fieldNames.get(target);

			Set<String> usedNames = usedFieldNames.computeIfAbsent(linked.getParent().getFullName(), s -> new HashSet<>());
			if (usedNames.add(name)) {
				fieldNames.put(linked, name);
			}
		}

		return fieldNames;
	}

	private static FieldEntry fieldFromInsn(FieldInsnNode insn) {
		return new FieldEntry(new ClassEntry(insn.owner), insn.name, new TypeDescriptor(insn.desc));
	}
}
