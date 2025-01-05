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
