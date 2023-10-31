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

import org.objectweb.asm.tree.ClassNode;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.List;

public class CodecIndexTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.info("Usage: CodecIndexTest <path> [<customCodec>...]");
			System.exit(1);
		}

		ClassNode node = IndexTestUtil.getClassNode(args[0]);

		CodecIndex index = new CodecIndex();
		if (args.length > 1) {
			List<String> customCodecs = Arrays.asList(args).subList(1, args.length);
			index.addCustomCodecs(customCodecs);
		}

		index.visitClassNode(node);

		dumpIndex(index);
	}

	private static void dumpIndex(CodecIndex index) {
		StringBuilder sb = new StringBuilder();
		sb.append("CodecIndex\n");
		if (index.hasCustomCodecs()) {
			sb.append("  Custom codecs:\n");
			for (String codec : index.getCustomCodecs()) {
				sb.append("    ").append(codec).append("\n");
			}
		}

		sb.append("\nFields:\n\n");
		if (index.getFieldNames().isEmpty()) {
			sb.append("  No fields\n");
		} else {
			index.getFieldNames().forEach(((field, s) -> sb.append(field).append(" ").append(s).append("\n")));
		}

		sb.append("\nMethods:\n\n");
		if (index.getMethodNames().isEmpty()) {
			sb.append("  No methods\n");
		} else {
			index.getMethodNames().forEach(((method, s) -> sb.append(method).append(" ").append(s).append("\n")));
		}

		Logger.info(sb.toString());
	}
}
