package org.quiltmc.enigma_plugin.proposal;

import org.quiltmc.enigma_plugin.test.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.test.util.TestUtil;

import java.nio.file.Path;

/**
 * A name proposer test whose obfuscated test input jar is named conventionally.<br>
 * That is, it's name is prefixed with the name of the {@linkplain #getTarget()} class converted to {@code lowerCamelCase}.
 *
 * @see TestUtil#obfJarPathOf(String)
 */
public interface ConventionalNameProposerTest {
	Class<? extends NameProposer> getTarget();

	String getTargetId();

	default Path getObfJar() {
		return TestUtil.obfJarPathOf(TestUtil.unCapitalize(this.getTarget().getSimpleName()));
	}

	default Path getEnigmaProfile() {
		return TestUtil.DEFAULT_ENIGMA_PROFILE;
	}

	/**
	 * @return a new {@link ProposalAsserter} whose {@link ProposalAsserter#remapper() remapper} maps this test's
	 * {@linkplain #getObfJar() obf jar} and whose {@link ProposalAsserter#proposerId() proposerId} is this test's
	 * {@linkplain #getTargetId() target id}
	 */
	default ProposalAsserter createAsserter() {
		return new ProposalAsserter(
				TestUtil.setupEnigma(this.getObfJar(), this.getEnigmaProfile()),
				this.getTargetId()
		);
	}
}
