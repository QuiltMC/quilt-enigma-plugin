/*
 * Copyright 2025 QuiltMC
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
import org.quiltmc.enigma_plugin.test.util.ProposalAsserter;

import static org.quiltmc.enigma_plugin.test.util.TestUtil.fieldOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.methodOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.typeDescOf;

public class SimpleSubtypeFieldNameProposerTest implements ConventionalNameProposerTest, CommonDescriptors {
	private static final String SIMPLE_SUBTYPE_NAMES_TEST_NAME = "a/a/a";

	private static final String ENTITY_NAME = "a/a/b/b";
	private static final String ENTITY_NON_SUFFIXED_NAME = "a/a/b/c";
	private static final String LIVING_ENTITY_NAME = "a/a/b/d";
	private static final String BIRD_ENTITY_NAME = "a/a/b/a";
	private static final String WHILE_ENTITY_NAME = "a/a/b/e";
	private static final String SPECIFIC_ENTITY_NAME = "a/a/b/a/c";
	private static final String ENTITY_MARKER_NAME = "a/a/b/a/a";
	private static final String ENTITY_SPECIFIC_ENTITY_NAME = "a/a/b/a/b";

	private static final String BLOCK_ENTITY_RENDERER_NAME = "a/a/a/a";
	private static final String DISPLAY_BLOCK_ENTITY_RENDERER_NAME = "a/a/a/b";
	private static final String PAINTING_BLOCK_ENTITY_RENDERER_NAME = "a/a/a/d";
	private static final String DUPLICATE_BLOCK_ENTITY_RENDERER_NAME = "a/a/a/c";

	private static final String ENTITY = typeDescOf(ENTITY_NAME);
	private static final String ENTITY_NON_SUFFIXED = typeDescOf(ENTITY_NON_SUFFIXED_NAME);
	private static final String LIVING_ENTITY = typeDescOf(LIVING_ENTITY_NAME);
	private static final String BIRD_ENTITY = typeDescOf(BIRD_ENTITY_NAME);
	private static final String WHILE_ENTITY = typeDescOf(WHILE_ENTITY_NAME);
	private static final String SPECIFIC_ENTITY = typeDescOf(SPECIFIC_ENTITY_NAME);
	private static final String ENTITY_MARKER = typeDescOf(ENTITY_MARKER_NAME);
	private static final String ENTITY_SPECIFIC_ENTITY = typeDescOf(ENTITY_SPECIFIC_ENTITY_NAME);

	private static final String BLOCK_ENTITY_RENDERER = typeDescOf(BLOCK_ENTITY_RENDERER_NAME);
	private static final String DISPLAY_BLOCK_ENTITY_RENDERER = typeDescOf(DISPLAY_BLOCK_ENTITY_RENDERER_NAME);
	private static final String PAINTING_BLOCK_ENTITY_RENDERER = typeDescOf(PAINTING_BLOCK_ENTITY_RENDERER_NAME);
	private static final String DUPLICATE_BLOCK_ENTITY_RENDERER = typeDescOf(DUPLICATE_BLOCK_ENTITY_RENDERER_NAME);

	@Override
	public Class<? extends NameProposer> getTarget() {
		return SimpleSubtypeFieldNameProposer.class;
	}

	@Override
	public String getTargetId() {
		return SimpleSubtypeFieldNameProposer.ID;
	}

	// make sure simple type name is set up for Entity
	@Test
	void truncatedSubtypeSupertype() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		// simple type names, not dynamic subtype names
		asserter.assertProposal("ENTITY", fieldOf(testClass, "a", ENTITY));
		asserter.assertProposal("entity", fieldOf(testClass, "k", ENTITY));
		// eatStatic
		asserter.assertProposal("entity", localOf(methodOf(testClass, "a", V, ENTITY), 0));
		// eat
		asserter.assertProposal("entity", localOf(methodOf(testClass, "b", V, ENTITY), 1));
	}

	@Test
	void testTruncatedSubtypeNames() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		final var context = new ValidationContext(null);

		asserter.remapper().putMapping(
				context, new ClassEntry(ENTITY_NON_SUFFIXED_NAME),
				new EntryMapping("EntityNonSuffixed")
		);

		// NON_SUFFIXED
		asserter.assertNotProposed(fieldOf(testClass, "d", ENTITY_NON_SUFFIXED));
		// nonSuffixed
		asserter.assertNotProposed(fieldOf(testClass, "n", ENTITY_NON_SUFFIXED));
		// eatNonSuffixedStatic
		asserter.assertNotProposed(localOf(methodOf(testClass, "a", OBJ, ENTITY_NON_SUFFIXED, OBJ), 0));
		// eatNonSuffixed
		asserter.assertNotProposed(localOf(methodOf(testClass, "b", OBJ, ENTITY_NON_SUFFIXED, OBJ), 1));

		asserter.remapper().putMapping(context, new ClassEntry(LIVING_ENTITY_NAME), new EntryMapping("LivingEntity"));
		// don't propose names for non-concrete classes
		// LIVING_ENTITY
		asserter.assertNotProposed(fieldOf(testClass, "b", LIVING_ENTITY));
		// livingEntity
		asserter.assertNotProposed(fieldOf(testClass, "l", LIVING_ENTITY));
		// eatLivingStatic
		asserter.assertNotProposed(localOf(methodOf(testClass, "a", Z, LIVING_ENTITY), 0));
		// eatLiving
		asserter.assertNotProposed(localOf(methodOf(testClass, "b", Z, LIVING_ENTITY), 1));

		asserter.remapper().putMapping(
				context, new ClassEntry(BIRD_ENTITY_NAME),
				new EntryMapping("com/example/BirdEntity")
		);

		asserter.assertDynamicProposal("BIRD", fieldOf(testClass, "c", BIRD_ENTITY));
		asserter.assertDynamicProposal("bird", fieldOf(testClass, "m", BIRD_ENTITY));
		// eatBirdStatic
		asserter.assertDynamicProposal("bird", localOf(methodOf(testClass, "a", I, I, BIRD_ENTITY), 1));
		// eatBird
		asserter.assertDynamicProposal("bird", localOf(methodOf(testClass, "b", I, I, BIRD_ENTITY), 2));
	}

	@Test
	void testNoInvalidSubtypeNames() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		final var context = new ValidationContext(null);

		asserter.remapper().putMapping(
				context, new ClassEntry(WHILE_ENTITY_NAME),
				new EntryMapping("com/example/WhileEntity")
		);

		// "WHILE" proposed despite its lowercase form being invalid
		asserter.assertDynamicProposal("WHILE", fieldOf(testClass, "j", WHILE_ENTITY));
		// "while" not proposed for notWhile field or param because it's a keyword
		asserter.assertNotProposed(fieldOf(testClass, "t", WHILE_ENTITY));
		asserter.assertNotProposed(methodOf(testClass, "a", V, WHILE_ENTITY));
	}

	// make sure simple type name is set up for BlockEntityRenderer
	@Test
	void transformedSubtypeSupertype() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		// simple type names, not dynamic subtype names
		asserter.assertProposal("BLOCK_ENTITY_RENDERER", fieldOf(testClass, "e", BLOCK_ENTITY_RENDERER));
		asserter.assertProposal("blockEntityRenderer", fieldOf(testClass, "o", BLOCK_ENTITY_RENDERER));
		// renderStatic
		asserter.assertProposal("blockEntityRenderer", localOf(methodOf(testClass, "a", V, BLOCK_ENTITY_RENDERER), 0));
		// render
		asserter.assertProposal("blockEntityRenderer", localOf(methodOf(testClass, "b", V, BLOCK_ENTITY_RENDERER), 1));
	}

	@Test
	void testTransformedSubtypeNames() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		final var context = new ValidationContext(null);

		asserter.remapper().putMapping(
				context, new ClassEntry(DISPLAY_BLOCK_ENTITY_RENDERER_NAME),
				new EntryMapping("DisplayBlockEntityRenderer")
		);

		asserter.assertDynamicProposal("DISPLAY_RENDERER", fieldOf(testClass, "f", DISPLAY_BLOCK_ENTITY_RENDERER));
		asserter.assertDynamicProposal("displayRenderer", fieldOf(testClass, "p", DISPLAY_BLOCK_ENTITY_RENDERER));
		// renderStatic
		asserter.assertDynamicProposal("displayRenderer", localOf(methodOf(testClass, "a", V, DISPLAY_BLOCK_ENTITY_RENDERER), 0));
		// render
		asserter.assertDynamicProposal("displayRenderer", localOf(methodOf(testClass, "b", V, DISPLAY_BLOCK_ENTITY_RENDERER), 1));

		asserter.remapper().putMapping(
				context, new ClassEntry(PAINTING_BLOCK_ENTITY_RENDERER_NAME),
				new EntryMapping("PaintingBlockEntityRenderer")
		);

		asserter.assertDynamicProposal("PAINTING_RENDERER", fieldOf(testClass, "g", PAINTING_BLOCK_ENTITY_RENDERER));
		asserter.assertDynamicProposal("paintingRenderer", fieldOf(testClass, "q", PAINTING_BLOCK_ENTITY_RENDERER));
		// renderStatic
		asserter.assertDynamicProposal("paintingRenderer", localOf(methodOf(testClass, "a", V, PAINTING_BLOCK_ENTITY_RENDERER), 0));
		// render
		asserter.assertDynamicProposal("paintingRenderer", localOf(methodOf(testClass, "b", V, PAINTING_BLOCK_ENTITY_RENDERER), 1));
	}

	@Test
	void testNoDuplicates() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		final var context = new ValidationContext(null);

		asserter.remapper().putMapping(
				context, new ClassEntry(DUPLICATE_BLOCK_ENTITY_RENDERER_NAME),
				new EntryMapping("DuplicateBlockEntityRenderer")
		);

		// DUPLICATE_1
		asserter.assertNotProposed(fieldOf(testClass, "h", DUPLICATE_BLOCK_ENTITY_RENDERER));
		// DUPLICATE_2
		asserter.assertNotProposed(fieldOf(testClass, "i", DUPLICATE_BLOCK_ENTITY_RENDERER));
		// duplicate1
		asserter.assertNotProposed(fieldOf(testClass, "r", DUPLICATE_BLOCK_ENTITY_RENDERER));
		// duplicate2
		asserter.assertNotProposed(fieldOf(testClass, "s", DUPLICATE_BLOCK_ENTITY_RENDERER));

		final MethodEntry renderStaticDuplicateMethod = methodOf(
				testClass, "a", V,
				DUPLICATE_BLOCK_ENTITY_RENDERER, DUPLICATE_BLOCK_ENTITY_RENDERER
		);
		asserter.assertNotProposed(localOf(renderStaticDuplicateMethod, 0));
		asserter.assertNotProposed(localOf(renderStaticDuplicateMethod, 1));

		final MethodEntry renderDuplicateMethod = methodOf(
				testClass, "b", V,
				DUPLICATE_BLOCK_ENTITY_RENDERER, DUPLICATE_BLOCK_ENTITY_RENDERER
		);
		asserter.assertNotProposed(localOf(renderDuplicateMethod, 1));
		asserter.assertNotProposed(localOf(renderDuplicateMethod, 2));
	}

	@Test
	void testSubSubtypeNames() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(SIMPLE_SUBTYPE_NAMES_TEST_NAME);

		final var context = new ValidationContext(null);
		asserter.remapper().putMapping(
				context, new ClassEntry(SPECIFIC_ENTITY_NAME),
				new EntryMapping("SpecificEntity")
		);

		asserter.remapper().putMapping(
				context, new ClassEntry(ENTITY_MARKER_NAME),
				new EntryMapping("EntityMarker")
		);

		asserter.remapper().putMapping(
				context, new ClassEntry(ENTITY_SPECIFIC_ENTITY_NAME),
				new EntryMapping("EntitySpecificEntity")
		);

		// method: eatSpecificEntity
		// named simple type name despite being a matching subtype of Entity
		// non-sub-types take priority
		asserter.assertProposal("specificEntity", localOf(methodOf(testClass, "a", V, SPECIFIC_ENTITY), 1));
		// method: eatMarker
		asserter.assertDynamicProposal("marker", localOf(methodOf(testClass, "a", V, ENTITY_MARKER), 1));
		// method: eatEntitySpecificEntity
		// named by SpecificEntity's subtype pattern, not Entity's, because SpecificEntity is a closer supertype
		asserter.assertDynamicProposal("specificEntity", localOf(methodOf(testClass, "a", V, ENTITY_SPECIFIC_ENTITY), 1));
	}
}
