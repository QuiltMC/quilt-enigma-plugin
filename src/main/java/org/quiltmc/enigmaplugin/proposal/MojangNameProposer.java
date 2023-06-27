/*
 * Copyright 2023 QuiltMC
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

package org.quiltmc.enigmaplugin.proposal;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Optional;

/**
 * If the entry is not deobfuscated, propose the name that's already provided.
 * This is useful to avoid situations where we simply click "mark as deobf" to name something.
 */
public class MojangNameProposer implements NameProposer<Entry<?>> {
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
