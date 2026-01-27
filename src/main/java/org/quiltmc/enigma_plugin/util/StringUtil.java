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

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class StringUtil {
	private StringUtil() {
		throw new UnsupportedOperationException();
	}

	private static final Set<String> ILLEGAL_IDENTIFIERS = Set.of(
			"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
			"continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally",
			"float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
			"native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
			"strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try",
			"void", "volatile", "while", "_"
	);

	@Nullable
	public static String getObjectTypeOrNull(String descriptor) {
		if (descriptor.length() <= 3 || descriptor.charAt(0) != 'L') {
			return null;
		} else {
			return descriptor.substring(1, descriptor.length() - 1);
		}
	}

	public static boolean isValidJavaIdentifier(String id) {
		if (id.isEmpty() || ILLEGAL_IDENTIFIERS.contains(id) || !Character.isJavaIdentifierStart(id.charAt(0))) {
			return false;
		}

		for (int i = 1; i < id.length(); i++) {
			if (!Character.isJavaIdentifierPart(id.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Un-capitalized the first character of the passed {@code string}.
	 */
	public static String unCapitalize(String string) {
		if (string.isEmpty()) {
			return string;
		}

		final char first = string.charAt(0);
		final char firstLower = Character.toLowerCase(first);

		if (first == firstLower) {
			return string;
		} else {
			final var builder = new StringBuilder();
			builder.append(firstLower);

			for (int i = 1; i < string.length(); i++) {
				builder.append(string.charAt(i));
			}

			return builder.toString();
		}
	}
}
