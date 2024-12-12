package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.tinylog.Logger;

import java.util.Map;

public class MojmapPackageProposer extends NameProposer {
	public static final String ID = "mojmap_packages";

	public MojmapPackageProposer() {
		super(ID);
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		// no-op
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		// todo definitions file

		if (obfEntry == null) {
			// initial proposal for all classes
			for (ClassEntry classEntry : remapper.getJarIndex().getIndex(EntryIndex.class).getClasses()) {
				this.tryProposeForEntry(remapper, mappings, classEntry);
			}
		} else {
			this.tryProposeForEntry(remapper, mappings, obfEntry);
		}
	}

	private void tryProposeForEntry(EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, Entry<?> entry) {
		var mojmaps = MojmapNameProposer.mojmaps;

		if (entry instanceof ClassEntry classEntry && !classEntry.isInnerClass() && mojmaps != null) {
			String newName = getMojmappedName(remapper, classEntry);
			mappings.put(classEntry, new EntryMapping(newName));
		}
	}

	private String getMojmappedName(EntryRemapper remapper, ClassEntry classEntry) {
		var mojmaps = MojmapNameProposer.mojmaps;

		var mapping = mojmaps.get(classEntry);
		if (mapping != null && mapping.targetName() != null) {
			String mojmapName = mapping.targetName();
			String oldPackage = mojmapName.substring(0, mojmapName.lastIndexOf('/'));
			String className = remapper.deobfuscate(classEntry).getSimpleName();

			return oldPackage + "/" + className;
		} else {
			Logger.error("failed to propose name: could not find mojmap for " + classEntry.getFullName());
			return null;
		}
	}
}
