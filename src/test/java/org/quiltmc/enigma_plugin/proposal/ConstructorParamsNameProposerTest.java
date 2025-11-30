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
import org.quiltmc.enigma_plugin.test.util.CommonDescriptors;

import static org.quiltmc.enigma_plugin.test.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.methodOf;

public class ConstructorParamsNameProposerTest implements ConventionalNameProposerTest, CommonDescriptors {
	private static final String CONSTRUCTOR_PARAMS_TEST_NAME = "com/a/a";

	@Override
	public Class<? extends NameProposer> getTarget() {
		return ConstructorParamsNameProposer.class;
	}

	@Override
	public String getTargetId() {
		return ConstructorParamsNameProposer.ID;
	}

	@Test
	public void testConstructorParameterNames() {
		final var asserter = this.createAsserter();

		final var constructorParamsTest = new ClassEntry(CONSTRUCTOR_PARAMS_TEST_NAME);
		final MethodEntry constructor = methodOf(constructorParamsTest, "<init>", V, I, D, OPT, J);

		asserter.assertDynamicProposal("value", localOf(constructor, 1));
		asserter.assertDynamicProposal("scale", localOf(constructor, 2));
		asserter.assertDynamicProposal("factor", localOf(constructor, 4));
		asserter.assertDynamicProposal("seed", localOf(constructor, 5));
	}
}
