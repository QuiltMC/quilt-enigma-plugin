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
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleSubtypeSingleIndex;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeSingleIndex;

public class DefaultProposalService extends NameProposerService {
	public DefaultProposalService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		super();
		this.addIfEnabled(context, indexer, Arguments.DISABLE_RECORDS, RecordComponentNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONSTANT_FIELDS, ConstantFieldNameProposer::new);
		this.addIfEnabled(context, Arguments.DISABLE_EQUALS, EqualsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_LOGGER, LoggerNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CODECS, CodecNameProposer::new);

		if (indexer.getIndex(SimpleTypeSingleIndex.class).isEnabled()) {
			this.add(indexer, SimpleTypeFieldNameProposer::new);
		}

		if (indexer.getIndex(SimpleSubtypeSingleIndex.class).isEnabled()) {
			this.add(indexer, SimpleSubtypeFieldNameProposer::new);
		}

		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONSTRUCTOR_PARAMS, ConstructorParamsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_GETTER_SETTER, GetterSetterNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_DELEGATE_PARAMS, DelegateParametersNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_LAMBDA_PARAMS, LambdaParametersProposer::new);

		// conflict fixer must be last in order to get context from other dynamic proposers
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONFLICT_FIXER, ConflictFixProposer::new);
	}

	@Override
	public String getId() {
		return QuiltEnigmaPlugin.NAME_PROPOSAL_SERVICE_ID;
	}
}
