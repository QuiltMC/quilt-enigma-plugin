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

package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.index.JarIndexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class NameProposerService implements NameProposalService {
	private final List<NameProposer> nameProposers = new ArrayList<>();

	protected void addIfEnabled(EnigmaServiceContext<NameProposalService> context, String name, Supplier<NameProposer> factory) {
		this.addIfEnabled(context, null, name, indexer -> factory.get());
	}

	protected void addIfEnabled(EnigmaServiceContext<NameProposalService> context, JarIndexer indexer, String name, Function<JarIndexer, NameProposer> factory) {
		if (!Arguments.getBoolean(context, name)) {
			this.nameProposers.add(factory.apply(indexer));
		}
	}

	protected void addIfNotDisabled(EnigmaServiceContext<NameProposalService> context, String name, Supplier<NameProposer> factory) {
		this.addIfNotDisabled(context, null, name, indexer -> factory.get());
	}

	protected void addIfNotDisabled(EnigmaServiceContext<NameProposalService> context, JarIndexer indexer, String name, Function<JarIndexer, NameProposer> factory) {
		if (!Arguments.getBoolean(context, name, true)) {
			this.nameProposers.add(factory.apply(indexer));
		}
	}

	protected void add(JarIndexer indexer, Function<JarIndexer, NameProposer> factory) {
		this.nameProposers.add(factory.apply(indexer));
	}

	@Override
	public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
		HashMap<Entry<?>, EntryMapping> proposedNames = new HashMap<>();

		for (NameProposer proposer : this.nameProposers) {
			proposer.insertProposedNames(enigma, index, proposedNames);
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
