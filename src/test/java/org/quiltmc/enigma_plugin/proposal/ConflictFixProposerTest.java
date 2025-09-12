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
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.test.util.CommonDescriptors;
import org.quiltmc.enigma_plugin.test.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.test.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.test.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.methodOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.typeDescOf;

public class ConflictFixProposerTest implements ConventionalNameProposerTest, CommonDescriptors {
	private static final Path ENIGMA_PROFILE = TestUtil.enigmaProfileOf("conflict_fix/profile");

	private static final String CONFLICT_TEST_NAME = "a/a/a";

	private static final String IDENTIFIABLE = typeDescOf("a/a/b");
	private static final String IDENTIFIED = typeDescOf("a/a/c");

	@Override
	public Class<? extends NameProposer> getTarget() {
		return ConflictFixProposer.class;
	}

	@Override
	public String getTargetId() {
		return ConflictFixProposer.ID;
	}

	@Override
	public Path getEnigmaProfile() {
		return ENIGMA_PROFILE;
	}

	@Test
	public void testSimpleTypeNameConflictFix() {
		final var asserter = this.createAsserter();

		// tests the conflict fixer via introducing a conflict manually

		final var conflictTest = new ClassEntry(CONFLICT_TEST_NAME);
		final MethodEntry constructor = methodOf(conflictTest, "<init>", V, I, IDENTIFIABLE);

		final String simpleName = "idAble";
		final LocalVariableEntry param2 = localOf(constructor, 2);

		// param 2 is initially 'idAble'
		asserter.assertProposal(simpleName, param2);

		// fires dynamic proposal for the constructor parameter, creating a conflict
		// the conflict should then be automatically fixed by moving to the 'identifiable' name
		// note we bypass putMapping so that we can create a conflict
		final FieldEntry integer = fieldOf(conflictTest, "a", I);
		asserter.remapper().getMappings().insert(integer, new EntryMapping(simpleName));
		asserter.remapper().insertDynamicallyProposedMappings(integer, EntryMapping.OBFUSCATED, new EntryMapping(simpleName));

		asserter.assertDynamicProposal(simpleName, localOf(constructor, 1));
		// fallback name
		asserter.assertDynamicProposal("identifiable", param2);
	}

	@Test
	public void testSimpleTypeNameConflictFixNoFallback() {
		final ProposalAsserter asserter = this.createAsserter();

		final var conflictTest = new ClassEntry(CONFLICT_TEST_NAME);
		final MethodEntry constructor = methodOf(conflictTest, "<init>", V, I, IDENTIFIED);

		final String simpleName = "identified";
		final LocalVariableEntry param2 = localOf(constructor, 2);

		// param 2 is initially 'identified'
		asserter.assertProposal(simpleName, param2);

		// fires dynamic proposal for the constructor parameter, creating a conflict
		// the conflict should then be automatically fixed via removing the name
		// note we bypass putMapping so that we can create a conflict
		final FieldEntry backingField = fieldOf(conflictTest, "a", I);
		final EntryMapping mapping = new EntryMapping(simpleName);
		asserter.remapper().getMappings().insert(backingField, mapping);
		asserter.remapper().insertDynamicallyProposedMappings(backingField, EntryMapping.OBFUSCATED, mapping);

		asserter.assertDynamicProposal(simpleName, localOf(constructor, 1));
		asserter.assertNotProposed(param2);
	}
}
