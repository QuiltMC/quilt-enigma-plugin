/*
 * Copyright 2024 QuiltMC
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

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma_plugin.util.CommonDescriptors;
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.util.TestUtil.methodOf;

public class RecordComponentNameProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("RecordNamingTest-obf");

	@Test
	public void testRecordNames() {
		final var asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), RecordComponentNameProposer.ID);

		final var recordNamingTest = new ClassEntry("a/a/a");

		asserter.assertProposal("value", fieldOf(recordNamingTest, "b", I));
		asserter.assertProposal("value", methodOf(recordNamingTest, "a", I));

		asserter.assertProposal("scale", fieldOf(recordNamingTest, "c", D));
		asserter.assertProposal("scale", methodOf(recordNamingTest, "b", D));

		asserter.assertProposal("s", fieldOf(recordNamingTest, "d", OPT));
		asserter.assertProposal("s", methodOf(recordNamingTest, "c", OPT));

		// Overridden component getters could return other components, there's no way to be sure which
		// (overridden) getter corresponds to which component
		final var inner1 = new ClassEntry(recordNamingTest, "a");

		// scale override
		asserter.assertNotProposedByPlugin(methodOf(inner1, "a", D));
		// factor override
		asserter.assertNotProposed(methodOf(inner1, "b", D));
		asserter.assertProposal("value", methodOf(inner1, "c", I));
	}

	@Test
	public void testCodecEntryRecordNames() {
		final ProposalAsserter asserter = new ProposalAsserter(
				TestUtil.setupEnigma(TestUtil.obfJarPathOf("CodecTest-obf")),
				RecordComponentNameProposer.ID
		);

		final var codecTestExampleRecord = new ClassEntry("com/a/a$a");

		asserter.assertProposal("value", fieldOf(codecTestExampleRecord, "b", I));
		asserter.assertProposal("value", methodOf(codecTestExampleRecord, "a", I));
	}
}
