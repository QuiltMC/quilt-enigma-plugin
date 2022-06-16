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
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.util.Optional;

public class EqualsNameProposer implements NameProposer<LocalVariableEntry> {
    private static final MethodDescriptor EQUALS_DESCRIPTOR = new MethodDescriptor("(Ljava/lang/Object;)Z");

    @Override
    public Optional<String> doProposeName(LocalVariableEntry entry, EntryRemapper remapper) {
        return Optional.of("o");
    }

    @Override
    public boolean canPropose(Entry<?> entry) {
        if (entry instanceof LocalVariableEntry localVar) {
            MethodEntry parent = localVar.getParent();
            if (parent == null) {
                return false;
            }

            String methodName = parent.getName();
            return methodName.equals("equals") && parent.getDesc().equals(EQUALS_DESCRIPTOR);
        }

        return false;
    }

    @Override
    public LocalVariableEntry upcast(Entry<?> entry) {
        return (LocalVariableEntry) entry;
    }
}
