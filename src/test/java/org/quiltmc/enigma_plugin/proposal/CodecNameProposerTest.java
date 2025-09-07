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

import static org.quiltmc.enigma_plugin.util.TestUtil.field;
import static org.quiltmc.enigma_plugin.util.TestUtil.methodOf;

public class CodecNameProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("CodecTest-obf");

	@Test
	public void testCodecNames() {
		final var asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), CodecNameProposer.ID);

		final var codecTest = new ClassEntry("com/a/a");

		asserter.assertProposal("value", field(codecTest, "c", I));
		asserter.assertProposal("getValue", methodOf(codecTest, "a", I));

		asserter.assertProposal("scale", field(codecTest, "d", D));
		asserter.assertProposal("getScale", methodOf(codecTest, "b", D));

		asserter.assertProposal("factor", field(codecTest, "e", OPT));
		asserter.assertProposal("getFactor", methodOf(codecTest, "c", OPT));

		asserter.assertProposal("seed", field(codecTest, "b", J));
	}
}
