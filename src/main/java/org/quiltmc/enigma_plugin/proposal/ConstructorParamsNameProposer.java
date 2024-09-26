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
import org.quiltmc.enigma_plugin.index.ConstructorParametersIndex;
import org.quiltmc.enigma_plugin.index.JarIndexer;

import java.util.Map;

public class ConstructorParamsNameProposer extends NameProposer {
	public static final String ID = "constructor_params";
	private final ConstructorParametersIndex index;

	public ConstructorParamsNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(ConstructorParametersIndex.class);
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (obfEntry instanceof FieldEntry field && this.index.isFieldLinked(field)) {
			for (LocalVariableEntry parameter : this.index.getParametersForField(field)) {
				if (this.hasJarProposal(remapper, parameter)) {
					continue;
				}

				this.insertDynamicProposal(mappings, parameter, newMapping);
			}
		} else if (obfEntry instanceof LocalVariableEntry parameter && this.index.isParameterLinked(parameter)) {
			FieldEntry linkedField = this.index.getLinkedField(parameter);

			if (!this.hasJarProposal(remapper, linkedField)) {
				this.insertDynamicProposal(mappings, linkedField, newMapping);
			}

			for (LocalVariableEntry param : this.index.getParametersForField(linkedField)) {
				if (this.hasJarProposal(remapper, param)) {
					continue;
				}

				if (param != parameter) {
					this.insertDynamicProposal(mappings, param, newMapping);
				}
			}
		} else if (obfEntry == null) {
			// Mappings were just loaded
			for (LocalVariableEntry parameter : this.index.getParameters()) {
				if (this.hasJarProposal(remapper, parameter)) {
					continue;
				}

				FieldEntry linkedField = this.index.getLinkedField(parameter);
				EntryMapping mapping = remapper.getMapping(linkedField);

				this.insertDynamicProposal(mappings, parameter, mapping);
			}
		}
	}
}
