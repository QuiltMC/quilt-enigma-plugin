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

package org.quiltmc.enigma_plugin.index.simple_type_single;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma_plugin.util.CasingUtil;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonToken;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleTypeFieldNamesRegistry {
	private final Path path;
	/**
	 * Using a {@link LinkedHashMap} to ensure we keep the read order.
	 */
	private final Map<String, Entry> entries = new LinkedHashMap<>();

	public SimpleTypeFieldNamesRegistry(Path path) {
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
				String type = reader.nextName();

				if (this.entries.containsKey(type)) {
					throw new IllegalArgumentException("Duplicate type " + type);
				}

				switch (reader.peek()) {
					case STRING -> {
						String localName = reader.nextString();
						this.entries.put(type, new Entry(type, localName, CasingUtil.toScreamingSnakeCase(localName)));
					}
					case BEGIN_OBJECT -> {
						String localName = null;
						String staticName = null;
						boolean exclusive = false;
						Inherit inherit = Inherit.DEFAULT;
						List<Name> fallback = Collections.emptyList();

						reader.beginObject();

						while (reader.hasNext()) {
							String key = reader.nextName();

							switch (key) {
								case "local_name" -> localName = reader.nextString();
								case "static_name" -> staticName = reader.nextString();
								case "exclusive" -> exclusive = reader.nextBoolean();
								case "inherit" -> inherit = Inherit.read(reader);
								case "fallback" -> {
									reader.beginArray();

									fallback = this.collectFallbacks(reader, type);

									reader.endArray();
								}
								default -> reader.skipValue();
							}
						}

						reader.endObject();

						if (localName == null) {
							Logger.error("Failed parsing local name for type " + type);
							break;
						}

						if (staticName == null) staticName = CasingUtil.toScreamingSnakeCase(localName);

						this.entries.put(type, new Entry(type, new Name(localName, staticName), exclusive, inherit, fallback));
					}
					default -> reader.skipValue();
				}
			}

			reader.endObject();
		} catch (IOException e) {
			Logger.error(e, "Failed to read simple type field names registry.");
		}
	}

	private List<Name> collectFallbacks(JsonReader reader, String type) throws IOException {
		var list = new ArrayList<Name>();

		while (reader.hasNext()) {
			switch (reader.peek()) {
				case STRING -> {
					String name = reader.nextString();
					list.add(new Name(name, CasingUtil.toScreamingSnakeCase(name)));
				}
				case BEGIN_OBJECT -> {
					String localName = null;
					String staticName = null;
					reader.beginObject();

					while (reader.hasNext()) {
						String key = reader.nextName();

						switch (key) {
							case "local_name" -> localName = reader.nextString();
							case "static_name" -> staticName = reader.nextString();
							default -> reader.skipValue();
						}
					}

					reader.endObject();

					if (localName == null) {
						Logger.error("Failed parsing fallback local name for type " + type);
						break;
					}

					if (staticName == null) staticName = CasingUtil.toScreamingSnakeCase(localName);

					list.add(new Name(localName, staticName));
				}
			}
		}

		return list;
	}

	public record Entry(String type, Name name, boolean exclusive, Inherit inherit, List<Name> fallback) {
		public Entry(String type, String localName, String staticName) {
			this(type, new Name(localName, staticName), false, Inherit.None.INSTANCE, Collections.emptyList());
		}

		public @Nullable Name findFallback(Predicate<Name> predicate) {
			for (var fallback : this.fallback) {
				if (predicate.test(fallback)) {
					return fallback;
				}
			}

			return null;
		}
	}

	public record Name(String local, String constant) {
	}

	public sealed interface Inherit {
		Inherit DEFAULT = None.INSTANCE;

		String KEY = "inherit";
		String TYPE_KEY = "type";

		String MISSING_REQUIREMENT_MESSAGE_TEMPLATE = "%s requires a %s";

		static Inherit read(JsonReader reader) throws IOException {
			switch (reader.peek()) {
				case BOOLEAN -> {
					return reader.nextBoolean() ? Direct.INSTANCE : None.INSTANCE;
				}
				case BEGIN_OBJECT -> {
					reader.beginObject();

					if (!reader.hasNext() || !reader.nextName().equals(TYPE_KEY)) {
						throw new IllegalArgumentException("\"%s\" must be the first property of an \"%s\" object".formatted(TYPE_KEY, KEY));
					}

					String typeName = reader.nextString();
					final Type type;
					try {
						type = Type.valueOf(typeName);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(
							"Invalid \"%s\" object \"%s\"; must be one of: %s".formatted(
								KEY, TYPE_KEY,
								Arrays.stream(Type.values())
									.map(Object::toString)
									.map(name -> "\"" + name + "\"")
									.collect(Collectors.joining(", "))
							),
							e
						);
					}

					Inherit inherit = type.read(reader);

					while (reader.hasNext()) {
						reader.skipValue();
					}

					reader.endObject();

					return inherit;
				}
				default -> throw new IllegalArgumentException("Invalid \"%s\" value; must be BOOLEAN or OBJECT".formatted(KEY));
			}
		}

		enum Type {
			NONE,
			DIRECT,
			TRUNCATED_SUBTYPE_NAME,
			TRANSFORMED_SUBTYPE_NAME;

			Inherit read(JsonReader reader) throws IOException {
				return switch (this) {
					case NONE -> None.INSTANCE;
					case DIRECT -> Direct.INSTANCE;
					case TRUNCATED_SUBTYPE_NAME -> TruncatedSubtypeName.readValue(reader);
					case TRANSFORMED_SUBTYPE_NAME -> TransformedSubtypeName.readValue(reader);
				};
			}
		}

		final class None implements Inherit {
			public static final None INSTANCE = new None();

			private None() { }
		}

		final class Direct implements Inherit {
			public static final Direct INSTANCE = new Direct();

			private Direct() { }
		}

		record TruncatedSubtypeName(String suffix) implements Inherit {
			private static final String SUFFIX_KEY = "suffix";

			private static TruncatedSubtypeName readValue(JsonReader reader) throws IOException {
				while (reader.hasNext()) {
					String key = reader.nextName();
					if (key.equals(SUFFIX_KEY)) {
						final String suffix = reader.nextString();
						if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isJavaIdentifierPart)) {
							throw new IllegalArgumentException("invalid suffix: " + suffix);
						}

						return new TruncatedSubtypeName(suffix);
					} else {
						reader.skipValue();
					}
				}

				throw new IllegalArgumentException(MISSING_REQUIREMENT_MESSAGE_TEMPLATE.formatted(
					Type.TRUNCATED_SUBTYPE_NAME.toString(), SUFFIX_KEY
				));
			}
		}

		record TransformedSubtypeName(Pattern pattern, String replacement) implements Inherit {
			private static final String PATTERN_KEY = "pattern";
			private static final String REPLACEMENT_KEY = "replacement";

			private static TransformedSubtypeName readValue(JsonReader reader) throws IOException {
				Pattern pattern = null;
				String replacement = null;
				while (reader.hasNext()) {
					String key = reader.nextName();
					switch (key) {
						case PATTERN_KEY -> pattern = Pattern.compile(reader.nextString());
						case REPLACEMENT_KEY -> replacement = reader.nextString();
						default -> reader.skipValue();
					}
				}

				if (pattern == null) {
					throw new IllegalArgumentException(MISSING_REQUIREMENT_MESSAGE_TEMPLATE.formatted(
						Type.TRANSFORMED_SUBTYPE_NAME, PATTERN_KEY
					));
				}

				if (replacement == null) {
					throw new IllegalArgumentException(MISSING_REQUIREMENT_MESSAGE_TEMPLATE.formatted(
						Type.TRANSFORMED_SUBTYPE_NAME, REPLACEMENT_KEY
					));
				}

				return new TransformedSubtypeName(pattern, replacement);
			}
		}
	}
}
