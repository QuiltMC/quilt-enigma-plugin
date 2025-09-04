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

package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.index.DelegatingMethodIndex;
import org.quiltmc.enigma_plugin.index.JarIndexer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelegatingMethodNameProposer extends NameProposer {
	public static final String ID = "delegating_method";

	final DelegatingMethodIndex index;

	public DelegatingMethodNameProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(DelegatingMethodIndex.class);
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) { }

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (obfEntry == null) {
			this.index.forEachDelegation((delegate, delegaters) -> {
				final EntryMapping mapping = remapper.getMapping(delegate);
				if (mapping.targetName() != null) {
					delegaters.forEach(delegater ->
							this.insertDelegaterName(delegater, remapper, mappings, mapping.targetName())
					);
				}
			});
		} else if (obfEntry instanceof MethodEntry method) {
			if (newMapping.targetName() != null) {
				this.index.streamDelegaters(method).forEach(delegater ->
						this.insertDelegaterName(delegater, remapper, mappings, newMapping.targetName())
				);
			}
		}
	}

	private void insertDelegaterName(MethodEntry delegater, EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, String name) {
		this.insertDelegaterNameImpl(delegater, new HashSet<>(), remapper, mappings, new EntryMapping(name));
	}

	private void insertDelegaterNameImpl(
			MethodEntry delegater, Set<List<TypeDescriptor>> delegateParamDescriptors,
			EntryRemapper remapper, Map<Entry<?>, EntryMapping> mappings, EntryMapping mapping
	) {
		// stop propagation if delegater is named or would conflict with a delegate earlier in the chain
		if (remapper.getMapping(delegater).targetName() == null && delegateParamDescriptors.add(delegater.getDesc().getTypeDescs())) {
			this.insertDynamicProposal(mappings, delegater, mapping);
			// recur down the chain of delegaters
			this.index.streamDelegaters(delegater).forEach(outerDelegater ->
					this.insertDelegaterNameImpl(outerDelegater, delegateParamDescriptors, remapper, mappings, mapping)
			);
		}
	}
}
