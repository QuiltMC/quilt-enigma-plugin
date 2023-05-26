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
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigmaplugin.index.JarIndexer;
import org.quiltmc.enigmaplugin.index.simple_type_single.SimpleTypeSingleIndex;
import org.quiltmc.enigmaplugin.index.simple_type_single.SimpleTypeSingleIndex.ParameterEntry;

import java.util.Optional;

public class SimpleTypeFieldNameProposer implements NameProposer<Entry<?>> {
	private final SimpleTypeSingleIndex index;

	public SimpleTypeFieldNameProposer(JarIndexer index) {
		this.index = index.getSimpleTypeSingleIndex();
	}

	@Override
	public Optional<String> doProposeName(Entry<?> entry,NameProposerService service, EntryRemapper remapper) {
		if (entry instanceof FieldEntry fieldEntry) {
			return Optional.ofNullable(this.index.getField(fieldEntry));
		} else if (entry instanceof LocalVariableEntry localVariableEntry) {
			var paramEntry = ParameterEntry.fromLocalVariableEntry(localVariableEntry);
			return Optional.ofNullable(this.index.getParam(paramEntry));
		}

		return Optional.empty();
	}

	@Override
	public boolean canPropose(Entry<?> entry) {
		return entry instanceof FieldEntry || entry instanceof LocalVariableEntry;
	}

	@Override
	public Entry<?> upcast(Entry<?> entry) {
		return entry;
	}
}
