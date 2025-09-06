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
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.util.TestUtil.localVar;
import static org.quiltmc.enigma_plugin.util.TestUtil.method;

public class DelegateParametersNameProposerTest {
	private static final Path JAR = TestUtil.obfJarPathOf("DELEGATE_PARAMETERS-obf");

	@Test
	public void testDelegateParameterNames() {
		final ProposalAsserter asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), DelegateParametersNameProposer.ID);

		final var delegateParametersTest = new ClassEntry("a/a/a");

		asserter.assertDynamicProposal("seed", localVar(method(delegateParametersTest, "a", "(J)I"), 1));
		asserter.assertDynamicProposal("seed", localVar(method(delegateParametersTest, "a", "(I)V"), 1));
		asserter.assertDynamicProposal("seed", localVar(method(delegateParametersTest, "b", "(J)V"), 1));

		asserter.assertNotProposed(localVar(method(delegateParametersTest, "b", "(I)V"), 1));

		// Multiple parameters passed to the same method call within a single method def shouldn't be named
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "a", "(II)I"), 0));
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "a", "(II)I"), 1));

		asserter.assertNotProposed(localVar(method(delegateParametersTest, "a", "(IIII)I"), 0));
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "a", "(IIII)I"), 1));
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "a", "(IIII)I"), 2));
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "a", "(IIII)I"), 3));

		// If multiple parameters would have the same name, don't name any of them
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "b", "(II)I"), 0));
		asserter.assertNotProposed(localVar(method(delegateParametersTest, "b", "(II)I"), 1));

		final ClassEntry test1 = new ClassEntry(delegateParametersTest, "a");

		asserter.assertDynamicProposal("val", localVar(method(test1, "<init>", "(I)V"), 1));
		asserter.assertDynamicProposal("val", localVar(method(test1, "<init>", "(IJ)V"), 1));
		asserter.assertDynamicProposal("val", localVar(method(test1, "<init>", "(IJI)V"), 1));
		asserter.assertDynamicProposal("val", localVar(method(test1, "a", "(I)La/a/a$a;"), 0));
		asserter.assertDynamicProposal("val", localVar(method(test1, "a", "(IJ)La/a/a$a;"), 0));

		asserter.assertDynamicProposal("j", localVar(method(test1, "<init>", "(IJ)V"), 2));
		asserter.assertDynamicProposal("j", localVar(method(test1, "<init>", "(IJI)V"), 2));
		asserter.assertDynamicProposal("j", localVar(method(test1, "a", "(IJ)La/a/a$a;"), 1));

		asserter.assertDynamicProposal("index", localVar(method(test1, "<init>", "(IJI)V"), 4));

		// Test dynamic renames
		asserter.remapper().putMapping(new ValidationContext(null), localVar(method(test1, "<init>", "(IJI)V"), 2), new EntryMapping("silly"));
		asserter.assertDynamicProposal("silly", localVar(method(test1, "<init>", "(IJ)V"), 2));
		asserter.assertDynamicProposal("silly", localVar(method(test1, "a", "(IJ)La/a/a$a;"), 1));

		asserter.remapper().putMapping(new ValidationContext(null), localVar(method(test1, "<init>", "(IJI)V"), 2), EntryMapping.OBFUSCATED);
		asserter.assertDynamicProposal("j", localVar(method(test1, "<init>", "(IJ)V"), 2));
		asserter.assertDynamicProposal("j", localVar(method(test1, "<init>", "(IJI)V"), 2));
		asserter.assertDynamicProposal("j", localVar(method(test1, "a", "(IJ)La/a/a$a;"), 1));

		// Ensure names are also loaded from external classes
		final ClassEntry getterSetterTest = new ClassEntry("a/a/b");
		asserter.assertDynamicProposal("bound", localVar(method(getterSetterTest, "b", "(I)V"), 1));

		// Names shouldn't be proposed for synthetic parameters (like Enum constructor name and ordinal)
		final var enumTest = new ClassEntry("a/a/a/a");
		final MethodEntry constructor = method(enumTest, "<init>", "(Ljava/lang/String;IZ)V");

		asserter.assertNotProposed(localVar(constructor, 1));
		asserter.assertNotProposed(localVar(constructor, 2));
	}
}
