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
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.quiltmc.enigmaplugin.index.GetterSetterIndex;
import org.quiltmc.enigmaplugin.index.JarIndexer;
import org.quiltmc.enigmaplugin.util.Descriptors;

import java.util.Optional;

public class GetterSetterNameProposer implements NameProposer<Entry<?>> {
	private final GetterSetterIndex index;

	public GetterSetterNameProposer(JarIndexer index) {
		this.index = index.getGetterSetterIndex();
	}

	@Override
	public Optional<String> doProposeName(Entry<?> entry, NameProposerService service, EntryRemapper remapper) {
		if (entry instanceof MethodEntry methodEntry) {
			FieldEntry linkedField = this.index.getLinkedField(methodEntry);

			var newName = service.getMappedFieldName(remapper, linkedField);

			if (newName == null) {
				return Optional.empty();
			}

			if (methodEntry.getDesc().getReturnDesc().equals(Descriptors.VOID_TYPE)) {
				// Setter
				newName = "set" + newName.substring(0, 1).toUpperCase() + newName.substring(1);
			} else {
				// Getter
				newName = "get" + newName.substring(0, 1).toUpperCase() + newName.substring(1);
			}

			return Optional.of(newName);
		} else if (entry instanceof LocalVariableEntry paramEntry) {
			FieldEntry linkedField = this.index.getLinkedField(paramEntry);

			var newName = service.getMappedFieldName(remapper, linkedField);

			return Optional.ofNullable(newName);
		}

		return Optional.empty();
	}

	@Override
	public boolean canPropose(Entry<?> entry) {
		return entry instanceof MethodEntry methodEntry && this.index.getLinkedField(methodEntry) != null
				|| entry instanceof LocalVariableEntry paramEntry && this.index.getLinkedField(paramEntry) != null;
	}

	@Override
	public Entry<?> upcast(Entry<?> entry) {
		return entry;
	}
}
