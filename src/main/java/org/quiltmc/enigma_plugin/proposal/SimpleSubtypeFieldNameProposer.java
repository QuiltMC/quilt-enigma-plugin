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
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleSubtypeSingleIndex;

import java.util.Map;

import static org.quiltmc.enigma_plugin.util.CasingUtil.toScreamingSnakeCase;

public class SimpleSubtypeFieldNameProposer extends NameProposer {
	public static final String ID = "simple_subtype_field_names";
	private final SimpleSubtypeSingleIndex index;

	public SimpleSubtypeFieldNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(SimpleSubtypeSingleIndex.class);
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) { }

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		this.index.getFields().forEach((field, info) -> {
			if (!this.hasJarProposal(remapper, field)) {
				EntryMapping mapping = remapper.getMapping(new ClassEntry(info.type()));
				String typeName = mapping.targetName();
				if (typeName != null) {
					info.entry().renamer().rename(typeName)
						.map(name -> info.isConstant() ? toScreamingSnakeCase(name) : unCapitalize(name))
						.ifPresent(name -> this.insertDynamicProposal(mappings, field, name));
				}
			}
		});

		this.index.getParams().forEach((param, info) -> {
			if (!this.hasJarProposal(remapper, param)) {
				EntryMapping mapping = remapper.getMapping(new ClassEntry(info.type()));
				String typeName = mapping.targetName();
				if (typeName != null) {
					info.entry().renamer().rename(typeName)
						.map(SimpleSubtypeFieldNameProposer::unCapitalize)
						.ifPresent(name -> this.insertDynamicProposal(mappings, param, name));
				}
			}
		});
	}

	private static String unCapitalize(String string) {
		var builder = new StringBuilder();
		builder.append(Character.toLowerCase(string.charAt(0)));
		for (int i = 1; i < string.length(); i++) {
			builder.append(string.charAt(i));
		}

		return builder.toString();
	}
}
