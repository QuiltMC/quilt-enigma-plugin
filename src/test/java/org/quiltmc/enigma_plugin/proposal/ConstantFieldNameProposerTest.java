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
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma_plugin.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.util.TestUtil;

import java.nio.file.Path;

public class ConstantFieldNameProposerTest {
	private static final Path JAR = TestUtil.obfJarPathOf("field_names-obf");

	@Test
	public void testConstantFieldNames() {
		final ProposalAsserter asserter = new ProposalAsserter(TestUtil.setupEnigma(JAR), ConstantFieldNameProposer.ID);

		var classTest = new ClassEntry("a/a/a/c");
		var desc = new TypeDescriptor("La/a/a/f;");

		asserter.assertProposal("FOO", new FieldEntry(classTest, "a", desc));
		asserter.assertProposal("BAR_FOO", new FieldEntry(classTest, "b", desc));
		asserter.assertProposal("BAZ_FOO", new FieldEntry(classTest, "c", desc));
		asserter.assertProposal("LOREM_IPSUM_BAZ", new FieldEntry(classTest, "d", desc));
		asserter.assertProposal("AN_ID", new FieldEntry(classTest, "e", desc));
		asserter.assertProposal("ANOTHER_ID_FOO", new FieldEntry(classTest, "f", desc));
		asserter.assertProposal("ONE", new FieldEntry(classTest, "g", desc));
		asserter.assertProposal("TWO", new FieldEntry(classTest, "h", desc));
		asserter.assertProposal("THREE", new FieldEntry(classTest, "i", desc));

		var class2Test = new ClassEntry("a/a/a/a");

		asserter.assertProposal("FOO", new FieldEntry(class2Test, "a", desc));
		asserter.assertProposal("BAR_FOO", new FieldEntry(class2Test, "b", desc));
		asserter.assertProposal("AN_ID", new FieldEntry(class2Test, "c", desc));
		asserter.assertProposal("NORTH", new FieldEntry(class2Test, "d", desc));
		asserter.assertProposal("EAST", new FieldEntry(class2Test, "e", desc));

		var class3Test = new ClassEntry("a/a/a/b");

		asserter.assertProposal("FOO", new FieldEntry(class3Test, "a", desc));
		asserter.assertProposal("BAR_FOO", new FieldEntry(class3Test, "b", desc));

		var enumTest = new ClassEntry("a/a/a/e");
		var enumDesc = new TypeDescriptor("La/a/a/e;");

		asserter.assertProposal("NORTH", new FieldEntry(enumTest, "a", enumDesc));
		asserter.assertProposal("EAST", new FieldEntry(enumTest, "b", enumDesc));
		asserter.assertProposal("WEST", new FieldEntry(enumTest, "c", enumDesc));
		asserter.assertProposal("SOUTH", new FieldEntry(enumTest, "d", enumDesc));

		var enum2Test = new ClassEntry("a/a/a/d");
		var enum2Desc = new TypeDescriptor("La/a/a/d;");

		asserter.assertProposal("CONSTANT", new FieldEntry(enum2Test, "a", enum2Desc));
		asserter.assertProposal("INT", new FieldEntry(enum2Test, "b", enum2Desc));
		asserter.assertProposal("DOUBLE", new FieldEntry(enum2Test, "c", enum2Desc));
		asserter.assertProposal("EXPONENTIAL", new FieldEntry(enum2Test, "d", enum2Desc));
		asserter.assertProposal("GAUSSIAN", new FieldEntry(enum2Test, "e", enum2Desc));
	}
}
