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

package org.quiltmc.enigma_plugin.index.simple_subtype_single;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonToken;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleSubtypeFieldNamesRegistry {
	private final Path path;
	/**
	 * Using a {@link LinkedHashMap} to ensure we keep the read order.
	 */
	private final Map<String, Entry> entries = new LinkedHashMap<>();

	public SimpleSubtypeFieldNamesRegistry(Path path) {
		this.path = path;
	}

	public @Nullable Entry getEntry(String type) {
		return this.entries.get(type);
	}

	public void read() {
		try (var reader = JsonReader.json5(this.path)) {
			if (reader.peek() != JsonToken.BEGIN_OBJECT) {
				return;
			}

			reader.beginObject();

			while (reader.hasNext()) {
				String superType = reader.nextName();

				if (superType.equals("$schema")) {
					reader.skipValue();
					continue;
				}

				if (this.entries.containsKey(superType)) {
					throw new IllegalArgumentException("Duplicate supertype " + superType);
				}

				switch (reader.peek()) {
					case STRING -> {
						String suffix = reader.nextString();
						this.entries.put(superType, new Entry(superType, suffix));
					}
					default -> reader.skipValue();
				}
			}

			reader.endObject();
		} catch (IOException e) {
			Logger.error(e, "Failed to read simple type field names registry.");
		}
	}

	public record Entry(String type, String suffix) { }
}
