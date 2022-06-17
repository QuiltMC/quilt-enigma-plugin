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

package org.quiltmc.enigmaplugin.util;

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
}
