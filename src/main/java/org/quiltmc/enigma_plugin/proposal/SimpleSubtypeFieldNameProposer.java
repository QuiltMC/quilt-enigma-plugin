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
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleSubtypeSingleIndex;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleSubtypeSingleIndex.FieldInfo;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleSubtypeSingleIndex.SubtypeEntry;

import java.util.Map;

import static org.quiltmc.enigma_plugin.util.CasingUtil.toScreamingSnakeCase;

public class SimpleSubtypeFieldNameProposer extends NameProposer {
	public static final String ID = "simple_subtype_field_names";
	private final SimpleSubtypeSingleIndex index;

	private static String unCapitalize(String string) {
		var builder = new StringBuilder();
		builder.append(Character.toLowerCase(string.charAt(0)));
		for (int i = 1; i < string.length(); i++) {
			builder.append(string.charAt(i));
		}

		return builder.toString();
	}

	public SimpleSubtypeFieldNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(SimpleSubtypeSingleIndex.class);
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) { }

	@Override
	public void proposeDynamicNames(
			EntryRemapper remapper, Entry<?> obfEntry,
			EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings
	) {
		if (obfEntry == null) {
			this.index.forEachField((type, field, info) -> this.proposeField(remapper, mappings, type, field, info));
			this.index.forEachParam((type, param, entry) -> this.proposeParam(remapper, mappings, type, param, entry));
		} else if (obfEntry instanceof ClassEntry type) {
			this.index.forEachFieldOfType(type, (field, info) -> this.proposeField(remapper, mappings, type, field, info));
			this.index.forEachParamOfType(type, (param, entry) -> this.proposeParam(remapper, mappings, type, param, entry));
		}
	}

	private void proposeField(
			EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings,
			ClassEntry type, FieldEntry field, FieldInfo info
	) {
		this.proposeField(remapper, mappings, remapper.getMapping(type).targetName(), field, info);
	}

	private void proposeField(
			EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings,
			String typeName, FieldEntry field, FieldInfo info
	) {
		if (!this.hasJarProposal(remapper, field)) {
			if (typeName != null) {
				info.entry().renamer().rename(typeName)
					.map(name -> info.isConstant() ? toScreamingSnakeCase(name) : unCapitalize(name))
					.ifPresent(name -> this.insertDynamicProposal(mappings, field, name));
			}
		}
	}

	private void proposeParam(
			EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings,
			ClassEntry type, LocalVariableEntry param, SubtypeEntry entry
	) {
		this.proposeParam(remapper, mappings, remapper.getMapping(type).targetName(), param, entry);
	}

	private void proposeParam(
			EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings,
			String typeName, LocalVariableEntry param, SubtypeEntry entry
	) {
		if (!this.hasJarProposal(remapper, param)) {
			if (typeName != null) {
				entry.renamer().rename(typeName)
					.map(SimpleSubtypeFieldNameProposer::unCapitalize)
					.ifPresent(name -> this.insertDynamicProposal(mappings, param, name));
			}
		}
	}
}
