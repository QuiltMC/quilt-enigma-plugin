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
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.util.CommonDescriptors;
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.util.TestUtil.methodOf;

public class ConstructorParamsNameProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("CodecTest-obf");

	@Test
	public void testConstructorParameterNames() {
		final var asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), ConstructorParamsNameProposer.ID);

		final var codecTest = new ClassEntry("com/a/a");
		final MethodEntry constructor = methodOf(codecTest, "<init>", V, I, D, OPT, J);

		asserter.assertDynamicProposal("value", localOf(constructor, 1));
		asserter.assertDynamicProposal("scale", localOf(constructor, 2));
		asserter.assertDynamicProposal("factor", localOf(constructor, 4));
		asserter.assertDynamicProposal("seed", localOf(constructor, 5));
	}
}
