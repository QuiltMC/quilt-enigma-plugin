/*
 * Copyright 2023 QuiltMC
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

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.objectweb.asm.Opcodes;
import org.quiltmc.enigma_plugin.util.Descriptors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConstructorParametersIndex implements Index {
	private final Map<LocalVariableEntry, FieldEntry> entries = new HashMap<>();
	private final Map<FieldEntry, Set<LocalVariableEntry>> entriesByField = new HashMap<>();

	@Override
	public void visitClassNode(ClassNode classNode) {
		for (var method : classNode.methods) {
			if (method.name.equals("<init>")) {
				this.visitConstructor(classNode, method);
			}
		}
	}

	private void visitConstructor(ClassNode classNode, MethodNode constructorNode) {
		var classEntry = new ClassEntry(classNode.name);
		var methodEntry = new MethodEntry(classEntry, constructorNode.name, new MethodDescriptor(constructorNode.desc));

		var parameters = Descriptors.getParameters(constructorNode);
		if (parameters.isEmpty()) return;

		/*if (this.callToCanonical(classNode, constructorNode)) {
			// @TODO Handle non-canonical constructors one day, as not every field will be present.
		}*/

		for (var inst : constructorNode.instructions) {
			// Search for every field assignation.
			if (inst.getOpcode() == Opcodes.PUTFIELD) {
				var fieldInst = (FieldInsnNode) inst;

				if (!fieldInst.owner.equals(classNode.name)) continue; // The owner isn't this class.

				var previousInst = fieldInst.getPrevious();

				if (previousInst.getOpcode() >= Opcodes.ILOAD && previousInst.getOpcode() <= Opcodes.ALOAD) {
					var loadInst = (VarInsnNode) previousInst;

					if (parameters.get(parameters.size() - 1).lvtIndex() < loadInst.var) {
						continue; // This load opcode does not correspond to a parameter.
					}

					var param = new LocalVariableEntry(methodEntry, loadInst.var, "", true, null);
					var field = new FieldEntry(classEntry, fieldInst.name, new TypeDescriptor(fieldInst.desc));
					this.entries.put(param, field);
					this.entriesByField.computeIfAbsent(field, f -> new HashSet<>()).add(param);
				}
			}
		}
	}

	/**
	 * Gets the linked field of the given parameter.
	 *
	 * @param entry the parameter
	 * @return the field
	 */
	public FieldEntry getLinkedField(LocalVariableEntry entry) {
		return this.entries.get(entry);
	}

	public Set<LocalVariableEntry> getParameters() {
		return this.entries.keySet();
	}

	public boolean isParameterLinked(LocalVariableEntry parameter) {
		return this.entries.containsKey(parameter);
	}

	public boolean isFieldLinked(FieldEntry field) {
		return this.entriesByField.containsKey(field);
	}

	public Set<LocalVariableEntry> getParametersForField(FieldEntry field) {
		return this.entriesByField.get(field);
	}

	private boolean callToCanonical(ClassNode classNode, MethodNode constructorNode) {
		for (var inst : constructorNode.instructions) {
			if (inst.getOpcode() == Opcodes.INVOKESPECIAL && inst instanceof MethodInsnNode instNode) {
				if (instNode.owner.equals(classNode.name) && instNode.name.equals("<init>")) {
					return true;
				}
			}
		}

		return false;
	}
}
