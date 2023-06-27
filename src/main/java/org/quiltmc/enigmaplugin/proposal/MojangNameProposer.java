package org.quiltmc.enigmaplugin.proposal;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.quiltmc.enigmaplugin.index.JarIndexer;

import java.util.Optional;

/**
 * If the entry is not deobfuscated, propose the name that's already provided.
 * This is useful to avoid situations where we simply click "mark as deobf" to name something.
 */
public class MojangNameProposer implements NameProposer<Entry<?>> {
    @SuppressWarnings("unused")
    public MojangNameProposer(JarIndexer indexer) {
    }

    @Override
    public Optional<String> doProposeName(Entry<?> entry, NameProposerService service, EntryRemapper remapper) {
        String name = entry.getName();
        if (remapper.getDeobfMapping(entry).targetName() == null
                && (entry instanceof FieldEntry && !name.startsWith("f_")
                || entry instanceof MethodEntry && !name.startsWith("m_")
                || entry instanceof ClassEntry && !name.startsWith("C_"))) {
            return Optional.of(name);
        }

        return Optional.empty();
    }

    @Override
    public boolean canPropose(Entry<?> entry) {
        return entry instanceof FieldEntry || entry instanceof MethodEntry || entry instanceof ClassEntry;
    }

    @Override
    public Entry<?> upcast(Entry<?> entry) {
        return entry;
    }
}
