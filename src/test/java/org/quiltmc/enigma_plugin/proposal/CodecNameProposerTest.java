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
import org.quiltmc.enigma_plugin.test.util.CommonDescriptors;

import static org.quiltmc.enigma_plugin.test.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.methodOf;

public class CodecNameProposerTest implements ConventionalNameProposerTest, CommonDescriptors {
	@Override
	public Class<? extends NameProposer> getTarget() {
		return CodecNameProposer.class;
	}

	@Override
	public String getTargetId() {
		return CodecNameProposer.ID;
	}

	@Test
	public void testCodecNames() {
		final var asserter = this.createAsserter();

		final var codecTest = new ClassEntry("com/a/a");

		asserter.assertProposal("value", fieldOf(codecTest, "c", I));
		asserter.assertProposal("getValue", methodOf(codecTest, "a", I));

		asserter.assertProposal("scale", fieldOf(codecTest, "d", D));
		asserter.assertProposal("getScale", methodOf(codecTest, "b", D));

		asserter.assertProposal("factor", fieldOf(codecTest, "e", OPT));
		asserter.assertProposal("getFactor", methodOf(codecTest, "c", OPT));

		asserter.assertProposal("seed", fieldOf(codecTest, "b", J));
	}
}
