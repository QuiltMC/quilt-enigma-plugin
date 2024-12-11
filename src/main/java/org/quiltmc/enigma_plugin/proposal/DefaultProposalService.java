package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;
import org.quiltmc.enigma_plugin.index.JarIndexer;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeSingleIndex;

public class DefaultProposalService extends NameProposerService {
	public DefaultProposalService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		super();
		this.addIfEnabled(context, indexer, Arguments.DISABLE_RECORDS, RecordComponentNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONSTANT_FIELDS, ConstantFieldNameProposer::new);
		this.addIfEnabled(context, Arguments.DISABLE_EQUALS, EqualsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_LOGGER, LoggerNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CODECS, CodecNameProposer::new);
		this.addIfNotDisabled(context, Arguments.DISABLE_MAP_NON_HASHED, NonHashedNameProposer::new);

		if (indexer.getIndex(SimpleTypeSingleIndex.class).isEnabled()) {
			this.add(indexer, SimpleTypeFieldNameProposer::new);
		}

		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONSTRUCTOR_PARAMS, ConstructorParamsNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_GETTER_SETTER, GetterSetterNameProposer::new);
		this.addIfEnabled(context, indexer, Arguments.DISABLE_DELEGATE_PARAMS, DelegateParametersNameProposer::new);

		// conflict fixer must be last in order to get context from other dynamic proposers
		this.addIfEnabled(context, indexer, Arguments.DISABLE_CONFLICT_FIXER, ConflictFixProposer::new);
	}

	@Override
	public String getId() {
		return QuiltEnigmaPlugin.NAME_PROPOSAL_SERVICE_ID;
	}
}
