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
import org.quiltmc.enigmaplugin.index.JarIndexer;
import org.quiltmc.enigmaplugin.index.LoggerIndex;

import java.util.Optional;

/**
 * Proposes names for the {@code org/slf4j/Logger} class. Will always propose the same name, {@code LOGGER}.
 */
public class LoggerNameProposer implements NameProposer<FieldEntry> {
	private final LoggerIndex index;

	public LoggerNameProposer(JarIndexer index) {
		this.index = index.getLoggerIndex();
	}

	@Override
	public Optional<String> doProposeName(FieldEntry entry, NameProposerService service, EntryRemapper remapper) {
		return Optional.of("LOGGER");
	}

	@Override
	public boolean canPropose(Entry<?> entry) {
		return entry instanceof FieldEntry fieldEntry
				&& this.index.hasField(fieldEntry);
	}

	@Override
	public FieldEntry upcast(Entry<?> entry) {
		return (FieldEntry) entry;
	}
}
