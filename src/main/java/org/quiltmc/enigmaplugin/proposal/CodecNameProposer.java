/*
 * Copyright 2022 QuiltMC
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
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.quiltmc.enigmaplugin.index.CodecIndex;

import java.util.Optional;

public class CodecNameProposer implements NameProposer<Entry<?>> {
    private final CodecIndex index;

    public CodecNameProposer(CodecIndex index) {
        this.index = index;
    }

    @Override
    public Optional<String> doProposeName(Entry<?> entry, EntryRemapper remapper) {
        if (entry instanceof FieldEntry field && index.hasField(field)) {
            return Optional.ofNullable(index.getFieldName(field));
        } else if (entry instanceof MethodEntry method && index.hasMethod(method)) {
            return Optional.ofNullable(index.getMethodName(method));
        }
        return Optional.empty();
    }

    @Override
    public boolean canPropose(Entry<?> entry) {
        return entry instanceof FieldEntry || entry instanceof MethodEntry;
    }

    @Override
    public Entry<?> upcast(Entry<?> entry) {
        return entry;
    }
}
