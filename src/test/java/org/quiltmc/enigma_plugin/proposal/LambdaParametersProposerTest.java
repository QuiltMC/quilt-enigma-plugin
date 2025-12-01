package org.quiltmc.enigma_plugin.proposal;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.quiltmc.enigma_plugin.test.util.CommonDescriptors;
import org.quiltmc.enigma_plugin.test.util.ProposalAsserter;

import java.util.stream.Stream;

import static org.quiltmc.enigma_plugin.test.util.TestUtil.localOf;
import static org.quiltmc.enigma_plugin.test.util.TestUtil.methodOf;

public class LambdaParametersProposerTest implements ConventionalNameProposerTest, CommonDescriptors {
	private static final String LAMBDA_PARAMETERS_TEST_NAME = "a/a/a";

	@Override
	public Class<? extends NameProposer> getTarget() {
		return LambdaParametersProposer.class;
	}

	@Override
	public String getTargetId() {
		return LambdaParametersProposer.ID;
	}

	@Test
	public void testLambdaParameters() {
		final ProposalAsserter asserter = this.createAsserter();

		final var testClass = new ClassEntry(LAMBDA_PARAMETERS_TEST_NAME);

		final String targetName = "dinner";
		asserter.remapper().putMapping(
			new ValidationContext(null),
			localOf(methodOf(testClass, "eat", V, STR), 1),
			new EntryMapping(targetName)
		);

		Stream.of("a", "b", "c", "d", "e").forEach(minArgsMethodName ->
			asserter.assertDynamicProposal(
				targetName,
				localOf(methodOf(testClass, minArgsMethodName, V, STR), 0)
			)
		);

		asserter.assertDynamicProposal(targetName, localOf(methodOf(testClass, "a", V, Z, I, STR), 2));
	}
}
