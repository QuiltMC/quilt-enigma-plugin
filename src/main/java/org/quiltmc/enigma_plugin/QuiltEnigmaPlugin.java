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

package org.quiltmc.enigma_plugin;

import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.service.ObfuscationTestService;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.obfuscation.NameObfuscationTestService;
import org.quiltmc.enigma_plugin.proposal.DefaultProposalService;
import org.quiltmc.enigma_plugin.proposal.FallbackProposalService;
import org.quiltmc.enigma_plugin.proposal.UncheckedProposalService;

public class QuiltEnigmaPlugin implements EnigmaPlugin {
	public static final String SERVICE_ID_PREFIX = "quiltmc:";
	public static final String INDEX_SERVICE_ID = SERVICE_ID_PREFIX + "jar_index";
	public static final String NAME_PROPOSAL_SERVICE_ID = SERVICE_ID_PREFIX + "name_proposal";
	public static final String FALLBACK_NAME_PROPOSAL_SERVICE_ID = NAME_PROPOSAL_SERVICE_ID + "/fallback";
	public static final String UNCHECKED_NAME_PROPOSAL_SERVICE_ID = NAME_PROPOSAL_SERVICE_ID + "/unchecked";
	public static final String OBFUSCATION_SERVICE_ID = SERVICE_ID_PREFIX + "obfuscation_test";

	@Override
	public void init(EnigmaPluginContext ctx) {
		var indexer = new JarIndexer();
		ctx.registerService(JarIndexerService.TYPE, indexer::withContext);
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new DefaultProposalService(indexer, ctx1));
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new FallbackProposalService(indexer, ctx1));
		ctx.registerService(NameProposalService.TYPE, ctx1 -> new UncheckedProposalService(indexer, ctx1));
		ctx.registerService(ObfuscationTestService.TYPE, NameObfuscationTestService::new);
	}
}
