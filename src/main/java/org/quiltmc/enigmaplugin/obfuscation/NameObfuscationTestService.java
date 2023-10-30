/*
 * Copyright 2016, 2017, 2018, 2019 FabricMC
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

package org.quiltmc.enigmaplugin.obfuscation;

import cuchaz.enigma.api.service.EnigmaServiceContext;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

public class NameObfuscationTestService implements ObfuscationTestService {
	private static final String DEFAULT_PACKAGE = "net/minecraft/unmapped";
	private static final String DEFAULT_CLASS_PREFIX = "C_";
	private static final String DEFAULT_FIELD_PREFIX = "f_";
	private static final String DEFAULT_METHOD_PREFIX = "m_";
	@SuppressWarnings("FieldCanBeLocal")
	private final String packagePrefix;
	private final String classPrefix;
	private final String fieldPrefix;
	private final String methodPrefix;
	private final String classPackagePrefix;

	public NameObfuscationTestService(EnigmaServiceContext<ObfuscationTestService> context) {
		this.packagePrefix = context.getArgument("package").orElse(DEFAULT_PACKAGE) + "/";
		this.classPrefix = context.getArgument("classPrefix").orElse(DEFAULT_CLASS_PREFIX);
		this.fieldPrefix = context.getArgument("fieldPrefix").orElse(DEFAULT_FIELD_PREFIX);
		this.methodPrefix = context.getArgument("methodPrefix").orElse(DEFAULT_METHOD_PREFIX);

		this.classPackagePrefix = this.packagePrefix + this.classPrefix;
	}

	@Override
	public boolean testDeobfuscated(Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			String[] parts = classEntry.getFullName().split("\\$");

			String lastPart = parts[parts.length - 1];
			return !lastPart.startsWith(this.classPrefix) && !lastPart.startsWith(this.classPackagePrefix);
		} else if (entry instanceof FieldEntry) {
			return !entry.getName().startsWith(this.fieldPrefix);
		} else if (entry instanceof MethodEntry) {
			return !entry.getName().startsWith(this.methodPrefix);
		} else {
			// unknown type
			return false;
		}
	}
}
