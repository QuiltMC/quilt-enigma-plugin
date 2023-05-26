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

import java.util.Optional;

public interface NameProposer<E extends Entry<?>> {
	Optional<String> doProposeName(E entry, EntryRemapper remapper);

	boolean canPropose(Entry<?> entry);

	E upcast(Entry<?> entry);

	default Optional<String> proposeName(Entry<?> entry, EntryRemapper remapper) {
		return doProposeName(upcast(entry), remapper);
	}
}
