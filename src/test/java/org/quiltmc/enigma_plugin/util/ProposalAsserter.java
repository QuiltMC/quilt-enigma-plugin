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
