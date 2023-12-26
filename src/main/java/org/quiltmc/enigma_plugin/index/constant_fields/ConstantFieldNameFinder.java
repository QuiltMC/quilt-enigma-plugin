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

package org.quiltmc.enigma_plugin.index.constant_fields;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.tinylog.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConstantFieldNameFinder implements Opcodes {
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

	public Map<FieldEntry, String> findNames(ConstantFieldIndex fieldIndex) throws Exception {
		Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
		Map<FieldEntry, String> fieldNames = new HashMap<>();
		Map<String, Set<String>> usedFieldNames = new HashMap<>();
		Map<String, Set<String>> duplicatedFieldNames = new HashMap<>();
		Map<FieldEntry, FieldEntry> linkedFields = new HashMap<>();

		for (String clazz : fieldIndex.getStaticInitializers().keySet()) {
			var initializers = fieldIndex.getStaticInitializers().get(clazz);

			for (var initializer : initializers) {
				var frames = analyzer.analyze(clazz, initializer);
				var instructions = initializer.instructions;

				for (int i = 1; i < instructions.size(); i++) {
					var insn = instructions.get(i);
					var prevInsn = insn.getPrevious();

					if (!isClassPutStatic(clazz, insn)) {
						continue;
					}

					/*
					 * We want instructions in the form of
					 * prevInsn: INVOKESTATIC ${clazz}.* (*)L*; || INVOKESPECIAL ${clazz}.<init> (*)V
					 * insn:     PUTSTATIC ${clazz}.* : L*;
					 */
					var putStatic = (FieldInsnNode) insn;
					if (!(prevInsn instanceof MethodInsnNode invokeInsn) || !invokeInsn.owner.equals(clazz)) {
						continue; // Ensure the previous instruction was an invocation of one of this class' methods
					}

					if (invokeInsn.getOpcode() != INVOKESTATIC && !isInit(invokeInsn)) {
						continue; // Ensure the invocation is either an INVOKESTATIC or a constructor invocation
					}

					// Search for a name within the frame for the invocation instruction
					var frame = frames[i - 1];
					String name = null;
					for (int j = 0; j < frame.getStackSize(); j++) {
						var value = frame.getStack(j);
						for (var stackInsn : value.insns) {
							if (stackInsn instanceof LdcInsnNode ldc && ldc.cst instanceof String constant && !constant.isBlank()) {
								name = constant;
								break;
							}
						}
					}

					if (name == null) {
						// If we couldn't find a name, try to link this field to one from another class instead
						FieldInsnNode otherFieldInsn = null;
						for (int j = 0; j < frame.getStackSize(); j++) {
							var value = frame.getStack(j);
							AbstractInsnNode lastStackInsn = null;
							for (var stackInsn : value.insns) {
								lastStackInsn = stackInsn;
								if (stackInsn instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == GETSTATIC && !fieldInsn.owner.equals(clazz)) {
									otherFieldInsn = fieldInsn;
									break;
								}
							}

							/* Search between the last stack instruction and the invocation instruction, useful for parameters passed to a constructor
							 * lastStackInsn: NEW * // Last instruction in the stack
							 *                DUP
							 *                GETSTATIC !${clazz}.* : * // Instruction we are looking for
							 *                INVOKESPECIAL *.<init> (*)V
							 * invokeInsn:    INVOKESPECIAL ${clazz}.<init> (L*;*)V
							 */
							if (otherFieldInsn == null && lastStackInsn != null) {
								var stackInsn = lastStackInsn;
								while ((stackInsn = stackInsn.getNext()) != null && stackInsn != invokeInsn) {
									if (stackInsn instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == GETSTATIC && !fieldInsn.owner.equals(clazz)) {
										otherFieldInsn = fieldInsn;
										break;
									}
								}
							}
						}

						if (otherFieldInsn != null) {
							linkedFields.put(fieldFromInsn(putStatic), fieldFromInsn(otherFieldInsn));
						}

						continue; // Done with the current putStatic
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
					boolean hasAlphabetic = false;
					for (int j = 0; j < name.length(); j++) {
						char c = name.charAt(j);

						if (isCharacterUsable(c)) {
							if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
								hasAlphabetic = true;
							}

							if (j > 0 && Character.isUpperCase(c) && Character.isLowerCase(usableName.charAt(usableName.length() - 1))) {
								usableName.append('_');
							}

							usableName.append(c);
						} else {
							usableName.append('_');
						}
					}

					if (!hasAlphabetic || usableName.isEmpty()) {
						continue;
					}

					String fieldName = usableName.toString().toUpperCase(Locale.ROOT);

					Set<String> usedNames = usedFieldNames.computeIfAbsent(clazz, k -> new HashSet<>());
					Set<String> duplicatedNames = duplicatedFieldNames.computeIfAbsent(clazz, k -> new HashSet<>());

					if (!duplicatedNames.contains(fieldName)) {
						if (!usedNames.add(fieldName)) {
							Logger.warn("Duplicate key: " + fieldName + " (" + name + ") in " + clazz);
							duplicatedNames.add(fieldName);
							usedNames.remove(fieldName);
						}
					}

					if (usedNames.contains(fieldName)) {
						fieldNames.put(fieldFromInsn(putStatic), fieldName);
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
