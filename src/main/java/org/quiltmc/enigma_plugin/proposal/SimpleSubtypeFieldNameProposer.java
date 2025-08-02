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

import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.simple_subtype_single.SimpleSubtypeSingleIndex;

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
		this.index.getFields().forEach((field, fieldInfo) -> {
			if (!this.hasJarProposal(remapper, field)) {
				EntryMapping mapping = remapper.getMapping(new ClassEntry(fieldInfo.type()));
				String typeName = mapping.targetName();
				if (typeName != null) {
					String truncatedName = toLowerCaseTruncatedOrNull(typeName, fieldInfo.subtypeEntry().suffix());
					if (truncatedName != null) {
						this.insertDynamicProposal(
							mappings, field,
							fieldInfo.isConstant() ? toScreamingSnakeCase(truncatedName) : truncatedName
						);
					}
				}
			}
		});

		this.index.getParams().forEach((param, paramInfo) -> {
			if (!this.hasJarProposal(remapper, param)) {
				EntryMapping mapping = remapper.getMapping(new ClassEntry(paramInfo.type()));
				String typeName = mapping.targetName();
				if (typeName != null) {
					String truncatedName = toLowerCaseTruncatedOrNull(typeName, paramInfo.subtypeEntry().suffix());
					if (truncatedName != null) {
						this.insertDynamicProposal(mappings, param, truncatedName);
					}
				}
			}
		});
	}

	@Nullable
	private static String toLowerCaseTruncatedOrNull(String typeName, String suffix) {
		int truncatedLen = typeName.length() - suffix.length();
		if (truncatedLen > 0 && typeName.endsWith(suffix)) {
			StringBuilder nameBuilder = new StringBuilder();
			nameBuilder.append(Character.toLowerCase(typeName.charAt(0)));
			for (int i = 1; i < truncatedLen; i++) {
				nameBuilder.append(typeName.charAt(i));
			}

			return nameBuilder.toString();
		} else {
			return null;
		}
	}
}
