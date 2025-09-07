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

import static org.quiltmc.enigma_plugin.util.TestUtil.field;
import static org.quiltmc.enigma_plugin.util.TestUtil.localVar;
import static org.quiltmc.enigma_plugin.util.TestUtil.typeDescOf;

public class SimpleTypeFieldNameProposerTest implements CommonDescriptors {
	private static final Path JAR = TestUtil.obfJarPathOf("simple_type_names-obf");

	private static final String VALUE_D_NAME = "a/a/a/a/j";

	private static final String CONFIG = typeDescOf("a/a/a/a/a");
	private static final String POS = typeDescOf("a/a/a/a/b");
	private static final String POSITION = typeDescOf("a/a/a/a/c");
	private static final String RANDOM_POSITION = typeDescOf("a/a/a/a/d");
	private static final String STATE_A = typeDescOf("a/a/a/a/e");
	private static final String STATE_B = typeDescOf("a/a/a/a/f");
	private static final String VALUE_A = typeDescOf("a/a/a/a/g");
	private static final String VALUE_B = typeDescOf("a/a/a/a/h");
	private static final String VALUE_C = typeDescOf("a/a/a/a/i");
	private static final String VALUE_D = typeDescOf(VALUE_D_NAME);
	private static final String VALUE_DD = typeDescOf("a/a/a/a/k");
	private static final String VALUE_E = typeDescOf("a/a/a/a/l");
	private static final String VALUE_EE = typeDescOf("a/a/a/a/m");

	@Test
	public void testSimpleTypeSingleNames() {
		final ProposalAsserter asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), SimpleTypeFieldNameProposer.ID);

		final var simpleTypeNamesTest = new ClassEntry("a/a/a/a");

		final var fields = new ClassEntry(simpleTypeNamesTest, "a");
		final ClassEntry duplicate = new ClassEntry(fields, "a");
		// unnamedValueA
		asserter.assertNotProposed(field(duplicate, "a", VALUE_A));
		// unnamedValueA2
		asserter.assertNotProposed(field(duplicate, "b", VALUE_A));

		final var fallbacks = new ClassEntry(fields, "b");
		asserter.assertProposal("POS", field(fallbacks, "a", POS));
		asserter.assertProposal("position", field(fallbacks, "b", POSITION));
		asserter.assertProposal("randomPosition", field(fallbacks, "c", RANDOM_POSITION));
		asserter.assertProposal("STATIC_STATE_A", field(fallbacks, "d", STATE_A));
		asserter.assertProposal("STATIC_STATE_B", field(fallbacks, "e", STATE_B));
		asserter.assertProposal("VALUE_A", field(fallbacks, "f", VALUE_A));
		asserter.assertProposal("VALUE_B", field(fallbacks, "g", VALUE_B));
		asserter.assertProposal("valueC", field(fallbacks, "h", VALUE_C));

		final var inheritance = new ClassEntry(fields, "c");
		asserter.assertProposal("valueD", field(inheritance, "a", VALUE_DD));
		// valueE
		asserter.assertNotProposed(field(inheritance, "b", VALUE_EE));

		final var unique = new ClassEntry(fields, "d");
		asserter.assertProposal("CONFIG", field(unique, "a", CONFIG));
		asserter.assertProposal("STATIC_STATE", field(unique, "b", STATE_A));
		asserter.assertProposal("value", field(unique, "c", VALUE_C));

		final var parameters = new ClassEntry(simpleTypeNamesTest, "b");

		// method name: config
		asserter.assertProposal("config", localVar(TestUtil.methodOf(parameters, "a", V, CONFIG), 0));

		// method name: pos
		asserter.assertProposal("pos", localVar(TestUtil.methodOf(parameters, "a", V, POS), 1));

		final MethodEntry pos1 = TestUtil.methodOf(parameters, "a", V, POS, POSITION);
		asserter.assertProposal("pos", localVar(pos1, 1));
		asserter.assertProposal("position", localVar(pos1, 2));

		final MethodEntry pos2 = TestUtil.methodOf(parameters, "a", V, POS, RANDOM_POSITION);
		asserter.assertProposal("pos", localVar(pos2, 1));
		asserter.assertProposal("position", localVar(pos2, 2));

		final MethodEntry pos3 = TestUtil.methodOf(parameters, "a", V, POS, POSITION, RANDOM_POSITION);
		asserter.assertProposal("pos", localVar(pos3, 1));
		asserter.assertProposal("position", localVar(pos3, 2));
		asserter.assertProposal("randomPosition", localVar(pos3, 3));

		// method name: state
		asserter.assertProposal("state", localVar(TestUtil.methodOf(parameters, "a", V, STATE_A), 1));

		final MethodEntry state = TestUtil.methodOf(parameters, "a", V, STATE_A, STATE_B);
		asserter.assertProposal("stateA", localVar(state, 0));
		asserter.assertProposal("stateB", localVar(state, 1));

		final MethodEntry value1 = TestUtil.methodOf(parameters, "a", V, VALUE_A, VALUE_B, VALUE_C);
		asserter.assertProposal("valueA", localVar(value1, 0));
		asserter.assertProposal("valueB", localVar(value1, 1));
		asserter.assertProposal("valueC", localVar(value1, 2));

		final MethodEntry value2 = TestUtil.methodOf(parameters, "a", V, VALUE_A, VALUE_A);
		// unnamedValueA
		asserter.assertNotProposed(localVar(value2, 0));
		// unnamedValueA2
		asserter.assertNotProposed(localVar(value2, 1));

		// Check that both the parent and child class have names
		// method name: value
		asserter.assertProposal("valueD", localVar(TestUtil.methodOf(parameters, "a", V, VALUE_D), 0));

		// method name: value
		asserter.assertProposal("valueD", localVar(TestUtil.methodOf(parameters, "a", V, VALUE_DD), 0));

		// Check that just the parent class has a name
		// method name: value
		asserter.assertProposal("valueE", localVar(TestUtil.methodOf(parameters, "a", V, VALUE_E), 0));

		// method name: value; param name: valueD
		asserter.assertNotProposed(localVar(TestUtil.methodOf(parameters, "a", V, VALUE_EE), 0));

		// Check that indexing order doesn't affect inheritance tree
		final ClassEntry valueD = new ClassEntry(VALUE_D_NAME);
		// method name: value
		asserter.assertProposal("valueD", localVar(TestUtil.methodOf(valueD, "a", V, VALUE_D), 0));

		// method name: value
		asserter.assertProposal("valueD", localVar(TestUtil.methodOf(valueD, "a", V, VALUE_DD), 0));
	}
}
