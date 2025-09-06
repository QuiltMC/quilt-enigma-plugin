package org.quiltmc.enigma_plugin.util;

import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

/**
 * Convenient aid for asserting proposals.
 *
 * <p> To ensure test independence, instances should only exist for the duration of a single test.
 *
 * @param remapper the remapper
 * @param proposerId the id of the proposer being tested; used for {@link #assertNotProposedByPlugin(Entry)}
 */
public record ProposalAsserter(EntryRemapper remapper, String proposerId) {
	public void assertProposal(String name, Entry<?> entry) {
		TestUtil.assertProposal(name, entry, this.remapper);
	}

	public void assertDynamicProposal(String name, Entry<?> entry) {
		TestUtil.assertDynamicProposal(name, entry, this.remapper);
	}

	public void assertNotProposed(Entry<?> entry) {
		TestUtil.assertNotProposed(entry, this.remapper);
	}

	public void assertNotProposedByPlugin(Entry<?> entry) {
		TestUtil.assertNotProposedBy(entry, this.proposerId, this.remapper);
	}
}
