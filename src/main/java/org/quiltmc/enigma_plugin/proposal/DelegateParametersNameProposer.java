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
import org.tinylog.Logger;

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

	private void proposeNameUpwards(EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, LocalVariableEntry entry, String name) {
		this.proposeNameUpwards(remapper, mappings, entry, name, 0);
	}

	private void proposeNameUpwards(EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, LocalVariableEntry entry, String name, int depth) {
		if (depth >= 15) {
			Logger.warn("Tried to propose delegate parameters too deep!");
			return;
		}

		// Propose a name for all the parameters pointing to the given parameter
		var links = this.index.getLinks(entry);
		for (var link : links) {
			if (this.hasJarProposal(remapper, link)) {
				continue;
			}

			this.insertDynamicProposal(mappings, link, name);
			this.proposeNameUpwards(remapper, mappings, link, name, depth + 1);
		}
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		// Mappings loaded
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

			return;
		}

		if (obfEntry instanceof LocalVariableEntry paramEntry) {
			String name;
			if (newMapping.targetName() != null) {
				name = newMapping.targetName();
			} else {
				name = this.resolveName(remapper, mappings, paramEntry);
			}

			if (newMapping.targetName() == null) {
				this.insertDynamicProposal(mappings, paramEntry, name);
			}

			this.proposeNameUpwards(remapper, mappings, paramEntry, name);
		}
	}
}
