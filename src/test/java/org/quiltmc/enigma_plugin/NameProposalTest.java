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

package org.quiltmc.enigma_plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
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
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.io.IOException;
import java.nio.file.Path;

public class NameProposalTest {
	private static final Path JAR = Path.of("build/obf/obf.jar");
	private static final Path PROFILE = Path.of("build/resources/testInputs/profile.json");
	private static EntryRemapper remapper;

	@BeforeAll
	public static void setupEnigma() throws IOException {
		var profile = EnigmaProfile.read(PROFILE);
		var enigma = Enigma.builder().setProfile(profile).build();

		var project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		remapper = project.getRemapper();

		// Manually fire dynamic proposals
		remapper.insertDynamicallyProposedMappings(null, null, null);
	}

	public static void assertProposal(String name, Entry<?> entry) {
		var mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		Assertions.assertEquals(name, mapping.targetName());
		Assertions.assertEquals(TokenType.JAR_PROPOSED, mapping.tokenType());
	}

	public static void assertDynamicProposal(String name, Entry<?> entry) {
		var mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		Assertions.assertEquals(name, mapping.targetName());
		Assertions.assertEquals(TokenType.DYNAMIC_PROPOSED, mapping.tokenType());
	}

	public static void assertNotProposed(Entry<?> entry) {
		var mapping = remapper.getMapping(entry);
		Assertions.assertNotNull(mapping);
		Assertions.assertEquals(EntryMapping.DEFAULT, mapping);
	}

	@Test
	public void testRecordNames() {
		var classEntry = new ClassEntry("com/a/d");

		assertProposal("value", new FieldEntry(classEntry, "b", new TypeDescriptor("I")));
		assertProposal("value", new MethodEntry(classEntry, "a", new MethodDescriptor("()I")));

		assertProposal("scale", new FieldEntry(classEntry, "c", new TypeDescriptor("D")));
		assertProposal("scale", new MethodEntry(classEntry, "b", new MethodDescriptor("()D")));

		assertProposal("s", new FieldEntry(classEntry, "d", new TypeDescriptor("Ljava/util/Optional;")));
		assertProposal("s", new MethodEntry(classEntry, "c", new MethodDescriptor("()Ljava/util/Optional;")));

		var withCodecEntry = new ClassEntry("com/a/a$a");

		assertProposal("value", new FieldEntry(withCodecEntry, "b", new TypeDescriptor("I")));
		assertProposal("value", new MethodEntry(withCodecEntry, "a", new MethodDescriptor("()I")));
	}

	@Test
	public void testConstantFieldNames() {
		var classEntry = new ClassEntry("com/a/a/c");
		var desc = new TypeDescriptor("Lcom/a/a/f;");

		assertProposal("FOO", new FieldEntry(classEntry, "a", desc));
		assertProposal("BAR_FOO", new FieldEntry(classEntry, "b", desc));
		assertProposal("BAZ_FOO", new FieldEntry(classEntry, "c", desc));
		assertProposal("LOREM_IPSUM_BAZ", new FieldEntry(classEntry, "d", desc));
		assertProposal("AN_ID", new FieldEntry(classEntry, "e", desc));
		assertProposal("ANOTHER_ID_FOO", new FieldEntry(classEntry, "f", desc));
		assertProposal("ONE", new FieldEntry(classEntry, "g", desc));
		assertProposal("TWO", new FieldEntry(classEntry, "h", desc));
		assertProposal("THREE", new FieldEntry(classEntry, "i", desc));

		var class2Entry = new ClassEntry("com/a/a/a");

		assertProposal("FOO", new FieldEntry(class2Entry, "a", desc));
		assertProposal("BAR_FOO", new FieldEntry(class2Entry, "b", desc));
		assertProposal("AN_ID", new FieldEntry(class2Entry, "c", desc));
		assertProposal("NORTH", new FieldEntry(class2Entry, "d", desc));
		assertProposal("EAST", new FieldEntry(class2Entry, "e", desc));

		var class3Entry = new ClassEntry("com/a/a/b");

		assertProposal("FOO", new FieldEntry(class3Entry, "a", desc));
		assertProposal("BAR_FOO", new FieldEntry(class3Entry, "b", desc));

		var enumEntry = new ClassEntry("com/a/a/e");
		var enumDesc = new TypeDescriptor("Lcom/a/a/e;");

		assertProposal("NORTH", new FieldEntry(enumEntry, "a", enumDesc));
		assertProposal("EAST", new FieldEntry(enumEntry, "b", enumDesc));
		assertProposal("WEST", new FieldEntry(enumEntry, "c", enumDesc));
		assertProposal("SOUTH", new FieldEntry(enumEntry, "d", enumDesc));

		var enum2Entry = new ClassEntry("com/a/a/d");
		var enum2Desc = new TypeDescriptor("Lcom/a/a/d;");

		assertProposal("CONSTANT", new FieldEntry(enum2Entry, "a", enum2Desc));
		assertProposal("INT", new FieldEntry(enum2Entry, "b", enum2Desc));
		assertProposal("DOUBLE", new FieldEntry(enum2Entry, "c", enum2Desc));
		assertProposal("EXPONENTIAL", new FieldEntry(enum2Entry, "d", enum2Desc));
		assertProposal("GAUSSIAN", new FieldEntry(enum2Entry, "e", enum2Desc));
	}

	@Test
	public void testCodecNames() {
		var classEntry = new ClassEntry("com/a/a");

		assertProposal("value", new FieldEntry(classEntry, "c", new TypeDescriptor("I")));
		assertProposal("getValue", new MethodEntry(classEntry, "a", new MethodDescriptor("()I")));

		assertProposal("scale", new FieldEntry(classEntry, "d", new TypeDescriptor("D")));
		assertProposal("getScale", new MethodEntry(classEntry, "b", new MethodDescriptor("()D")));

		assertProposal("factor", new FieldEntry(classEntry, "e", new TypeDescriptor("Ljava/util/Optional;")));
		assertProposal("getFactor", new MethodEntry(classEntry, "c", new MethodDescriptor("()Ljava/util/Optional;")));

		assertProposal("seed", new FieldEntry(classEntry, "b", new TypeDescriptor("J")));
	}

	@Test
	public void testConstructorParameterNames() {
		var classEntry = new ClassEntry("com/a/a");
		var constructor = new MethodEntry(classEntry, "<init>", new MethodDescriptor("(IDLjava/util/Optional;J)V"));

		assertDynamicProposal("value", new LocalVariableEntry(constructor, 1));
		assertDynamicProposal("scale", new LocalVariableEntry(constructor, 2));
		assertDynamicProposal("factor", new LocalVariableEntry(constructor, 4));
		assertDynamicProposal("seed", new LocalVariableEntry(constructor, 5));
	}

	@Test
	public void testGetterSetterNames() {
		var classEntry = new ClassEntry("com/a/c");

		var vc = new ValidationContext(null);
		remapper.putMapping(vc, new FieldEntry(classEntry, "a", new TypeDescriptor("I")), new EntryMapping("silliness"));
		remapper.putMapping(vc, new FieldEntry(classEntry, "b", new TypeDescriptor("Ljava/lang/String;")), new EntryMapping("name"));

		MethodEntry method;
		assertDynamicProposal("getSilliness", new MethodEntry(classEntry, "a", new MethodDescriptor("()I")));
		assertDynamicProposal("setSilliness", (method = new MethodEntry(classEntry, "a", new MethodDescriptor("(I)V"))));
		assertDynamicProposal("silliness", new LocalVariableEntry(method, 1));

		assertDynamicProposal("getName", new MethodEntry(classEntry, "b", new MethodDescriptor("()Ljava/lang/String;")));
		assertDynamicProposal("setName", (method = new MethodEntry(classEntry, "b", new MethodDescriptor("(Ljava/lang/String;)V"))));
		assertDynamicProposal("name", new LocalVariableEntry(method, 1));
	}

	@Test
	public void testSimpleTypeSingleNames() {
		var classEntry = new ClassEntry("com/a/e");
		var fieldsClassEntry = new ClassEntry(classEntry, "a");

		var owner = new ClassEntry(fieldsClassEntry, "a");
		assertNotProposed(new FieldEntry(owner, "a", new TypeDescriptor("Lcom/a/b/g;")));
		assertNotProposed(new FieldEntry(owner, "b", new TypeDescriptor("Lcom/a/b/g;")));

		owner = new ClassEntry(fieldsClassEntry, "b");
		assertProposal("POS", new FieldEntry(owner, "a", new TypeDescriptor("Lcom/a/b/b;")));
		assertProposal("position", new FieldEntry(owner, "b", new TypeDescriptor("Lcom/a/b/c;")));
		assertProposal("randomPosition", new FieldEntry(owner, "c", new TypeDescriptor("Lcom/a/b/d;")));
		assertProposal("STATIC_STATE_A", new FieldEntry(owner, "d", new TypeDescriptor("Lcom/a/b/e;")));
		assertProposal("STATIC_STATE_B", new FieldEntry(owner, "e", new TypeDescriptor("Lcom/a/b/f;")));
		assertProposal("VALUE_A", new FieldEntry(owner, "f", new TypeDescriptor("Lcom/a/b/g;")));
		assertProposal("VALUE_B", new FieldEntry(owner, "g", new TypeDescriptor("Lcom/a/b/h;")));
		assertProposal("valueC", new FieldEntry(owner, "h", new TypeDescriptor("Lcom/a/b/i;")));

		owner = new ClassEntry(fieldsClassEntry, "c");
		assertProposal("CONFIG", new FieldEntry(owner, "a", new TypeDescriptor("Lcom/a/b/a;")));
		assertProposal("STATIC_STATE", new FieldEntry(owner, "b", new TypeDescriptor("Lcom/a/b/e;")));
		assertProposal("value", new FieldEntry(owner, "c", new TypeDescriptor("Lcom/a/b/i;")));

		owner = new ClassEntry(classEntry, "b");
		var parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/a;)V"));
		assertProposal("config", new LocalVariableEntry(parent, 0));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/b;)V"));
		assertProposal("pos", new LocalVariableEntry(parent, 1));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/b;Lcom/a/b/c;)V"));
		assertProposal("pos", new LocalVariableEntry(parent, 1));
		assertProposal("position", new LocalVariableEntry(parent, 2));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/b;Lcom/a/b/d;)V"));
		assertProposal("pos", new LocalVariableEntry(parent, 1));
		assertProposal("position", new LocalVariableEntry(parent, 2));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/b;Lcom/a/b/c;Lcom/a/b/d;)V"));
		assertProposal("pos", new LocalVariableEntry(parent, 1));
		assertProposal("position", new LocalVariableEntry(parent, 2));
		assertProposal("randomPosition", new LocalVariableEntry(parent, 3));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/e;)V"));
		assertProposal("state", new LocalVariableEntry(parent, 1));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/e;Lcom/a/b/f;)V"));
		assertProposal("stateA", new LocalVariableEntry(parent, 0));
		assertProposal("stateB", new LocalVariableEntry(parent, 1));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/g;Lcom/a/b/h;Lcom/a/b/i;)V"));
		assertProposal("valueA", new LocalVariableEntry(parent, 0));
		assertProposal("valueB", new LocalVariableEntry(parent, 1));
		assertProposal("valueC", new LocalVariableEntry(parent, 2));

		parent = new MethodEntry(owner, "a", new MethodDescriptor("(Lcom/a/b/g;Lcom/a/b/g;)V"));
		assertNotProposed(new LocalVariableEntry(parent, 0));
		assertNotProposed(new LocalVariableEntry(parent, 1));
	}
}
