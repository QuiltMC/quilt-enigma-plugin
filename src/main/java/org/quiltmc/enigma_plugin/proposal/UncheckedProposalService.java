package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;
import org.quiltmc.enigma_plugin.index.JarIndexer;

public class UncheckedProposalService  extends NameProposerService {
	public UncheckedProposalService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		super();
		this.addIfEnabled(context, Arguments.DISABLE_MOJMAP, MojmapPackageProposer::new);
	}

	@Override
	public String getId() {
		return QuiltEnigmaPlugin.UNCHECKED_NAME_PROPOSAL_SERVICE_ID;
	}

	@Override
	public boolean bypassValidation() {
		return true;
	}
}
