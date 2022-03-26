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

import cuchaz.enigma.api.service.EnigmaServiceContext;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;
import org.quiltmc.enigmaplugin.index.JarIndexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NameProposerService implements NameProposalService {
    public static final String DISABLE_RECORDS_ARG = "disable_records";
    public static final String DISABLE_ENUM_FIELDS_ARG = "disable_enum_fields";
    private final List<NameProposer<?>> nameProposers;

    public NameProposerService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
        nameProposers = new ArrayList<>();
        boolean disableRecords = context.getArgument(DISABLE_RECORDS_ARG).map(Boolean::parseBoolean).orElse(false);
        boolean disableEnumFields = context.getArgument(DISABLE_ENUM_FIELDS_ARG).map(Boolean::parseBoolean).orElse(false);

        if (!disableRecords) {
            nameProposers.add(new RecordComponentNameProposer(indexer.getRecordIndex()));
        }
        if (!disableEnumFields) {
            nameProposers.add(new EnumFieldNameProposer(indexer.getEnumFieldsIndex()));
        }
    }

    @Override
    public Optional<String> proposeName(Entry<?> obfEntry, EntryRemapper remapper) {
        Optional<String> name;
        for (NameProposer<?> proposer : nameProposers) {
            if (proposer.canPropose(obfEntry)) {
                name = proposer.proposeName(obfEntry, remapper);
                if (name.isPresent()) {
                    return name;
                }
            }
        }

        return Optional.empty();
    }
}
