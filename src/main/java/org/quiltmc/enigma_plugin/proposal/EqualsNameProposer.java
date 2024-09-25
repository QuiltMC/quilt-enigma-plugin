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
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.List;
import java.util.Map;

public class EqualsNameProposer extends NameProposer {
	public static final String ID = "equals";
	private static final MethodDescriptor EQUALS_DESCRIPTOR = new MethodDescriptor("(Ljava/lang/Object;)Z");

	public EqualsNameProposer(@Nullable List<NameProposer> proposerList) {
		super(ID, proposerList);
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		EntryIndex entryIndex = index.getIndex(EntryIndex.class);

		for (MethodEntry method : entryIndex.getMethods()) {
			String methodName = method.getName();
			if (methodName.equals("equals") && method.getDesc().equals(EQUALS_DESCRIPTOR)) {
				LocalVariableEntry param = new LocalVariableEntry(method, 1);
				this.insertProposal(mappings, param, "o");
			}
		}
	}
}
