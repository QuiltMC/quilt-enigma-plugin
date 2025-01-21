/*
 * Copyright 2025 QuiltMC
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

import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.Map;

/**
 * An extremely simple proposer that provides the names of all entries from the given mappings.
 * This proposer is meant to be run first so that the names are overridden by all other proposers.
 */
public class MappingMergeNameProposer extends NameProposer {
	public static final String ID = "mapping_merge";

	private final String mappingPath;
	// must be static for now. nasty hack to make sure we don't read mappings twice when also using the package proposer
	// we can guarantee that this is nonnull for the other proposer because jar proposal blocks dynamic proposal
	private static EntryTree<EntryMapping> mergedMappings;

	@Nullable
	public static EntryTree<EntryMapping> getMergedMappings() {
		return mergedMappings;
	}

	public MappingMergeNameProposer(@Nullable String mappingPath) {
		super(ID);
		this.mappingPath = mappingPath;
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		if (this.mappingPath != null) {
			Path path = Path.of(this.mappingPath);
			try {
				mergedMappings = enigma.readMappings(path).orElse(null);
			} catch (Exception e) {
				Logger.error(e, "could not read mappings to merge (path: " + path + ")!");
			}
		} else {
			Logger.error("no mapping path provided for merge, disabling " + this.getSourcePluginId());
		}

		if (mergedMappings != null) {
			mergedMappings.getRootNodes().forEach((node) -> this.proposeNodeAndChildren(mappings, node));
		}
	}

	private void proposeNodeAndChildren(Map<Entry<?>, EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() != null && node.getValue().targetName() != null) {
			this.insertProposal(mappings, node.getEntry(), node.getValue().targetName());
		}

		node.getChildNodes().forEach((child) -> this.proposeNodeAndChildren(mappings, child));
	}
}
