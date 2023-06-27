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

package org.quiltmc.enigmaplugin;

import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceContext;

/**
 * Contains all the keys of the arguments used by this plugin.
 */
public class Arguments {
	public static final String DISABLE_RECORDS = "disable_records";
	public static final String DISABLE_ENUM_FIELDS = "disable_enum_fields";
	public static final String DISABLE_EQUALS = "disable_equals";
	public static final String DISABLE_LOGGER = "disable_logger";
	public static final String DISABLE_CONSTRUCTOR_PARAMS = "disable_constructor_params";
	public static final String DISABLE_GETTER_SETTER = "disable_getter_setter";
	public static final String DISABLE_CODECS = "disable_codecs";
	public static final String DISABLE_MAP_NON_HASHED = "disable_map_non_hashed";
	public static final String CUSTOM_CODECS = "custom_codecs";
	public static final String SIMPLE_TYPE_FIELD_NAMES_PATH = "simple_type_field_names_path";

	public static <T extends EnigmaService> boolean isDisabled(EnigmaServiceContext<T> context, String arg) {
		return context.getArgument(arg).map(Boolean::parseBoolean).orElse(false);
	}
}
