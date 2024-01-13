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

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma_plugin.Arguments;

public abstract class Index implements Opcodes {
	@Nullable
	private final String toggleKey;
	private boolean enabled;

	protected Index(@Nullable String toggleKey, boolean enabled) {
		this.toggleKey = toggleKey;
		this.enabled = enabled;
	}

	protected Index(@Nullable String toggleKey) {
		this(toggleKey, true);
	}

	public void withContext(EnigmaServiceContext<JarIndexerService> context) {
		if (this.toggleKey != null) {
			this.enabled ^= Arguments.getBoolean(context, this.toggleKey);
		}
	}

	public void visitClassNode(ClassProvider classProvider, ClassNode classNode) {
		this.visitClassNode(classNode);
	}

	public void visitClassNode(ClassNode classNode) {
	}

	public boolean isEnabled() {
		return this.enabled;
	}
}
