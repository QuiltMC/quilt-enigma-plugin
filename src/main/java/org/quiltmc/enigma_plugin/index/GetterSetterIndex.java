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

import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.quiltmc.enigma_plugin.util.Descriptors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GetterSetterIndex extends Index {
	private final Map<MethodEntry, FieldEntry> linked = new HashMap<>();
	private final Map<LocalVariableEntry, FieldEntry> linkedSetterParams = new HashMap<>();
	private final Map<FieldEntry, Set<Entry<?>>> links = new HashMap<>();

	public GetterSetterIndex() {
		super(Arguments.DISABLE_GETTER_SETTER);
	}

	@Override
	public void visitClassNode(ClassNode node) {
		for (var method : node.methods) {
			if (!AsmUtil.matchAccess(method, ACC_STATIC) && !AsmUtil.matchAccess(method, ACC_NATIVE)) {
				var descriptor = new MethodDescriptor(method.desc);

				if (descriptor.getReturnDesc().equals(Descriptors.VOID_TYPE)
						&& descriptor.getArgumentDescs().size() == 1) { // Potential setter.
					if (descriptor.getArgumentDescs().get(0).equals(Descriptors.BOOLEAN_TYPE)) {
						continue; // Ignore booleans for now.
					}

					AsmUtil.getFieldFromSetter(node, method)
							.ifPresent(field -> {
								this.linkField(node, method, descriptor, field);
							});
				} else { // Potential getter.
					if (descriptor.getReturnDesc().equals(Descriptors.BOOLEAN_TYPE)) {
						continue; // Ignore booleans for now.
					}

					AsmUtil.getFieldFromGetter(node, method)
							.ifPresent(field -> {
								this.linkField(node, method, descriptor, field);
							});
				}
			}
		}
	}

	private void linkField(ClassNode classNode, MethodNode methodNode, MethodDescriptor descriptor, FieldNode fieldNode) {
		var classEntry = new ClassEntry(classNode.name);
		var methodEntry = new MethodEntry(classEntry, methodNode.name, descriptor);
		var fieldEntry = new FieldEntry(classEntry, fieldNode.name, new TypeDescriptor(fieldNode.desc));

		this.linked.put(methodEntry, fieldEntry);
		this.links.computeIfAbsent(fieldEntry, f -> new HashSet<>()).add(methodEntry);

		if (descriptor.getArgumentDescs().size() == 1) {
			var paramEntry = new LocalVariableEntry(methodEntry, 1, "", true, null);
			this.linkedSetterParams.put(paramEntry, fieldEntry);
			this.links.get(fieldEntry).add(paramEntry);
		}
	}

	public FieldEntry getLinkedField(MethodEntry method) {
		return this.linked.get(method);
	}

	public FieldEntry getLinkedField(LocalVariableEntry param) {
		return this.linkedSetterParams.get(param);
	}

	public Set<Entry<?>> getFieldLinks(FieldEntry field) {
		return this.links.get(field);
	}

	public boolean fieldHasLinks(FieldEntry field) {
		return this.links.containsKey(field);
	}

	public Set<MethodEntry> getLinkedMethods() {
		return this.linked.keySet();
	}

	public Set<LocalVariableEntry> getLinkedParameters() {
		return this.linkedSetterParams.keySet();
	}
}
