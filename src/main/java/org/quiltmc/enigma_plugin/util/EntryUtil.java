/*
 * Copyright 2025 QuiltMC
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

package org.quiltmc.enigma_plugin.util;

import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.stream.Stream;

public final class EntryUtil {
	private EntryUtil() {
		throw new UnsupportedOperationException();
	}

	public static EntryMapping getMappingOrNonHashed(Entry<?> entry, EntryRemapper remapper, TokenType type, String sourcePluginId) {
		final EntryMapping mapping = remapper.getMapping(entry);
		return mappingOrNonHashed(entry, mapping, type, sourcePluginId);
	}

	public static EntryMapping mappingOrNonHashed(Entry<?> entry, @Nullable EntryMapping mapping, TokenType type, String sourcePluginId) {
		if (mapping == null) {
			mapping = EntryMapping.OBFUSCATED;
		}

		if (mapping.targetName() == null) {
			String name = getNonHashedNameOrNull(entry);
			return name == null ? mapping : new EntryMapping(name, null, type, sourcePluginId);
		} else {
			return mapping;
		}
	}

	@Nullable
	public static String getNonHashedNameOrNull(Entry<?> entry) {
		String name = entry.getName();
		if (entry instanceof FieldEntry) {
			return name.startsWith("f_") ? null : name;
		} else if (entry instanceof MethodEntry method) {
			return name.startsWith("m_") || method.isConstructor() ? null : name;
		} else if (entry instanceof ClassEntry clazz) {
			return clazz.getSimpleName().startsWith("C_") ? null : clazz.getName();
		} else {
			return null;
		}
	}

	// InheritanceIndex::getAncestors returns an unordered set
	/**
	 * @return a breadth-first stream of the passed {@code classEntry}'s ancestors;
	 * order within one level of depth is not guaranteed
	 */
	public static Stream<ClassEntry> streamAncestors(ClassEntry classEntry, InheritanceIndex inheritance) {
		final Collection<ClassEntry> parents = inheritance.getParents(classEntry);
		return Stream.concat(
			parents.stream(),
			parents.stream().flatMap(parent -> streamAncestors(parent, inheritance))
		);
	}
}
