/*
 * Copyright 2022 QuiltMC
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

package org.quiltmc.enigmaplugin.index;

import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

public class RecordIndexTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: RecordIndexTest <path>");
			System.exit(1);
		}

		ClassNode node = IndexTestUtil.getClassNode(args[0]);

		RecordIndex index = new RecordIndex();

		index.visitClassNode(node);

		dumpIndex(index);
	}

	private static void dumpIndex(RecordIndex index) {
		System.out.println("RecordIndex");

		Map<FieldEntry, String> fieldNames = index.getAllFieldNames();
		System.out.println("\nFields:\n");
		System.out.println("  " + fieldNames.size() + " fields");
		if (fieldNames.isEmpty()) {
			System.out.println("  No fields");
		} else {
			fieldNames.forEach(((field, s) -> System.out.println(field + " " + s)));
		}

		Map<MethodEntry, String> methodNames = index.getAllMethodNames();
		System.out.println("\nMethods:\n");
		System.out.println("  " + methodNames.size() + " methods");
		if (methodNames.isEmpty()) {
			System.out.println("  No methods");
		} else {
			methodNames.forEach(((method, s) -> System.out.println(method + " " + s)));
		}
	}
}
