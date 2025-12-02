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

package org.quiltmc.enigma_plugin.util;

import org.jspecify.annotations.Nullable;

public class CasingUtil {
	public static String toCamelCase(String name) {
		// Make sure the first letter is lower case
		name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		while (name.contains("_")) {
			name = name.replaceFirst("_[a-z]", String.valueOf(Character.toUpperCase(name.charAt(name.indexOf('_') + 1))));
		}

		return name;
	}

	public static String toScreamingSnakeCase(String name) {
		var builder = new StringBuilder();

		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);

			if (c == '_') {
				builder.append(c);
			} else {
				if (Character.isUpperCase(c) && i != 0) {
					builder.append('_');
				}

				builder.append(Character.toUpperCase(c));
			}
		}

		return builder.toString();
	}

	/**
	 * {@return whether a character is alphanumeric}
	 */
	public static boolean isCharacterUsable(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
	}

	/**
	 * Convert a string to UPPER_SNAKE_CASE, removing or replacing non-alphanumeric characters with underscores.
	 *
	 * @return the string in UPPER_SNAKE_CASE, or null if it didn't contain any letter
	 */
	@Nullable
	public static String toSafeScreamingSnakeCase(String name) {
		StringBuilder usableName = new StringBuilder();
		boolean hasAlphabetic = false;
		boolean prevUsable = false;

		for (int j = 0; j < name.length(); j++) {
			char c = name.charAt(j);

			if (isCharacterUsable(c)) {
				if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
					hasAlphabetic = true;
				}

				// Add an underscore before if the current character follows another letter/number and is the start of a camel cased word
				if (j > 0 && Character.isUpperCase(c) && j < name.length() - 1 && Character.isLowerCase(name.charAt(j + 1)) && prevUsable) {
					usableName.append('_');
				}

				usableName.append(Character.toUpperCase(c));
				prevUsable = true;
			} else if (j > 0 && j < name.length() - 1 && prevUsable) {
				// Replace unusable characters with underscores if they aren't at the start or end, and are following another usable character
				usableName.append('_');
				prevUsable = false;
			}
		}

		if (!hasAlphabetic) {
			return null;
		}

		return usableName.toString();
	}
}
