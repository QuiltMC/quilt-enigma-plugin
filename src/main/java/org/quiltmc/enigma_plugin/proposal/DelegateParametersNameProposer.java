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

package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma_plugin.index.DelegateParametersIndex;
import org.quiltmc.enigma_plugin.index.JarIndexer;

import java.util.Map;

public class DelegateParametersNameProposer extends NameProposer {
	public static final String ID = "delegate_params";
	private final DelegateParametersIndex index;

	public DelegateParametersNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(DelegateParametersIndex.class);
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		// TODO: Filter out non-renamable entries
	}

	private String resolveName(EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, LocalVariableEntry entry) {
		if (entry == null) {
			return null;
		}

		var name = this.index.getName(entry);
		if (name != null) {
			return name;
		}

		var mapping = remapper.getMapping(entry);
		if (mapping.targetName() != null) {
			return mapping.targetName();
		} else {
			mapping = mappings.get(entry);
			if (mapping != null && mapping.targetName() != null) {
				return mapping.targetName();
			} else {
				return this.resolveName(remapper, mappings, this.index.get(entry));
			}
		}
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (obfEntry == null) {
			for (var entry : this.index.getKeys()) {
				if (this.hasJarProposal(remapper, entry)) {
					continue;
				}

				var name = this.resolveName(remapper, mappings, entry);

				if (name != null) {
					this.insertDynamicProposal(mappings, entry, name);
				}
			}
		}
		// TODO: Changes
	}
}
