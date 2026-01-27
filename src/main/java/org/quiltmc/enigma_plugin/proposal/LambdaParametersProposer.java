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
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.LambdaParametersIndex;

import java.util.Map;

public class LambdaParametersProposer extends NameProposer {
	public static final String ID = "lambda_params";

	private final LambdaParametersIndex index;

	public LambdaParametersProposer(JarIndexer index) {
		super(ID);
		this.index = index.getIndex(LambdaParametersIndex.class);
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) { }

	@Override
	public void proposeDynamicNames(
			EntryRemapper remapper, Entry<?> obfEntry,
			EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings
	) {
		if (obfEntry == null) {
			this.index.forEachFunctionalParam(((functionalParam, lambdaParams) -> {
				final EntryMapping functionalMapping = remapper.getMapping(functionalParam);
				if (functionalMapping.targetName() != null) {
					lambdaParams.forEach(lambdaParam -> {
						this.insertDynamicProposal(mappings, lambdaParam, functionalMapping.targetName());
					});
				}
			}));
		} else if (obfEntry instanceof LocalVariableEntry local) {
			if (newMapping.targetName() != null) {
				this.index.streamLambdaParams(local).forEach(lambdaParam -> {
					this.insertDynamicProposal(mappings, lambdaParam, newMapping.targetName());
				});
			}
		}
	}
}
