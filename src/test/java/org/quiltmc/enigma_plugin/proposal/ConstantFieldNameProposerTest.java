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
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

import static org.quiltmc.enigma_plugin.util.TestUtil.field;
import static org.quiltmc.enigma_plugin.util.TestUtil.typeDescOf;

public class ConstantFieldNameProposerTest {
	private static final Path JAR = TestUtil.obfJarPathOf("field_names-obf");

	private static final String ENUM_TEST_NAME = "a/a/a/e";
	private static final String ENUM_2_TEST_NAME = "a/a/a/d";

	@Test
	public void testConstantFieldNames() {
		final var asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), ConstantFieldNameProposer.ID);

		final var classTest = new ClassEntry("a/a/a/c");
		final String something = typeDescOf("a/a/a/f");

		asserter.assertProposal("FOO", field(classTest, "a", something));
		asserter.assertProposal("BAR_FOO", field(classTest, "b", something));
		asserter.assertProposal("BAZ_FOO", field(classTest, "c", something));
		asserter.assertProposal("LOREM_IPSUM_BAZ", field(classTest, "d", something));
		asserter.assertProposal("AN_ID", field(classTest, "e", something));
		asserter.assertProposal("ANOTHER_ID_FOO", field(classTest, "f", something));
		asserter.assertProposal("ONE", field(classTest, "g", something));
		asserter.assertProposal("TWO", field(classTest, "h", something));
		asserter.assertProposal("THREE", field(classTest, "i", something));

		final var class2Test = new ClassEntry("a/a/a/a");

		asserter.assertProposal("FOO", field(class2Test, "a", something));
		asserter.assertProposal("BAR_FOO", field(class2Test, "b", something));
		asserter.assertProposal("AN_ID", field(class2Test, "c", something));
		asserter.assertProposal("NORTH", field(class2Test, "d", something));
		asserter.assertProposal("EAST", field(class2Test, "e", something));

		final var class3Test = new ClassEntry("a/a/a/b");

		asserter.assertProposal("FOO", field(class3Test, "a", something));
		asserter.assertProposal("BAR_FOO", field(class3Test, "b", something));

		final var enumTest = new ClassEntry(ENUM_TEST_NAME);
		final String enumDesc = typeDescOf(ENUM_TEST_NAME);

		asserter.assertProposal("NORTH", field(enumTest, "a", enumDesc));
		asserter.assertProposal("EAST", field(enumTest, "b", enumDesc));
		asserter.assertProposal("WEST", field(enumTest, "c", enumDesc));
		asserter.assertProposal("SOUTH", field(enumTest, "d", enumDesc));

		final var enum2Test = new ClassEntry(ENUM_2_TEST_NAME);
		final String enum2Desc = typeDescOf(ENUM_2_TEST_NAME);

		asserter.assertProposal("CONSTANT", field(enum2Test, "a", enum2Desc));
		asserter.assertProposal("INT", field(enum2Test, "b", enum2Desc));
		asserter.assertProposal("DOUBLE", field(enum2Test, "c", enum2Desc));
		asserter.assertProposal("EXPONENTIAL", field(enum2Test, "d", enum2Desc));
		asserter.assertProposal("GAUSSIAN", field(enum2Test, "e", enum2Desc));
	}
}
