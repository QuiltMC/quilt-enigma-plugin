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
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.quiltmc.enigma_plugin.test.util.CommonDescriptors;

import static org.quiltmc.enigma_plugin.test.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.methodOf;

public class GetterSetterNameProposerTest implements ConventionalNameProposerTest, CommonDescriptors {
	private static final String GETTER_SETTER_TEST_NAME = "a/a/a";

	@Override
	public Class<? extends NameProposer> getTarget() {
		return GetterSetterNameProposer.class;
	}

	@Override
	public String getTargetId() {
		return GetterSetterNameProposer.ID;
	}

	@Test
	public void testGetterSetterNames() {
		final var asserter = this.createAsserter();

		final var getterSetterTest = new ClassEntry(GETTER_SETTER_TEST_NAME);

		final var context = new ValidationContext(null);
		asserter.remapper().putMapping(context, fieldOf(getterSetterTest, "a", "I"), new EntryMapping("silliness"));
		asserter.remapper().putMapping(context, fieldOf(getterSetterTest, "b", "Ljava/lang/String;"), new EntryMapping("name"));

		asserter.assertDynamicProposal("getSilliness", methodOf(getterSetterTest, "a", I));

		final MethodEntry setSilliness = methodOf(getterSetterTest, "a", V, I);
		asserter.assertDynamicProposal("setSilliness", setSilliness);
		asserter.assertDynamicProposal("silliness", localOf(setSilliness, 1));

		asserter.assertDynamicProposal("getName", methodOf(getterSetterTest, "b", STR));

		final MethodEntry setName = methodOf(getterSetterTest, "b", V, STR);
		asserter.assertDynamicProposal("setName", setName);
		asserter.assertDynamicProposal("name", localOf(setName, 1));
	}
}
