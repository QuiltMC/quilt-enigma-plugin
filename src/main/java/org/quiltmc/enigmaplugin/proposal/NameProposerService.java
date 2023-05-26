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
import org.quiltmc.enigmaplugin.Arguments;
import org.quiltmc.enigmaplugin.index.JarIndexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class NameProposerService implements NameProposalService {
	private final List<NameProposer<?>> nameProposers = new ArrayList<>();

	public NameProposerService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		this.addIfEnabled(context, indexer, Arguments.DISABLE_RECORDS, RecordComponentNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_ENUM_FIELDS, EnumFieldNameProposer::new);
		this.addIfEnabled(context, Arguments.DISABLE_EQUALS, EqualsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_LOGGER, LoggerNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CODECS, CodecNameProposer::new);

		if (indexer.getSimpleTypeSingleIndex().isEnabled()) {
			this.nameProposers.add(new SimpleTypeFieldNameProposer(indexer));
		}
	}

	private void addIfEnabled(EnigmaServiceContext<NameProposalService> context, String name, Supplier<NameProposer<?>> factory) {
		this.addIfEnabled(context, null, name, indexer -> factory.get());
	}

	private void addIfEnabled(EnigmaServiceContext<NameProposalService> context, JarIndexer indexer, String name, Function<JarIndexer, NameProposer<?>> factory) {
		if (!Arguments.isDisabled(context, name)) {
			this.nameProposers.add(factory.apply(indexer));
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
