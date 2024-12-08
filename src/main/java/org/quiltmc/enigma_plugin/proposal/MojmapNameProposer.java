package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.tinylog.Logger;

import java.util.Map;
import java.util.Optional;

public class MojmapNameProposer extends NameProposer {
	public static final String ID = "mojmap";

	private final String mojmapPath;
	private EntryTree<EntryMapping> mojmaps;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public MojmapNameProposer(Optional<String> path) {
		super(ID);

		if (path.isPresent()) {
			this.mojmapPath = path.get();
		} else {
			Logger.error("No mojmap path provided, disabling " + this.getSourcePluginId());
			this.mojmapPath = null;
		}
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		// todo read mojmaps
		// needs more context (enigma change)
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		// todo dynamically fix class names to match their moj package
		// todo definitions file
	}

	@Override
	public boolean fallback() {
		return true;
	}
}
