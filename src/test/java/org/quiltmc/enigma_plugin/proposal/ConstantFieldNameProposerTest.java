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

import static org.quiltmc.enigma_plugin.test.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.typeDescOf;

public class ConstantFieldNameProposerTest implements ConventionalNameProposerTest {
	private static final String CLASS_TEST_NAME = "a/a/c";
	private static final String CLASS_2_TEST_NAME = "a/a/a";
	private static final String CLASS_3_TEST_NAME = "a/a/b";
	private static final String SOMETHING_NAME = "a/a/f";
	private static final String ENUM_TEST_NAME = "a/a/e";
	private static final String ENUM_2_TEST_NAME = "a/a/d";

	@Override
	public Class<? extends NameProposer> getTarget() {
		return ConstantFieldNameProposer.class;
	}

	@Override
	public String getTargetId() {
		return ConstantFieldNameProposer.ID;
	}

	@Test
	public void testConstantFieldNames() {
		final var asserter = this.createAsserter();

		final var classTest = new ClassEntry(CLASS_TEST_NAME);
		final String something = typeDescOf(SOMETHING_NAME);

		asserter.assertProposal("FOO", fieldOf(classTest, "a", something));
		asserter.assertProposal("BAR_FOO", fieldOf(classTest, "b", something));
		asserter.assertProposal("BAZ_FOO", fieldOf(classTest, "c", something));
		asserter.assertProposal("LOREM_IPSUM_BAZ", fieldOf(classTest, "d", something));
		asserter.assertProposal("AN_ID", fieldOf(classTest, "e", something));
		asserter.assertProposal("ANOTHER_ID_FOO", fieldOf(classTest, "f", something));
		asserter.assertProposal("ONE", fieldOf(classTest, "g", something));
		asserter.assertProposal("TWO", fieldOf(classTest, "h", something));
		asserter.assertProposal("THREE", fieldOf(classTest, "i", something));
		// don't propose 123ABC for INVALID
		asserter.assertNotProposed(fieldOf(classTest, "j", something));

		final var class2Test = new ClassEntry(CLASS_2_TEST_NAME);

		asserter.assertProposal("FOO", fieldOf(class2Test, "a", something));
		asserter.assertProposal("BAR_FOO", fieldOf(class2Test, "b", something));
		asserter.assertProposal("AN_ID", fieldOf(class2Test, "c", something));
		asserter.assertProposal("NORTH", fieldOf(class2Test, "d", something));
		asserter.assertProposal("EAST", fieldOf(class2Test, "e", something));

		final var class3Test = new ClassEntry(CLASS_3_TEST_NAME);

		asserter.assertProposal("FOO", fieldOf(class3Test, "a", something));
		asserter.assertProposal("BAR_FOO", fieldOf(class3Test, "b", something));

		final var enumTest = new ClassEntry(ENUM_TEST_NAME);
		final String enumDesc = typeDescOf(ENUM_TEST_NAME);

		asserter.assertProposal("NORTH", fieldOf(enumTest, "a", enumDesc));
		asserter.assertProposal("EAST", fieldOf(enumTest, "b", enumDesc));
		asserter.assertProposal("WEST", fieldOf(enumTest, "c", enumDesc));
		asserter.assertProposal("SOUTH", fieldOf(enumTest, "d", enumDesc));

		final var enum2Test = new ClassEntry(ENUM_2_TEST_NAME);
		final String enum2Desc = typeDescOf(ENUM_2_TEST_NAME);

		asserter.assertProposal("CONSTANT", fieldOf(enum2Test, "a", enum2Desc));
		asserter.assertProposal("INT", fieldOf(enum2Test, "b", enum2Desc));
		asserter.assertProposal("DOUBLE", fieldOf(enum2Test, "c", enum2Desc));
		asserter.assertProposal("EXPONENTIAL", fieldOf(enum2Test, "d", enum2Desc));
		asserter.assertProposal("GAUSSIAN", fieldOf(enum2Test, "e", enum2Desc));
	}
}
