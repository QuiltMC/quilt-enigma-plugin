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

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.enum_fields.EnumFieldsIndex;

import java.util.Map;

public class EnumFieldNameProposer extends NameProposer {
	public static final String ID = "enum_fields";
	private final EnumFieldsIndex enumIndex;

	public EnumFieldNameProposer(JarIndexer index) {
		super(ID);
		this.enumIndex = index.getEnumFieldsIndex();
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		for (FieldEntry field : this.enumIndex.getFields()) {
			String name = this.enumIndex.getName(field);
			this.insertProposal(mappings, field, name);
		}
	}
}
