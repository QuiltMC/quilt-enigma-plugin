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
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.constant_fields.ConstantFieldIndex;

import java.util.List;
import java.util.Map;

public class ConstantFieldNameProposer extends NameProposer {
	public static final String ID = "constant_fields";
	private final ConstantFieldIndex fieldIndex;

	public ConstantFieldNameProposer(JarIndexer index, @Nullable List<NameProposer> proposerList) {
		super(ID, proposerList);
		this.fieldIndex = index.getIndex(ConstantFieldIndex.class);
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		for (FieldEntry field : this.fieldIndex.getFields()) {
			String name = this.fieldIndex.getName(field);
			this.insertProposal(mappings, field, name);
		}
	}
}
