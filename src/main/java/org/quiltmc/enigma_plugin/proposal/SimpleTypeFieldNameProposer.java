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

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeSingleIndex;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleTypeFieldNameProposer extends NameProposer {
	public static final String ID = "simple_type_field_names";
	private final SimpleTypeSingleIndex index;

	public SimpleTypeFieldNameProposer(JarIndexer index) {
		super(ID, null);
		this.index = index.getIndex(SimpleTypeSingleIndex.class);
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		for (FieldEntry field : this.index.getFields()) {
			String name = this.index.getField(field);

			this.insertProposal(mappings, field, name);
		}

		for (LocalVariableEntry param : this.index.getParams()) {
			String name = this.index.getParam(param);

			this.insertProposal(mappings, param, name);
		}
	}

	// todo never called
	public void fixConflicts(Map<Entry<?>, EntryMapping> mappings, EntryRemapper remapper, LocalVariableEntry entry, String name) {
		Optional<LocalVariableEntry> conflict = getConflictingParam(remapper, entry, name);

		while (conflict.isPresent()) {
			LocalVariableEntry conflictEntry = conflict.get();
			var fallbacks = this.index.getParamFallbacks(conflictEntry);

			for (String fallbackName : fallbacks) {
				Optional<LocalVariableEntry> newConflict = getConflictingParam(remapper, conflictEntry, fallbackName);
				if (newConflict.isEmpty()) {
					this.insertDynamicProposal(mappings, remapper, conflictEntry, fallbackName);
					conflict = getConflictingParam(remapper, conflictEntry, name);
					break;
				}
			}

			this.insertDynamicProposal(mappings, remapper, conflictEntry, (String) null);
		}
	}

	private Optional<LocalVariableEntry> getConflictingParam(EntryRemapper remapper, LocalVariableEntry entry, String name) {
		MethodEntry method = entry.getParent();
		var args = method.getParameterIterator(remapper.getJarIndex().getIndex(EntryIndex.class), remapper.getDeobfuscator());

		while (args.hasNext()) {
			LocalVariableEntry arg = args.next();
			if (arg.getIndex() != entry.getIndex() && arg.getName().equals(name)) {
				return Optional.of(arg);
			}
		}

		return Optional.empty();
	}
}
