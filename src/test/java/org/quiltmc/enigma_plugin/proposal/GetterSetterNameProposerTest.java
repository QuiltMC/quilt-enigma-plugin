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
import org.quiltmc.enigma_plugin.util.CommonDescriptors;
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.util.TestUtil.methodOf;

public class GetterSetterNameProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("GetterSetterTest-obf");

	@Test
	public void testGetterSetterNames() {
		final var asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), GetterSetterNameProposer.ID);

		final var getterSetterTest = new ClassEntry("a/a/a");

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
