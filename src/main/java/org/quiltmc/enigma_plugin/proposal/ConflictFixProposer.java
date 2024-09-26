package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeSingleIndex;

import java.util.Map;
import java.util.Optional;

public class ConflictFixProposer extends NameProposer {
	public static final String ID = "conflict_fix";
	private final SimpleTypeSingleIndex index;

	public ConflictFixProposer(JarIndexer jarIndex) {
		super(ID, null);
		this.index = jarIndex.getIndex(SimpleTypeSingleIndex.class);
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		for (Map.Entry<Entry<?>, EntryMapping> entry : mappings.entrySet()) {
			fixConflicts(mappings, remapper, entry.getKey(), entry.getValue());
		}
	}

	private void fixConflicts(Map<Entry<?>, EntryMapping> mappings, EntryRemapper remapper, Entry<?> entry, EntryMapping mapping) {
		if (entry instanceof LocalVariableEntry param && mapping != null) {
			this.fixParamConflicts(mappings, remapper, param, mapping);
		}
	}

	private void fixParamConflicts(Map<Entry<?>, EntryMapping> mappings, EntryRemapper remapper, LocalVariableEntry entry, EntryMapping mapping) {
		String name = mapping.targetName();
		Optional<LocalVariableEntry> conflict = getConflictingParam(remapper, entry, name);

		while (conflict.isPresent()) {
			LocalVariableEntry conflictEntry = conflict.get();
			var fallbacks = this.index.getParamFallbacks(conflictEntry);

			boolean fixed = false;

			if (fallbacks != null) {
				for (String fallbackName : fallbacks) {
					Optional<LocalVariableEntry> newConflict = getConflictingParam(remapper, conflictEntry, fallbackName);
					if (newConflict.isEmpty()) {
						this.insertDynamicProposal(mappings, remapper, conflictEntry, fallbackName);
						conflict = getConflictingParam(remapper, conflictEntry, name);
						fixed = true;
						break;
					}
				}
			}

			if (!fixed) {
				this.insertDynamicProposal(mappings, remapper, conflictEntry, (String) null);
			}
		}
	}

	private Optional<LocalVariableEntry> getConflictingParam(EntryRemapper remapper, LocalVariableEntry entry, String name) {
		MethodEntry method = entry.getParent();
		if (method != null) {
			var args = method.getParameterIterator(remapper.getJarIndex().getIndex(EntryIndex.class), remapper.getDeobfuscator());

			while (args.hasNext()) {
				LocalVariableEntry arg = args.next();
				if (arg.getIndex() != entry.getIndex() && arg.getName().equals(name)) {
					return Optional.of(arg);
				}
			}
		}

		return Optional.empty();
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
	}
}
