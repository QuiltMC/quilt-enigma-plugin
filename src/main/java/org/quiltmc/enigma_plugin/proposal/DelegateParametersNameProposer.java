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

	private EntryMapping resolveMapping(EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, LocalVariableEntry entry) {
		if (entry == null) {
			return EntryMapping.DEFAULT;
		}

		var mapping = remapper.getMapping(entry);
		if (mapping.targetName() != null) {
			return mapping;
		} else {
			mapping = mappings.get(entry);
			if (mapping != null && mapping.targetName() != null) {
				return mapping;
			} else {
				return this.resolveMapping(remapper, mappings, this.index.get(entry));
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

				var mapping = this.resolveMapping(remapper, mappings, entry);

				if (mapping.targetName() != null) {
					this.insertDynamicProposal(mappings, entry, mapping);
				}
			}
		}
	}
}
