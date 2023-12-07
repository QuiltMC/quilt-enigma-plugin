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

package org.quiltmc.enigmaplugin;

import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.service.ObfuscationTestService;
import org.quiltmc.enigmaplugin.index.JarIndexer;
import org.quiltmc.enigmaplugin.obfuscation.NameObfuscationTestService;
import org.quiltmc.enigmaplugin.proposal.NameProposerService;

public class QuiltEnigmaPlugin implements EnigmaPlugin {
	public static final String SERVICE_ID_PREFIX = "quiltmc:";
	public static final String INDEX_SERVICE_NAME = "jar_index";
	public static final String NAME_PROPOSAL_SERVICE_NAME = "name_proposal";
	public static final String NAME_PROPOSAL_SERVICE_ID = SERVICE_ID_PREFIX + NAME_PROPOSAL_SERVICE_NAME;
	public static final String OBFUSCATION_SERVICE_NAME = "obfuscation_test";

	@Override
	public void init(EnigmaPluginContext ctx) {
		var indexer = new JarIndexer();
		ctx.registerService(SERVICE_ID_PREFIX + INDEX_SERVICE_NAME, JarIndexerService.TYPE, indexer::withContext);
		ctx.registerService(NAME_PROPOSAL_SERVICE_ID, NameProposalService.TYPE, ctx1 -> new NameProposerService(indexer, ctx1));
		ctx.registerService(SERVICE_ID_PREFIX + OBFUSCATION_SERVICE_NAME, ObfuscationTestService.TYPE, NameObfuscationTestService::new);
	}
}
