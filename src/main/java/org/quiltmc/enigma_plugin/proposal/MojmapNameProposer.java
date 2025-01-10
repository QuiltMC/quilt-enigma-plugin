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

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class MojmapNameProposer extends NameProposer {
	public static final String ID = "mojmap";

	private final Optional<String> mojmapPath;
	// must be static for now. nasty hack to make sure we don't read mojmaps twice
	// we can guarantee that this is nonnull for the other proposer because jar proposal blocks dynamic proposal
	public static EntryTree<EntryMapping> mojmaps;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public MojmapNameProposer(Optional<String> mojmapPath) {
		super(ID);
		this.mojmapPath = mojmapPath;
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		if (mojmapPath.isPresent()) {
			Path path = Path.of(mojmapPath.get());
			try {
				mojmaps = enigma.readMappings(path).orElse(null);
			} catch (Exception e) {
				Logger.error(e, "could not read mojmaps!");
			}
		} else {
			Logger.error("no mojmap path provided, disabling " + this.getSourcePluginId());
		}

		if (mojmaps != null) {
			mojmaps.getRootNodes().forEach((node) -> this.proposeNodeAndChildren(mappings, node));
		}
	}

	private void proposeNodeAndChildren(Map<Entry<?>, EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() != null && node.getValue().targetName() != null) {
			this.insertProposal(mappings, node.getEntry(), node.getValue().targetName());
		}

		node.getChildNodes().forEach((child) -> proposeNodeAndChildren(mappings, child));
	}
}
