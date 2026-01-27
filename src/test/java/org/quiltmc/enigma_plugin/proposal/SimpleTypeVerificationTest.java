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
import org.quiltmc.enigma_plugin.test.util.TestUtil;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.quiltmc.enigma_plugin.proposal.ConventionalNameProposerTest.PROFILE_JSON;

public class SimpleTypeVerificationTest {
	private static final String SOURCE_SET_NAME = "simpleTypeVerification";
	private static final Path OBF_JAR = TestUtil.obfJarPathOf(SOURCE_SET_NAME);
	private static final Path PROFILE = TestUtil.BUILD_RESOURCES.resolve(SOURCE_SET_NAME + "/" + PROFILE_JSON);

	@Test
	void test() {
		final IllegalStateException thrown = assertThrowsExactly(
				IllegalStateException.class,
				() -> TestUtil.setupEnigma(OBF_JAR, PROFILE)
		);

		assertThat(thrown.getMessage(), is("The following simple type field name type is missing: not/present"));
	}
}
