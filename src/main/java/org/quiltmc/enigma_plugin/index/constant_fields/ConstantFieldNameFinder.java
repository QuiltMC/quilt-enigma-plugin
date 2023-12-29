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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantFieldNameFinder implements Opcodes {
	private final HashMap<String, Set<String>> usedNamesByClass = new HashMap<>();
	private final HashMap<String, Set<String>> duplicatedNamesByClass = new HashMap<>();
	private final HashMap<FieldEntry, FieldEntry> linkedFields = new HashMap<>();

	private static boolean isClassPutStatic(String owner, AbstractInsnNode insn) {
		return insn.getOpcode() == PUTSTATIC && ((FieldInsnNode) insn).owner.equals(owner);
	}

	private static boolean isInit(AbstractInsnNode insn) {
		return insn.getOpcode() == INVOKESPECIAL && ((MethodInsnNode) insn).name.equals("<init>");
	}

	private static boolean isCharacterUsable(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
	}

	private static String processIdentifierPath(String name) {
		if (name == null) {
			return null;
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

		return name;
	}

	private static String stringToUpperSnakeCase(String s) {
		StringBuilder usableName = new StringBuilder();
		boolean hasAlphabetic = false;

		for (int j = 0; j < s.length(); j++) {
			char c = s.charAt(j);

			if (isCharacterUsable(c)) {
				if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
					hasAlphabetic = true;
				}

				if (j > 0 && Character.isUpperCase(c) && j < s.length() - 1 && Character.isLowerCase(s.charAt(j + 1))) {
					usableName.append('_');
				}

				usableName.append(Character.toUpperCase(c));
			} else {
				usableName.append('_');
			}
		}

		if (!hasAlphabetic) {
			return null;
		}

		return usableName.toString().toUpperCase();
	}

	private static FieldEntry fieldFromInsn(FieldInsnNode insn) {
		return new FieldEntry(new ClassEntry(insn.owner), insn.name, new TypeDescriptor(insn.desc));
	}

	private static String searchStringCstInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames) {
		int frameIndex = insns.indexOf(frameInsn);
		var frame = frames[frameIndex];

		AbstractInsnNode lastStackInsn = null;
		for (int i = 0; i < frame.getStackSize(); i++) {
			var value = frame.getStack(i);
			for (var stackInsn : value.insns) {
				if (stackInsn instanceof LdcInsnNode ldc && ldc.cst instanceof String constant && !constant.isBlank()) {
					return constant;
				} else if (stackInsn.getOpcode() == INVOKESTATIC) {
					return searchStringCstInStack(insns, stackInsn, frames);
				}

				lastStackInsn = stackInsn;
			}
		}

		if (lastStackInsn != null && lastStackInsn.getOpcode() == NEW && lastStackInsn.getNext() != null && lastStackInsn.getNext().getOpcode() == DUP) {
			// Find the last frame containing the DUP instruction
			var dup = lastStackInsn.getNext();
			int searchFrameIndex = insns.indexOf(dup) + 1;
			var searchFrame = frames[searchFrameIndex];

			while (searchFrame != null && searchFrameIndex <= frameIndex) {
				boolean contains = false;
				for (int j = 0; j < searchFrame.getStackSize(); j++) {
					if (frame.getStack(j).insns.contains(dup)) {
						contains = true;
						break;
					}
				}

				if (contains) {
					searchFrameIndex++;
					if (searchFrameIndex < frames.length) {
						searchFrame = frames[searchFrameIndex];
					} else {
						searchFrame = null;
					}
				} else {
					var insn = insns.get(searchFrameIndex - 1); // This was the last instruction with a frame with a dup
					if (insn != frameInsn) {
						return searchStringCstInStack(insns, insn, frames);
					}
				}
			}
		}

		return null;
	}

	private static FieldInsnNode searchFieldReferenceInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames, String clazz) {
		int frameIndex = insns.indexOf(frameInsn);
		var frame = frames[frameIndex];

		AbstractInsnNode lastStackInsn = null;
		for (int i = 0; i < frame.getStackSize(); i++) {
			var value = frame.getStack(i);
			for (var stackInsn : value.insns) {
				if (stackInsn instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == GETSTATIC && !fieldInsn.owner.equals(clazz)) {
					return fieldInsn;
				} else if (stackInsn.getOpcode() == INVOKESTATIC) {
					return searchFieldReferenceInStack(insns, stackInsn, frames, clazz);
				}

				lastStackInsn = stackInsn;
			}
		}

		if (lastStackInsn != null && lastStackInsn.getOpcode() == NEW && lastStackInsn.getNext() != null && lastStackInsn.getNext().getOpcode() == DUP) {
			// Find the last frame containing the DUP instruction
			var dup = lastStackInsn.getNext();
			int searchFrameIndex = insns.indexOf(dup) + 1;
			var searchFrame = frames[searchFrameIndex];

			while (searchFrame != null && searchFrameIndex <= frameIndex) {
				boolean contains = false;
				for (int j = 0; j < searchFrame.getStackSize(); j++) {
					if (frame.getStack(j).insns.contains(dup)) {
						contains = true;
						break;
					}
				}

				if (contains) {
					searchFrameIndex++;
					if (searchFrameIndex < frames.length) {
						searchFrame = frames[searchFrameIndex];
					} else {
						searchFrame = null;
					}
				} else {
					var insn = insns.get(searchFrameIndex - 1); // This was the last instruction with a frame with a dup
					if (insn != frameInsn) {
						return searchFieldReferenceInStack(insns, insn, frames, clazz);
					}
				}
			}
		}

		return null;
	}

	private FieldEntry followFieldLink(FieldEntry field, Map<FieldEntry, String> names) {
		if (names.containsKey(field)) {
			return field;
		} else if (this.linkedFields.containsKey(field)) {
			return this.followFieldLink(this.linkedFields.get(field), names);
		}

		return null;
	}

	private void clear() {
		this.usedNamesByClass.clear();
		this.duplicatedNamesByClass.clear();
		this.linkedFields.clear();
	}

	public Map<FieldEntry, String> findNames(ConstantFieldIndex fieldIndex) throws Exception {
		this.clear();

		Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
		Map<FieldEntry, String> fieldNames = new HashMap<>();

		for (String clazz : fieldIndex.getStaticInitializers().keySet()) {
			var initializers = fieldIndex.getStaticInitializers().get(clazz);

			this.findNamesInInitializers(clazz, initializers, analyzer, fieldNames);
		}

		// Insert linked names
		for (FieldEntry linked : this.linkedFields.keySet()) {
			FieldEntry target = this.followFieldLink(linked, fieldNames);
			String name = fieldNames.get(target);

			String clazz = linked.getParent().getFullName();
			Set<String> usedNames = this.usedNamesByClass.computeIfAbsent(clazz, s -> new HashSet<>());
			Set<String> duplicatedNames = this.duplicatedNamesByClass.computeIfAbsent(clazz, s -> new HashSet<>());
			if (!duplicatedNames.contains(name) && usedNames.add(name)) {
				fieldNames.put(linked, name);
			} else {
				duplicatedNames.add(name);
				Logger.warn("Duplicate name \"{}\" for field {}, linked to {}", name, linked, target);
			}
		}

		return fieldNames;
	}

	private void findNamesInInitializers(String clazz, List<MethodNode> initializers, Analyzer<SourceValue> analyzer, Map<FieldEntry, String> fieldNames) throws AnalyzerException {
		var usedNames = this.usedNamesByClass.computeIfAbsent(clazz, s -> new HashSet<>());
		var duplicatedNames = this.duplicatedNamesByClass.computeIfAbsent(clazz, s -> new HashSet<>());

		for (var initializer : initializers) {
			var frames = analyzer.analyze(clazz, initializer);
			var instructions = initializer.instructions;

			for (int i = 1; i < instructions.size(); i++) {
				var insn = instructions.get(i);
				var prevInsn = insn.getPrevious();

				/*
				 * We want instructions in the form of
				 * prevInsn: INVOKESTATIC ${clazz}.* (*)L*; || INVOKESPECIAL ${clazz}.<init> (*)V
				 * insn:     PUTSTATIC ${clazz}.* : L*;
				 */
				if (!isClassPutStatic(clazz, insn)) {
					continue; // Ensure the current instruction is a PUTSTATIC for one of this class' fields
				}

				var putStatic = (FieldInsnNode) insn;
				if (!(prevInsn instanceof MethodInsnNode invokeInsn) || !invokeInsn.owner.equals(clazz)) {
					continue; // Ensure the previous instruction is an invocation of one of this class' methods
				}

				if (invokeInsn.getOpcode() != INVOKESTATIC && !isInit(invokeInsn)) {
					continue; // Ensure the invocation is either an INVOKESTATIC or a constructor invocation
				}

				// Search for a name within the frame for the invocation instruction
				String name = searchStringCstInStack(instructions, invokeInsn, frames);

				FieldEntry fieldEntry = fieldFromInsn(putStatic);
				if (name == null) {
					// If we couldn't find a name, try to link this field to one from another class instead
					FieldInsnNode otherFieldInsn = searchFieldReferenceInStack(instructions, invokeInsn, frames, clazz);

					if (otherFieldInsn != null) {
						this.linkedFields.put(fieldEntry, fieldFromInsn(otherFieldInsn));
					}

					continue; // Done with the current putStatic
				}

				var fieldName = stringToUpperSnakeCase(processIdentifierPath(name));

				if (fieldName == null || fieldName.isEmpty()) {
					continue;
				}

				if (!duplicatedNames.contains(fieldName) && usedNames.add(fieldName)) {
					fieldNames.put(fieldEntry, fieldName);
				} else {
					duplicatedNames.add(fieldName);
					Logger.warn("Duplicate field name \"{}\" (\"{}\") for field {}", fieldName, name, fieldEntry);
				}
			}
		}
	}
}
