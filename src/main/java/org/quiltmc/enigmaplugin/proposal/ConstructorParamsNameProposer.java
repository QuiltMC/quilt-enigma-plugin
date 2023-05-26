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
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigmaplugin.index.ConstructorParametersIndex;
import org.quiltmc.enigmaplugin.index.JarIndexer;

import java.util.Optional;

public class ConstructorParamsNameProposer implements NameProposer<LocalVariableEntry> {
	private final ConstructorParametersIndex index;

	public ConstructorParamsNameProposer(JarIndexer index) {
		this.index = index.getConstructorParametersIndex();
	}

	@Override
	public Optional<String> doProposeName(LocalVariableEntry entry, NameProposerService service, EntryRemapper remapper) {
		FieldEntry linkedField = this.index.getLinkedField(entry);

		var newName = service.getMappedFieldName(remapper, linkedField);

		return Optional.ofNullable(newName);
	}

	@Override
	public boolean canPropose(Entry<?> entry) {
		return entry instanceof LocalVariableEntry paramEntry && this.index.getLinkedField(paramEntry) != null;
	}

	@Override
	public LocalVariableEntry upcast(Entry<?> entry) {
		return (LocalVariableEntry) entry;
	}
}
