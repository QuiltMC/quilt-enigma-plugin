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
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.util.CommonDescriptors;
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.util.TestUtil.field;
import static org.quiltmc.enigma_plugin.util.TestUtil.javaLangDescOf;
import static org.quiltmc.enigma_plugin.util.TestUtil.localVar;
import static org.quiltmc.enigma_plugin.util.TestUtil.method;
import static org.quiltmc.enigma_plugin.util.TestUtil.methodOf;

public class ConflictFixProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("z_conflicts-obf");

	@Test
	public void testSimpleTypeNameConflictFix() {
		final var asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), ConflictFixProposer.ID);

		// tests the conflict fixer via introducing a conflict manually

		final var conflictTest = new ClassEntry("a/a/a/a");
		final MethodEntry constructor = methodOf(conflictTest, "<init>", V, I, javaLangDescOf("CharSequence"));

		// param 2 is initially 'id'
		asserter.assertProposal("id", localVar(constructor, 2));

		// fires dynamic proposal for the constructor parameter, creating a conflict
		// the conflict should then be automatically fixed by moving to the 'identifier' name
		// note we bypass putMapping so that we can create a conflict
		asserter.remapper().getMappings().insert(field(conflictTest, "a", I), new EntryMapping("id"));
		asserter.remapper().insertDynamicallyProposedMappings(field(conflictTest, "a", I), EntryMapping.OBFUSCATED, new EntryMapping("id"));

		asserter.assertDynamicProposal("id", localVar(constructor, 1));
		asserter.assertDynamicProposal("identifier", localVar(constructor, 2));
	}

	@Test
	public void testSimpleTypeNameConflictFixNoFallback() {
		final ProposalAsserter asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), ConflictFixProposer.ID);

		final var conflictTest = new ClassEntry("a/a/a/a");
		final MethodEntry constructor = method(conflictTest, "<init>", "(ILjava/lang/StringBuilder;)V");

		// param 2 is initially 'id'
		asserter.assertProposal("stringBuilder", localVar(constructor, 2));

		// fires dynamic proposal for the constructor parameter, creating a conflict
		// the conflict should then be automatically fixed via removing the name
		// note we bypass putMapping so that we can create a conflict
		final FieldEntry backingField = field(conflictTest, "a", "I");
		final EntryMapping mapping = new EntryMapping("stringBuilder");
		asserter.remapper().getMappings().insert(backingField, mapping);
		asserter.remapper().insertDynamicallyProposedMappings(backingField, EntryMapping.OBFUSCATED, mapping);

		asserter.assertDynamicProposal("stringBuilder", localVar(constructor, 1));
		asserter.assertNotProposed(localVar(constructor, 2));
	}
}
