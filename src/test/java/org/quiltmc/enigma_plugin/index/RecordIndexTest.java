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

package org.quiltmc.enigma_plugin.index;

import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.objectweb.asm.tree.ClassNode;
import org.tinylog.Logger;

import java.util.Map;

public class RecordIndexTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.info("Usage: RecordIndexTest <path>");
			System.exit(1);
		}

		ClassNode node = IndexTestUtil.getClassNode(args[0]);

		RecordIndex index = new RecordIndex();

		index.visitClassNode(node);

		dumpIndex(index);
	}

	private static void dumpIndex(RecordIndex index) {
		StringBuilder sb = new StringBuilder();
		sb.append("RecordIndex\n");

		Map<FieldEntry, String> fieldNames = index.getAllFieldNames();
		sb.append("\nFields:\n\n");
		sb.append("  ").append(fieldNames.size()).append(" fields\n");
		if (fieldNames.isEmpty()) {
			sb.append("  No fields\n");
		} else {
			fieldNames.forEach(((field, s) -> sb.append(field).append(" ").append(s).append("\n")));
		}

		Map<MethodEntry, String> methodNames = index.getAllMethodNames();
		sb.append("\nMethods:\n\n");
		sb.append("  ").append(methodNames.size()).append(" methods\n");
		if (methodNames.isEmpty()) {
			sb.append("  No methods\n");
		} else {
			methodNames.forEach(((method, s) -> sb.append(method).append(" ").append(s).append("\n")));
		}

		Logger.info(sb.toString());
	}
}
