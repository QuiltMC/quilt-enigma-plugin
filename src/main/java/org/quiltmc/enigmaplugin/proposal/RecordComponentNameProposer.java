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

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigmaplugin.index.JarIndexer;
import org.quiltmc.enigmaplugin.index.RecordIndex;

import java.util.Map;

public class RecordComponentNameProposer extends NameProposer {
	public static final String ID = "records";
	private final RecordIndex index;

	public RecordComponentNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getRecordIndex();
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		for (ClassEntry recordClass : this.index.getRecordClasses()) {
			for (FieldEntry field : this.index.getFields(recordClass)) {
				String name = this.index.getFieldName(recordClass, field);

				this.insertProposal(mappings, field, name);
			}

			for (MethodEntry method : this.index.getMethods(recordClass)) {
				String name = this.index.getAccessorMethodName(recordClass, method);

				this.insertProposal(mappings, method, name);
			}
		}
	}

}
