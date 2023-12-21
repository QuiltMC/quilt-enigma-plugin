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

package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.index.GetterSetterIndex;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.util.Descriptors;

import java.util.Map;

public class GetterSetterNameProposer extends NameProposer {
	public static final String ID = "getter_setter";
	private final GetterSetterIndex index;

	public GetterSetterNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getGetterSetterIndex();
	}

	private static String getMethodName(String newName, MethodEntry method) {
		if (newName == null || newName.isEmpty()) {
			return null;
		}

		newName = newName.substring(0, 1).toUpperCase() + newName.substring(1);

		if (method.getDesc().getReturnDesc().equals(Descriptors.VOID_TYPE)) {
			return "set" + newName;
		} else {
			return "get" + newName;
		}
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (obfEntry == null) {
			// Mappings were just loaded
			for (MethodEntry method : this.index.getLinkedMethods()) {
				if (this.hasJarProposal(remapper, method)) {
					continue;
				}

				FieldEntry linkedField = this.index.getLinkedField(method);
				EntryMapping mapping = remapper.getMapping(linkedField);
				var newName = getMethodName(mapping.targetName(), method);

				if (newName == null) {
					continue;
				}

				this.insertDynamicProposal(mappings, method, newName);
			}

			for (LocalVariableEntry parameter : this.index.getLinkedParameters()) {
				if (this.hasJarProposal(remapper, parameter)) {
					continue;
				}

				FieldEntry linkedField = this.index.getLinkedField(parameter);
				EntryMapping mapping = remapper.getMapping(linkedField);
				var newName = mapping.targetName();

				if (newName == null || newName.isEmpty()) {
					continue;
				}

				this.insertDynamicProposal(mappings, parameter, newName);
			}

			return;
		}

		if (obfEntry instanceof FieldEntry field && this.index.fieldHasLinks(field)) {
			var name = newMapping.targetName();

			for (Entry<?> link : this.index.getFieldLinks(field)) {
				if (this.hasJarProposal(remapper, link)) {
					continue;
				}

				if (link instanceof MethodEntry method) {
					var newName = getMethodName(name, method);
					this.insertDynamicProposal(mappings, method, newName);
				} else if (link instanceof LocalVariableEntry parameter) {
					this.insertDynamicProposal(mappings, parameter, name);
				}
			}
		}
	}
}
