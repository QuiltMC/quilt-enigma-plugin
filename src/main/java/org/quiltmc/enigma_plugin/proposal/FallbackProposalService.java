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

import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;
import org.quiltmc.enigma_plugin.index.JarIndexer;

public class FallbackProposalService extends NameProposerService {
	public FallbackProposalService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		super();
		this.addIfEnabled(context, Arguments.DISABLE_MAPPING_MERGE, () -> new MappingMergeNameProposer(context.getSingleArgument(Arguments.MERGED_MAPPING_PATH).orElse(null)));
	}

	@Override
	public String getId() {
		return QuiltEnigmaPlugin.FALLBACK_NAME_PROPOSAL_SERVICE_ID;
	}

	@Override
	public boolean isFallback() {
		return true;
	}
}
