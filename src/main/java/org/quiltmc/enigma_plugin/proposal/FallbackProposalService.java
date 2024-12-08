package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;
import org.quiltmc.enigma_plugin.index.JarIndexer;

public class FallbackProposalService extends NameProposerService {
	public FallbackProposalService(JarIndexer indexer, EnigmaServiceContext<NameProposalService> context) {
		super();
		this.addIfEnabled(context, Arguments.DISABLE_MOJMAP, () -> new MojmapNameProposer(context.getSingleArgument(Arguments.MOJMAP_PATH)));
	}

	@Override
	public String getId() {
		return QuiltEnigmaPlugin.FALLBACK_NAME_PROPOSAL_SERVICE_ID;
	}
}
