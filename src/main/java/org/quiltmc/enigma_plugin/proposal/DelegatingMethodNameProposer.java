package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.index.delegating_method.DelegatingMethodIndex;
import org.quiltmc.enigma_plugin.index.JarIndexer;

import java.util.Map;

public class DelegatingMethodNameProposer extends NameProposer {
	public static final String ID = "delegating_method";

	private final DelegatingMethodIndex index;

	public DelegatingMethodNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(DelegatingMethodIndex.class);
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) { }

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (obfEntry == null) {
			this.index.forEachDelegation((delegate, delegaters) -> {
				final EntryMapping mapping = remapper.getMapping(delegate);
				if (mapping.targetName() != null) {
					delegaters.forEach(delegater ->
						this.insertDelegaterName(delegater, mappings, new EntryMapping(mapping.targetName()))
					);
				}
			});
		} else if (obfEntry instanceof MethodEntry method) {
			if (newMapping.targetName() != null) {
				this.index.streamDelegaters(method).forEach(delegater ->
					this.insertDelegaterName(delegater, mappings, new EntryMapping(newMapping.targetName()))
				);
			}
		}
	}

	private void insertDelegaterName(MethodEntry delegater, Map<Entry<?>, EntryMapping> mappings, EntryMapping mapping) {
		this.insertDynamicProposal(mappings, delegater, mapping);
		this.index.streamDelegaters(delegater).forEach(outerDelegater -> this.insertDelegaterName(outerDelegater, mappings, mapping));
	}
}
