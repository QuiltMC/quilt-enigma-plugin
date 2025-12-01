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

package org.quiltmc.enigma_plugin.proposal;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;
import org.quiltmc.enigma_plugin.util.EntryUtil;

import java.util.Map;

public abstract class NameProposer {
	private final String id;

	public NameProposer(String id) {
		this.id = id;
	}

	public String getSourcePluginId() {
		return getSourcePluginId(this.id);
	}

	public static String getSourcePluginId(String id) {
		return QuiltEnigmaPlugin.NAME_PROPOSAL_SERVICE_ID + "/" + id;
	}

	public void insertProposal(Map<Entry<?>, EntryMapping> mappings, Entry<?> entry, EntryMapping mapping) {
		if (mapping != null) {
			if (mapping.targetName() != null && !mapping.targetName().isEmpty()) {
				this.insertProposal(mappings, entry, mapping.targetName());
			} else {
				this.insertProposal(mappings, entry, (String) null);
			}
		}
	}

	public void insertProposal(Map<Entry<?>, EntryMapping> mappings, Entry<?> entry, String name) {
		this.insertProposal(mappings, entry, name, TokenType.JAR_PROPOSED);
	}

	public void insertDynamicProposal(Map<Entry<?>, EntryMapping> mappings, Entry<?> entry, EntryMapping mapping) {
		if (mapping != null) {
			if (mapping.targetName() != null && !mapping.targetName().isEmpty()) {
				this.insertDynamicProposal(mappings, entry, mapping.targetName());
			} else {
				this.insertDynamicProposal(mappings, entry, (String) null);
			}
		}
	}

	public void insertDynamicProposal(Map<Entry<?>, EntryMapping> mappings, Entry<?> entry, String name) {
		this.insertProposal(mappings, entry, name, TokenType.DYNAMIC_PROPOSED);
	}

	private void insertProposal(Map<Entry<?>, EntryMapping> mappings, Entry<?> entry, String name, TokenType tokenType) {
		if (!mappings.containsKey(entry)) {
			if (name != null) {
				EntryMapping mapping = new EntryMapping(name, null, tokenType, this.getSourcePluginId());
				mappings.put(entry, mapping);
			} else {
				mappings.put(entry, null);
			}
		}
	}

	public boolean hasJarProposal(EntryRemapper remapper, Entry<?> entry) {
		return remapper.getJarProposedMappings().contains(entry);
	}

	public abstract void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings);

	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
	}

	public EntryMapping getMappingOrNonHashed(Entry<?> entry, EntryRemapper remapper, TokenType type) {
		return EntryUtil.getMappingOrNonHashed(entry, remapper, type, this.getSourcePluginId());
	}

	public EntryMapping mappingOrNonHashed(Entry<?> entry, @Nullable EntryMapping mapping, TokenType type) {
		return EntryUtil.mappingOrNonHashed(entry, mapping, type, this.getSourcePluginId());
	}
}
