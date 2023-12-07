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

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigmaplugin.Arguments;
import org.quiltmc.enigmaplugin.index.JarIndexer;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class NameProposerService implements NameProposalService {
	/**
	 * Represents a cache of the successful proposed field names.
	 */
	private final Map<FieldEntry, String> namedFields = new WeakHashMap<>();
	private final List<NameProposer> nameProposers = new ArrayList<>();

	public NameProposerService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		this.addIfEnabled(context, indexer, Arguments.DISABLE_RECORDS, RecordComponentNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_ENUM_FIELDS, EnumFieldNameProposer::new);
		this.addIfEnabled(context, Arguments.DISABLE_EQUALS, EqualsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_LOGGER, LoggerNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CODECS, CodecNameProposer::new);
		this.addIfNotDisabled(context, Arguments.DISABLE_MAP_NON_HASHED, MojangNameProposer::new);

		if (indexer.getSimpleTypeSingleIndex().isEnabled()) {
			this.nameProposers.add(new SimpleTypeFieldNameProposer(indexer));
		}

		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONSTRUCTOR_PARAMS, ConstructorParamsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_GETTER_SETTER, GetterSetterNameProposer::new);
	}

	private void addIfEnabled(EnigmaServiceContext<NameProposalService> context, String name, Supplier<NameProposer> factory) {
		this.addIfEnabled(context, null, name, indexer -> factory.get());
	}

	private void addIfEnabled(EnigmaServiceContext<NameProposalService> context, JarIndexer indexer, String name, Function<JarIndexer, NameProposer> factory) {
		if (!Arguments.isDisabled(context, name)) {
			this.nameProposers.add(factory.apply(indexer));
		}
	}

	private void addIfNotDisabled(EnigmaServiceContext<NameProposalService> context, String name, Supplier<NameProposer> factory) {
		if (!Arguments.isDisabled(context, name, true)) {
			this.nameProposers.add(factory.get());
		}
	}

	public String getMappedFieldName(EntryRemapper remapper, FieldEntry field) {
		var deobfedField = remapper.extendedDeobfuscate(field);

		if (deobfedField != null && deobfedField.isDeobfuscated()) {
			return deobfedField.getValue().getName();
		} else {
			return this.namedFields.get(field);
		}
	}

	@Override
	public Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index) {
		HashMap<Entry<?>, EntryMapping> proposedNames = new HashMap<>();

		for (NameProposer proposer : this.nameProposers) {
			proposer.insertProposedNames(index, proposedNames);
		}

		return proposedNames;
	}

	@Override
	public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping) {
		HashMap<Entry<?>, EntryMapping> proposedNames = new HashMap<>();

		for (NameProposer proposer : this.nameProposers) {
			proposer.proposeDynamicNames(remapper, obfEntry, oldMapping, newMapping, proposedNames);
		}

		return proposedNames;
	}
}
