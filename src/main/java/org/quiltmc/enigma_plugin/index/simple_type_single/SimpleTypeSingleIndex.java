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

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeFieldNamesRegistry.Name;
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.quiltmc.enigma_plugin.util.Descriptors;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index of fields/local variables that are of a rather simple type (as-in easy to guess the variable name) and which
 * they are entirely unique within their context (no other fields/local vars in the same scope have the same type).
 */
public class SimpleTypeSingleIndex implements Opcodes {
	private final Map<LocalVariableEntry, String> parameters = new HashMap<>();
	private final Map<FieldEntry, String> fields = new HashMap<>();
	private final Map<ClassNode, Map<String, FieldBuildingEntry>> fieldCache = new HashMap<>();
	private SimpleTypeFieldNamesRegistry registry;

	public void loadRegistry(Path path) {
		if (path == null) {
			this.registry = null;
			return;
		}

		this.registry = new SimpleTypeFieldNamesRegistry(path);
		this.registry.read();
	}

	public boolean isEnabled() {
		return this.registry != null;
	}

	public void dropCache() {
		this.fieldCache.clear();
	}

	public @Nullable String getField(FieldEntry fieldEntry) {
		return this.fields.get(fieldEntry);
	}

	public @Nullable String getParam(LocalVariableEntry paramEntry) {
		return this.parameters.get(paramEntry);
	}

	public Set<FieldEntry> getFields() {
		return this.fields.keySet();
	}

	public Set<LocalVariableEntry> getParams() {
		return this.parameters.keySet();
	}

	@TestOnly
	public List<LocalVariableEntry> getParamsOf(MethodEntry methodEntry) {
		var params = new ArrayList<LocalVariableEntry>();

		this.parameters.forEach((param, name) -> {
			if (param.getParent() != null && param.getParent().equals(methodEntry)) {
				params.add(param);
			}
		});

		return params;
	}

	public void visitClassNode(ClassProvider provider, ClassNode classNode) {
		if (!this.isEnabled()) return;

		var parentEntry = new ClassEntry(classNode.name);

		this.collectMatchingFields(provider, classNode).forEach((name, entry) -> {
			if (entry != FieldBuildingEntry.NULL) {
				var fieldEntry = new FieldEntry(parentEntry, entry.node().name, new TypeDescriptor(entry.node().desc));
				this.fields.put(fieldEntry,
						AsmUtil.matchAccess(entry.node(), ACC_STATIC, ACC_FINAL)
								? entry.name().staticName()
								: entry.name().local()
				);
			}
		});

		for (var method : classNode.methods) {
			if (method.parameters == null) continue;

			var methodDescriptor = new MethodDescriptor(method.desc);
			var methodEntry = new MethodEntry(parentEntry, method.name, methodDescriptor);
			var parameters = Descriptors.getParameters(method);

			var types = new HashMap<Type, Integer>();

			for (var param : parameters) {
				types.compute(param.type(), (t, old) -> {
					if (old == null) {
						return 0;
					} else {
						return old + 1;
					}
				});
			}

			var bannedTypes = new HashSet<Type>();
			types.forEach((type, amount) -> {
				if (amount > 1) bannedTypes.add(type);
			});

			this.collectMatchingParameters(method, bannedTypes, parameters).forEach((name, param) -> {
				if (!param.isNull()) {
					boolean isStatic = AsmUtil.maskMatch(method.access, ACC_STATIC);
					int index = param.index() + (isStatic ? 0 : 1);
					var paramEntry = new LocalVariableEntry(methodEntry, index, "", true, null);
					this.parameters.put(paramEntry, name);
				}
			});
		}
	}

	private Map<String, FieldBuildingEntry> collectMatchingFields(ClassProvider classProvider,
			ClassNode classNode) {
		var existing = this.fieldCache.get(classNode);

		if (existing != null) return existing;

		var knownFields = new HashMap<String, FieldBuildingEntry>();
		for (var field : classNode.fields) {
			if (classNode.outerClass != null) {
				ClassNode outerClass = classProvider.get(classNode.outerClass);

				if (outerClass != null) {
					knownFields.putAll(this.collectMatchingFields(classProvider, outerClass));
				}
			}

			if (field.desc.charAt(0) != 'L') continue;
			String type = field.desc.substring(1, field.desc.length() - 1);

			var entry = this.registry.getEntry(type);
			if (entry != null) {
				var existingEntry = knownFields.get(entry.name().local());

				if (existingEntry != null) {
					Name foundFallback = entry.findFallback(fallback -> !knownFields.containsKey(fallback.local()));

					if (foundFallback != null) {
						knownFields.put(foundFallback.local(), new FieldBuildingEntry(field, foundFallback, entry));

						if (existingEntry != FieldBuildingEntry.NULL && existingEntry.entry().exclusive()) {
							Name replacement = existingEntry.entry().findFallback(
									fallback -> !knownFields.containsKey(fallback.local())
							);

							knownFields.put(entry.name().local(), FieldBuildingEntry.NULL);

							if (replacement != null) {
								knownFields.put(replacement.local(),
										new FieldBuildingEntry(existingEntry.node(), replacement, existingEntry.entry())
								);
							}
						}
					} else {
						knownFields.put(entry.name().local(), FieldBuildingEntry.NULL);
					}
				} else {
					knownFields.put(entry.name().local(), new FieldBuildingEntry(field, entry.name(), entry));
				}
			}
		}

		this.fieldCache.put(classNode, knownFields);

		return knownFields;
	}

	private Map<String, ParameterBuildingEntry> collectMatchingParameters(MethodNode method, Set<Type> bannedTypes,
			List<Descriptors.ParameterEntry> parameters) {
		var knownParameters = new HashMap<String, ParameterBuildingEntry>();

		for (int index = 0, lvtIndex = 0; index < method.parameters.size(); index++) {
			if (index > 0) lvtIndex += parameters.get(index - 1).getSize();

			if (bannedTypes.contains(parameters.get(index).type())) continue;

			ParameterNode node = method.parameters.get(index);
			String desc = parameters.get(index).getDescriptor();
			if (desc.charAt(0) != 'L') continue;
			String type = desc.substring(1, desc.length() - 1);

			var entry = this.registry.getEntry(type);
			if (entry != null) {
				ParameterBuildingEntry existingEntry = knownParameters.get(entry.name().local());

				if (existingEntry != null) {
					if (existingEntry.entry() == entry) {
						knownParameters.put(entry.name().local(), ParameterBuildingEntry.createNull(entry));
						continue;
					}

					Name foundFallback = entry.findFallback(fallback -> !knownParameters.containsKey(fallback.local()));

					if (foundFallback != null) {
						knownParameters.put(foundFallback.local(), new ParameterBuildingEntry(node, lvtIndex, entry));

						if (!existingEntry.isNull() && existingEntry.entry().exclusive()) {
							Name replacement = existingEntry.entry().findFallback(
									fallback -> !knownParameters.containsKey(fallback.local())
							);

							knownParameters.put(entry.name().local(), ParameterBuildingEntry.createNull(entry));

							if (replacement != null) {
								knownParameters.put(replacement.local(), new ParameterBuildingEntry(
										existingEntry.node(), existingEntry.index(), existingEntry.entry()
								));
							}
						}
					} else {
						knownParameters.put(entry.name().local(), ParameterBuildingEntry.createNull(entry));
					}
				} else {
					knownParameters.put(entry.name().local(), new ParameterBuildingEntry(node, lvtIndex, entry));
				}
			}
		}

		return knownParameters;
	}

	private record FieldBuildingEntry(FieldNode node, Name name, SimpleTypeFieldNamesRegistry.Entry entry) {
		public static final FieldBuildingEntry NULL = new FieldBuildingEntry(null, null, null);
	}

	private record ParameterBuildingEntry(ParameterNode node, int index, SimpleTypeFieldNamesRegistry.Entry entry) {
		public static ParameterBuildingEntry createNull(SimpleTypeFieldNamesRegistry.Entry entry) {
			return new ParameterBuildingEntry(null, -1, entry);
		}

		public boolean isNull() {
			return this.node == null;
		}
	}
}
