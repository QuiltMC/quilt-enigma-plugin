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

import org.quiltmc.enigma_plugin.test.util.ProposalAsserter;
import org.quiltmc.enigma_plugin.test.util.TestUtil;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A name proposer test whose test input source set is named conventionally.<br>
 * That is, it has the name of the {@linkplain #getTarget() target class} converted to {@code lowerCamelCase}.
 *
 * @see TestUtil#obfJarPathOf(String)
 */
public interface ConventionalNameProposerTest {
	Class<? extends NameProposer> getTarget();

	String getTargetId();

	default Path getObfJar() {
		return TestUtil.obfJarPathOf(this.getUnCapitalizedTarget());
	}

	default Path getEnigmaProfile() {
		final Path customProfile = TestUtil.BUILD_RESOURCES.resolve(this.getUnCapitalizedTarget() + "/profile.json");
		return Files.isRegularFile(customProfile)
				? customProfile
				: TestUtil.DEFAULT_ENIGMA_PROFILE;
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

	private String getUnCapitalizedTarget() {
		return TestUtil.unCapitalize(this.getTarget().getSimpleName());
	}
}
