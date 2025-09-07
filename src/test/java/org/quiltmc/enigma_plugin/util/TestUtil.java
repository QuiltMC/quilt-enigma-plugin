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

package org.quiltmc.enigma_plugin.util;

import org.junit.jupiter.api.Assertions;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;

import java.io.IOException;
import java.nio.file.Path;

public final class TestUtil {
	private TestUtil() {
		throw new UnsupportedOperationException();
	}

	private static final Path DEFAULT_PROFILE = Path.of("build/resources/testInputs/profile.json");

	public static Path obfJarPathOf(String name) {
		return Path.of("").toAbsolutePath().resolve("build/test-obf/%s.jar".formatted(name));
	}

	/**
	 * Sets up Enigma with the {@link #DEFAULT_PROFILE}.
	 */
	public static EntryRemapper setupEnigma(Path jar) {
		return setupEnigma(jar, DEFAULT_PROFILE);
	}

	public static EntryRemapper setupEnigma(Path jar, Path profile) {
		final EnigmaProfile enigmaProfile;
		try {
			enigmaProfile = EnigmaProfile.read(profile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		var enigma = Enigma.builder().setProfile(enigmaProfile).build();

		final EnigmaProject project;
		try {
			project = enigma.openJar(jar, new ClasspathClassProvider(), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		EntryRemapper remapper = project.getRemapper();

		// Manually fire dynamic proposals
		remapper.insertDynamicallyProposedMappings(null, null, null);

		return remapper;
	}

	public static void assertProposal(String name, Entry<?> entry, EntryRemapper remapper) {
		EntryMapping mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		Assertions.assertEquals(name, mapping.targetName());
		Assertions.assertEquals(TokenType.JAR_PROPOSED, mapping.tokenType());
	}

	public static void assertDynamicProposal(String name, Entry<?> entry, EntryRemapper remapper) {
		EntryMapping mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		Assertions.assertEquals(name, mapping.targetName());
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, mapping.tokenType());
	}

	public static void assertNotProposed(Entry<?> entry, EntryRemapper remapper) {
		EntryMapping mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		Assertions.assertEquals(EntryMapping.OBFUSCATED, mapping);
	}

	public static void assertNotProposedBy(Entry<?> entry, String unexpectedSourceProposerId, EntryRemapper remapper) {
		EntryMapping mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		if (mapping.sourcePluginId() != null) {
			Assertions.assertNotEquals(QuiltEnigmaPlugin.NAME_PROPOSAL_SERVICE_ID + "/" + unexpectedSourceProposerId, mapping.sourcePluginId());
		} else {
			Assertions.assertEquals(EntryMapping.OBFUSCATED, mapping);
		}
	}

	public static FieldEntry field(ClassEntry parent, String name, String desc) {
		return new FieldEntry(parent, name, new TypeDescriptor(desc));
	}

	public static MethodEntry methodOf(ClassEntry parent, String name, String returnDesc, String... paramDescriptors) {
		final String desc = "(" + String.join("", paramDescriptors) + ")" + returnDesc;
		return new MethodEntry(parent, name, new MethodDescriptor(desc));
	}

	public static LocalVariableEntry localVar(MethodEntry parent, int index) {
		return new LocalVariableEntry(parent, index);
	}

	public static String typeDescOf(String typeName) {
		return "L" + typeName + ";";
	}

	public static String javaLangDescOf(String simpleName) {
		return typeDescOf("java/lang/" + simpleName);
	}
}
