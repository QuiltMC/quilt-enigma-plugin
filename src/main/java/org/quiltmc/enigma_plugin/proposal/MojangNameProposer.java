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

package org.quiltmc.enigma_plugin.proposal;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.List;
import java.util.Map;

/**
 * If the entry is not deobfuscated, propose the name that's already provided.
 * This is useful to avoid situations where we simply click "mark as deobf" to name something.
 */
public class MojangNameProposer extends NameProposer {
	public static final String ID = "map_non_hashed";

	public MojangNameProposer(@Nullable List<NameProposer> proposerList) {
		super(ID, proposerList);
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		EntryIndex entryIndex = index.getIndex(EntryIndex.class);

		for (FieldEntry field : entryIndex.getFields()) {
			String name = field.getName();
			if (!name.startsWith("f_")) {
				this.insertProposal(mappings, field, name);
			}
		}

		for (MethodEntry method : entryIndex.getMethods()) {
			String name = method.getName();
			if (!name.startsWith("m_") && !method.isConstructor()) {
				this.insertProposal(mappings, method, name);
			}
		}

		for (ClassEntry clazz : entryIndex.getClasses()) {
			if (!clazz.getSimpleName().startsWith("C_")) {
				this.insertProposal(mappings, clazz, clazz.getFullName());
			}
		}
	}
}
