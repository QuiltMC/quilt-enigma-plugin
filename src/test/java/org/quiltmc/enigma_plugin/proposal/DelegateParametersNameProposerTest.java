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

import static org.quiltmc.enigma_plugin.util.TestUtil.localVar;
import static org.quiltmc.enigma_plugin.util.TestUtil.methodOf;
import static org.quiltmc.enigma_plugin.util.TestUtil.typeDescOf;

public class DelegateParametersNameProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("DELEGATE_PARAMETERS-obf");
	private static final String DELEGATE_PARAMETERS_TEST_NAME = "a/a/a";
	private static final String TEST_1 = typeDescOf(DELEGATE_PARAMETERS_TEST_NAME + "$a");

	@Test
	public void testDelegateParameterNames() {
		final ProposalAsserter asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), DelegateParametersNameProposer.ID);

		final var delegateParametersTest = new ClassEntry(DELEGATE_PARAMETERS_TEST_NAME);

		asserter.assertDynamicProposal("seed", localVar(methodOf(delegateParametersTest, "a", I, J), 1));
		asserter.assertDynamicProposal("seed", localVar(methodOf(delegateParametersTest, "a", V, I), 1));
		asserter.assertDynamicProposal("seed", localVar(methodOf(delegateParametersTest, "b", V, J), 1));

		// method name: bar
		asserter.assertNotProposed(localVar(methodOf(delegateParametersTest, "b", V, I), 1));

		// Multiple parameters passed to the same method call within a single method def shouldn't be named
		final MethodEntry addTwoAbs = methodOf(delegateParametersTest, "a", I, I, I);
		asserter.assertNotProposed(localVar(addTwoAbs, 0));
		asserter.assertNotProposed(localVar(addTwoAbs, 1));

		final MethodEntry addFourAbs = methodOf(delegateParametersTest, "a", I, I, I, I, I);
		asserter.assertNotProposed(localVar(addFourAbs, 0));
		asserter.assertNotProposed(localVar(addFourAbs, 1));
		asserter.assertNotProposed(localVar(addFourAbs, 2));
		asserter.assertNotProposed(localVar(addFourAbs, 3));

		// If multiple parameters would have the same name, don't name any of them
		final MethodEntry twoAbs = methodOf(delegateParametersTest, "b", I, I, I);
		asserter.assertNotProposed(localVar(twoAbs, 0));
		asserter.assertNotProposed(localVar(twoAbs, 1));

		final ClassEntry test1 = new ClassEntry(delegateParametersTest, "a");

		final MethodEntry constructorIJ = methodOf(test1, "<init>", V, I, J);
		final MethodEntry constructorIJI = methodOf(test1, "<init>", V, I, J, I);
		final MethodEntry createIJ = methodOf(test1, "a", TEST_1, I, J);

		asserter.assertDynamicProposal("val", localVar(methodOf(test1, "<init>", V, I), 1));
		asserter.assertDynamicProposal("val", localVar(constructorIJ, 1));
		asserter.assertDynamicProposal("val", localVar(constructorIJI, 1));
		// method name: create
		asserter.assertDynamicProposal("val", localVar(methodOf(test1, "a", TEST_1, I), 0));

		asserter.assertDynamicProposal("val", localVar(createIJ, 0));

		asserter.assertDynamicProposal("j", localVar(constructorIJ, 2));
		asserter.assertDynamicProposal("j", localVar(constructorIJI, 2));
		asserter.assertDynamicProposal("j", localVar(createIJ, 1));

		asserter.assertDynamicProposal("index", localVar(constructorIJI, 4));

		// Test dynamic renames
		asserter.remapper().putMapping(new ValidationContext(null), localVar(constructorIJI, 2), new EntryMapping("silly"));
		asserter.assertDynamicProposal("silly", localVar(constructorIJ, 2));
		asserter.assertDynamicProposal("silly", localVar(createIJ, 1));

		asserter.remapper().putMapping(new ValidationContext(null), localVar(constructorIJI, 2), EntryMapping.OBFUSCATED);
		asserter.assertDynamicProposal("j", localVar(constructorIJ, 2));
		asserter.assertDynamicProposal("j", localVar(constructorIJI, 2));
		asserter.assertDynamicProposal("j", localVar(createIJ, 1));

		// Ensure names are also loaded from external classes
		final ClassEntry getterSetterTest = new ClassEntry("a/a/b");
		// method name: foo
		asserter.assertDynamicProposal("bound", localVar(methodOf(getterSetterTest, "b", V, I), 1));

		// Names shouldn't be proposed for synthetic parameters (like Enum constructor name and ordinal)
		final var enumTest = new ClassEntry("a/a/a/a");
		final MethodEntry constructor = methodOf(enumTest, "<init>", V, STR, I, Z);

		asserter.assertNotProposed(localVar(constructor, 1));
		asserter.assertNotProposed(localVar(constructor, 2));
	}
}
