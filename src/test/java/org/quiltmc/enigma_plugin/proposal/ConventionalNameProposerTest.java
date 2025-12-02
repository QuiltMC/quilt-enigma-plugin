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
import org.quiltmc.enigma_plugin.util.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A name proposer test whose test input source set is named conventionally.<br>
 * That is, it has the name of the {@linkplain #getTarget() target class} converted to {@code lowerCamelCase}.
 */
public interface ConventionalNameProposerTest {
	/**
	 * The name of custom enigma profile files.
	 */
	String PROFILE_JSON = "profile.json";

	/**
	 * @return the class of the name proposer being tested
	 */
	Class<? extends NameProposer> getTarget();

	/**
	 * @return the id of the name proposer being tested
	 */
	String getTargetId();

	/**
	 * @return the obfuscated input jar for the proposer being tested
	 */
	default Path getObfJar() {
		return TestUtil.obfJarPathOf(this.getUnCapitalizedTarget());
	}

	/**
	 * @return this test's enigma profile; defaults to {@link TestUtil#DEFAULT_ENIGMA_PROFILE} if a custom
	 * {@value #PROFILE_JSON} isn't found in test input resources
	 */
	default Path getEnigmaProfile() {
		final Path customProfile = TestUtil.BUILD_RESOURCES.resolve(this.getUnCapitalizedTarget() + "/" + PROFILE_JSON);
		return Files.isRegularFile(customProfile)
				? customProfile
				: TestUtil.DEFAULT_ENIGMA_PROFILE;
	}

	/**
	 * @return a new {@link ProposalAsserter} whose {@linkplain ProposalAsserter#remapper() remapper} maps this test's
	 * {@linkplain #getObfJar() obf jar} using its {@linkplain #getEnigmaProfile() enigma profile} and whose
	 * {@linkplain ProposalAsserter#proposerId() proposer id} is this test's {@linkplain #getTargetId() target id}
	 */
	default ProposalAsserter createAsserter() {
		return new ProposalAsserter(
				TestUtil.setupEnigma(this.getObfJar(), this.getEnigmaProfile()),
				this.getTargetId()
		);
	}

	private String getUnCapitalizedTarget() {
		return StringUtil.unCapitalize(this.getTarget().getSimpleName());
	}
}
