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

		assertProposal("value", field(classEntry, "b", "I"));
		assertProposal("value", method(classEntry, "a", "()I"));

		assertProposal("scale", field(classEntry, "c", "D"));
		assertProposal("scale", method(classEntry, "b", "()D"));

		assertProposal("s", field(classEntry, "d", "Ljava/util/Optional;"));
		assertProposal("s", method(classEntry, "c", "()Ljava/util/Optional;"));

		var withCodecEntry = new ClassEntry("com/a/a$a");

		assertProposal("value", field(withCodecEntry, "b", "I"));
		assertProposal("value", method(withCodecEntry, "a", "()I"));
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

		assertProposal("value", field(classEntry, "c", "I"));
		assertProposal("getValue", method(classEntry, "a", "()I"));

		assertProposal("scale", field(classEntry, "d", "D"));
		assertProposal("getScale", method(classEntry, "b", "()D"));

		assertProposal("factor", field(classEntry, "e", "Ljava/util/Optional;"));
		assertProposal("getFactor", method(classEntry, "c", "()Ljava/util/Optional;"));

		assertProposal("seed", field(classEntry, "b", "J"));
	}

	@Test
	public void testConstructorParameterNames() {
		var classEntry = new ClassEntry("com/a/a");
		var constructor = method(classEntry, "<init>", "(IDLjava/util/Optional;J)V");

		assertDynamicProposal("value", localVar(constructor, 1));
		assertDynamicProposal("scale", localVar(constructor, 2));
		assertDynamicProposal("factor", localVar(constructor, 4));
		assertDynamicProposal("seed", localVar(constructor, 5));
	}

	@Test
	public void testGetterSetterNames() {
		var classEntry = new ClassEntry("com/a/c");

		var vc = new ValidationContext(null);
		remapper.putMapping(vc, field(classEntry, "a", "I"), new EntryMapping("silliness"));
		remapper.putMapping(vc, field(classEntry, "b", "Ljava/lang/String;"), new EntryMapping("name"));

		MethodEntry method;
		assertDynamicProposal("getSilliness", method(classEntry, "a", "()I"));
		assertDynamicProposal("setSilliness", (method = method(classEntry, "a", "(I)V")));
		assertDynamicProposal("silliness", localVar(method, 1));

		assertDynamicProposal("getName", method(classEntry, "b", "()Ljava/lang/String;"));
		assertDynamicProposal("setName", (method = method(classEntry, "b", "(Ljava/lang/String;)V")));
		assertDynamicProposal("name", localVar(method, 1));
	}

	@Test
	public void testSimpleTypeSingleNames() {
		var classEntry = new ClassEntry("com/a/e");
		var fieldsClassEntry = new ClassEntry(classEntry, "a");

		var owner = new ClassEntry(fieldsClassEntry, "a");
		assertNotProposed(field(owner, "a", "Lcom/a/b/g;"));
		assertNotProposed(field(owner, "b", "Lcom/a/b/g;"));

		owner = new ClassEntry(fieldsClassEntry, "b");
		assertProposal("POS", field(owner, "a", "Lcom/a/b/b;"));
		assertProposal("position", field(owner, "b", "Lcom/a/b/c;"));
		assertProposal("randomPosition", field(owner, "c", "Lcom/a/b/d;"));
		assertProposal("STATIC_STATE_A", field(owner, "d", "Lcom/a/b/e;"));
		assertProposal("STATIC_STATE_B", field(owner, "e", "Lcom/a/b/f;"));
		assertProposal("VALUE_A", field(owner, "f", "Lcom/a/b/g;"));
		assertProposal("VALUE_B", field(owner, "g", "Lcom/a/b/h;"));
		assertProposal("valueC", field(owner, "h", "Lcom/a/b/i;"));

		owner = new ClassEntry(fieldsClassEntry, "c");
		assertProposal("CONFIG", field(owner, "a", "Lcom/a/b/a;"));
		assertProposal("STATIC_STATE", field(owner, "b", "Lcom/a/b/e;"));
		assertProposal("value", field(owner, "c", "Lcom/a/b/i;"));

		owner = new ClassEntry(classEntry, "b");
		var parent = method(owner, "a", "(Lcom/a/b/a;)V");
		assertProposal("config", localVar(parent, 0));

		parent = method(owner, "a", "(Lcom/a/b/b;)V");
		assertProposal("pos", localVar(parent, 1));

		parent = method(owner, "a", "(Lcom/a/b/b;Lcom/a/b/c;)V");
		assertProposal("pos", localVar(parent, 1));
		assertProposal("position", localVar(parent, 2));

		parent = method(owner, "a", "(Lcom/a/b/b;Lcom/a/b/d;)V");
		assertProposal("pos", localVar(parent, 1));
		assertProposal("position", localVar(parent, 2));

		parent = method(owner, "a", "(Lcom/a/b/b;Lcom/a/b/c;Lcom/a/b/d;)V");
		assertProposal("pos", localVar(parent, 1));
		assertProposal("position", localVar(parent, 2));
		assertProposal("randomPosition", localVar(parent, 3));

		parent = method(owner, "a", "(Lcom/a/b/e;)V");
		assertProposal("state", localVar(parent, 1));

		parent = method(owner, "a", "(Lcom/a/b/e;Lcom/a/b/f;)V");
		assertProposal("stateA", localVar(parent, 0));
		assertProposal("stateB", localVar(parent, 1));

		parent = method(owner, "a", "(Lcom/a/b/g;Lcom/a/b/h;Lcom/a/b/i;)V");
		assertProposal("valueA", localVar(parent, 0));
		assertProposal("valueB", localVar(parent, 1));
		assertProposal("valueC", localVar(parent, 2));

		parent = method(owner, "a", "(Lcom/a/b/g;Lcom/a/b/g;)V");
		assertNotProposed(localVar(parent, 0));
		assertNotProposed(localVar(parent, 1));
	}

	@Test
	public void testDelegateParameterNames() {
		var classEntry = new ClassEntry("com/a/b");

		assertDynamicProposal("seed", localVar(method(classEntry, "a", "(J)I"), 1));
		assertDynamicProposal("seed", localVar(method(classEntry, "a", "(I)V"), 1));
		assertDynamicProposal("seed", localVar(method(classEntry, "b", "(J)V"), 1));

		assertNotProposed(localVar(method(classEntry, "b", "(I)V"), 1));

		// Multiple parameters passed to the same method call within a single method def shouldn't be named
		assertNotProposed(localVar(method(classEntry, "a", "(II)I"), 0));
		assertNotProposed(localVar(method(classEntry, "a", "(II)I"), 1));

		assertNotProposed(localVar(method(classEntry, "a", "(IIII)I"), 0));
		assertNotProposed(localVar(method(classEntry, "a", "(IIII)I"), 1));
		assertNotProposed(localVar(method(classEntry, "a", "(IIII)I"), 2));
		assertNotProposed(localVar(method(classEntry, "a", "(IIII)I"), 3));

		classEntry = new ClassEntry(classEntry, "a");

		assertDynamicProposal("val", localVar(method(classEntry, "<init>", "(I)V"), 1));
		assertDynamicProposal("val", localVar(method(classEntry, "<init>", "(IJ)V"), 1));
		assertDynamicProposal("val", localVar(method(classEntry, "<init>", "(IJI)V"), 1));
		assertDynamicProposal("val", localVar(method(classEntry, "a", "(I)Lcom/a/b$a;"), 0));
		assertDynamicProposal("val", localVar(method(classEntry, "a", "(IJ)Lcom/a/b$a;"), 0));

		assertDynamicProposal("j", localVar(method(classEntry, "<init>", "(IJ)V"), 2));
		assertDynamicProposal("j", localVar(method(classEntry, "<init>", "(IJI)V"), 2));
		assertDynamicProposal("j", localVar(method(classEntry, "a", "(IJ)Lcom/a/b$a;"), 1));

		assertDynamicProposal("index", localVar(method(classEntry, "<init>", "(IJI)V"), 4));

		// Test dynamic renames
		remapper.putMapping(new ValidationContext(null), localVar(method(classEntry, "<init>", "(IJI)V"), 2), new EntryMapping("silly"));
		assertDynamicProposal("silly", localVar(method(classEntry, "<init>", "(IJ)V"), 2));
		assertDynamicProposal("silly", localVar(method(classEntry, "a", "(IJ)Lcom/a/b$a;"), 1));

		remapper.putMapping(new ValidationContext(null), localVar(method(classEntry, "<init>", "(IJI)V"), 2), EntryMapping.DEFAULT);
		assertDynamicProposal("j", localVar(method(classEntry, "<init>", "(IJ)V"), 2));
		assertDynamicProposal("j", localVar(method(classEntry, "<init>", "(IJI)V"), 2));
		assertDynamicProposal("j", localVar(method(classEntry, "a", "(IJ)Lcom/a/b$a;"), 1));

		// Ensure names are also loaded from external classes
		classEntry = new ClassEntry("com/a/c");

		assertDynamicProposal("bound", localVar(method(classEntry, "b", "(I)V"), 1));

		// Names shouldn't be proposed for synthetic parameters (like Enum constructor name and ordinal)
		classEntry = new ClassEntry("com/a/a/e");
		var method = method(classEntry, "<init>", "(Ljava/lang/String;IZ)V");

		assertNotProposed(localVar(method, 1));
		assertNotProposed(localVar(method, 2));
	}

	private static FieldEntry field(ClassEntry parent, String name, String desc) {
		return new FieldEntry(parent, name, new TypeDescriptor(desc));
	}

	private static MethodEntry method(ClassEntry parent, String name, String desc) {
		return new MethodEntry(parent, name, new MethodDescriptor(desc));
	}

	private static LocalVariableEntry localVar(MethodEntry parent, int index) {
		return new LocalVariableEntry(parent, index);
	}
}
