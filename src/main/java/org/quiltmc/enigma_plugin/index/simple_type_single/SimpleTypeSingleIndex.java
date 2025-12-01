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

import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.index.Index;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeFieldNamesRegistry.Inherit;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeFieldNamesRegistry.Name;
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.quiltmc.enigma_plugin.util.Descriptors;
import org.quiltmc.enigma_plugin.util.EntryUtil;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.quiltmc.enigma_plugin.util.StringUtil.getObjectTypeOrNull;

/**
 * Index of fields/local variables that are of a rather simple type (as-in easy to guess the variable name) and which
 * they are entirely unique within their context (no other fields/local vars in the same scope have the same type).
 */
public class SimpleTypeSingleIndex extends Index {
	private final Map<LocalVariableEntry, String> parameters = new HashMap<>();
	private final Map<LocalVariableEntry, List<String>> parameterFallbacks = new HashMap<>();
	private final Map<FieldEntry, String> fields = new HashMap<>();
	private final Map<ClassNode, Map<String, FieldBuildingEntry>> fieldCache = new HashMap<>();
	private final Set<String> unverifiedTypes = new HashSet<>();

	private SimpleTypeFieldNamesRegistry registry;
	private InheritanceIndex inheritance;
	private VerificationLevel verificationLevel = VerificationLevel.DEFAULT;

	public SimpleTypeSingleIndex() {
		super(null);
	}

	@Override
	public void withContext(EnigmaServiceContext<JarIndexerService> context) {
		super.withContext(context);

		this.verificationLevel = context.getSingleArgument(Arguments.SIMPLE_TYPE_VERIFICATION_ERROR_LEVEL)
			.map(value -> {
				try {
					return VerificationLevel.valueOf(value);
				} catch (IllegalArgumentException e) {
					throw new IllegalStateException(
						"Invalid %s value: \"%s\"; must be one of %s".formatted(
							Arguments.SIMPLE_TYPE_VERIFICATION_ERROR_LEVEL,
							value,
							Arrays.stream(VerificationLevel.values())
								.map(Enum::name)
								.map(name -> '"' + name + '"')
								.collect(Collectors.joining(", "))
						),
						e
					);
				}
			})
			.orElse(VerificationLevel.DEFAULT);

		this.loadRegistry(context.getSingleArgument(Arguments.SIMPLE_TYPE_FIELD_NAMES_PATH)
				.map(context::getPath).orElse(null));
	}

	@Override
	public void setIndexingContext(Set<String> classes, JarIndex jarIndex) {
		this.inheritance = jarIndex.getIndex(InheritanceIndex.class);
	}

	private void loadRegistry(Path path) {
		if (path == null) {
			this.registry = null;
			return;
		}

		this.registry = SimpleTypeFieldNamesRegistry.readFrom(path);

		this.unverifiedTypes.clear();
		if (this.verificationLevel != VerificationLevel.NONE) {
			this.registry.streamTypes().forEach(this.unverifiedTypes::add);
		}
	}

	@Override
	public boolean isEnabled() {
		return this.registry != null;
	}

	public @Nullable String getField(FieldEntry fieldEntry) {
		return this.fields.get(fieldEntry);
	}

	public @Nullable String getParam(LocalVariableEntry paramEntry) {
		return this.parameters.get(paramEntry);
	}

	public @Nullable List<String> getParamFallbacks(LocalVariableEntry paramEntry) {
		return this.parameterFallbacks.get(paramEntry);
	}

	public Set<FieldEntry> getFields() {
		return this.fields.keySet();
	}

	public Set<LocalVariableEntry> getParams() {
		return this.parameters.keySet();
	}

	public void verifyTypes() {
		if (this.verificationLevel != VerificationLevel.NONE) {
			if (!this.unverifiedTypes.isEmpty()) {
				boolean single = this.unverifiedTypes.size() == 1;
				StringBuilder message = new StringBuilder("The following simple type field name type");
				message.append(single ? " is" : "s are");
				message.append(" missing:");

				if (single) {
					message.append(' ').append(this.unverifiedTypes.iterator().next());
				} else {
					this.unverifiedTypes.forEach(type -> message.append("\n\t").append(type));
				}

				if (this.verificationLevel == VerificationLevel.WARN) {
					Logger.warn(message);
				} else {
					throw new IllegalStateException(message.toString());
				}
			}
		}
	}

	@Override
	public void onIndexingEnded() {
		this.fieldCache.clear();
	}

	@Override
	public void visitClassNode(ClassProvider provider, ClassNode node) {
		if (!this.isEnabled()) return;

		var parentEntry = new ClassEntry(node.name);

		this.unverifiedTypes.remove(node.name);

		this.collectMatchingFields(provider, node, parentEntry).forEach((name, entry) -> {
			if (!entry.isNull()) {
				var fieldEntry = new FieldEntry(entry.parent, entry.node().name, new TypeDescriptor(entry.node().desc));
				this.fields.put(fieldEntry, name);
			}
		});

		for (var method : node.methods) {
			if (method.parameters == null) continue;

			var methodDescriptor = new MethodDescriptor(method.desc);
			var methodEntry = new MethodEntry(parentEntry, method.name, methodDescriptor);
			var parameters = Descriptors.getParameters(method);

			// Count the times a type is used in the descriptor
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

			// Don't propose names for types appearing more than once
			var bannedTypes = new HashSet<Type>();
			types.forEach((type, amount) -> {
				if (amount > 1) bannedTypes.add(type);
			});

			this.collectMatchingParameters(method, bannedTypes, parameters).forEach((name, param) -> {
				if (!param.isNull()) {
					boolean isStatic = AsmUtil.matchAccess(method, ACC_STATIC);
					int index = param.index() + (isStatic ? 0 : 1);
					var paramEntry = new LocalVariableEntry(methodEntry, index);
					this.parameters.put(paramEntry, name);
					this.parameterFallbacks.put(paramEntry, param.entry.fallback().stream().map(Name::local).toList());
				}
			});
		}
	}

	private Map<String, FieldBuildingEntry> collectMatchingFields(ClassProvider classProvider, ClassNode classNode, ClassEntry parent) {
		var existing = this.fieldCache.get(classNode);

		if (existing != null) return existing;

		var knownFields = new HashMap<String, FieldBuildingEntry>();
		for (var field : classNode.fields) {
			// Collect names from the outer class as initial context
			if (classNode.outerClass != null) {
				ClassNode outerClass = classProvider.get(classNode.outerClass);

				if (outerClass != null) {
					knownFields.putAll(this.collectMatchingFields(classProvider, outerClass, new ClassEntry(outerClass.name)));
				}
			}

			String type = this.verifyTypeOrNull(field.desc);
			if (type == null) continue;

			var entry = this.getEntry(type);
			if (entry != null) {
				Function<Name, String> nameGetter = AsmUtil.matchAccess(field, ACC_STATIC, ACC_FINAL) ? Name::constant : Name::local;

				// Check if there's a field by the default name
				var existingEntry = knownFields.get(nameGetter.apply(entry.name()));

				if (existingEntry != null) {
					// If the existing field is of the same type, remove it and skip this one
					if (existingEntry.entry() == entry) {
						knownFields.put(nameGetter.apply(entry.name()), FieldBuildingEntry.createNull(entry));
						continue;
					}

					// If there's already a field by the default name, find a fallback name
					Name foundFallback = entry.findFallback(fallback -> !knownFields.containsKey(nameGetter.apply(fallback)));

					if (foundFallback != null) {
						knownFields.put(nameGetter.apply(foundFallback), new FieldBuildingEntry(parent, field, foundFallback, entry));

						// If the existing entry is exclusive, remove it and if possible replace it with one of its fallbacks
						if (!existingEntry.isNull() && existingEntry.entry().exclusive()) {
							Name replacement = existingEntry.entry().findFallback(
									fallback -> !knownFields.containsKey(nameGetter.apply(fallback))
							);

							knownFields.put(nameGetter.apply(entry.name()), FieldBuildingEntry.createNull(entry));

							if (replacement != null) {
								knownFields.put(nameGetter.apply(replacement),
										new FieldBuildingEntry(parent, existingEntry.node(), replacement, existingEntry.entry())
								);
							}
						}
					} else {
						// If a fallback name couldn't be found, remove the name for the existing field
						knownFields.put(nameGetter.apply(entry.name()), FieldBuildingEntry.createNull(entry));
					}
				} else {
					// Another field with the name doesn't exist, proceed as usual
					knownFields.put(nameGetter.apply(entry.name()), new FieldBuildingEntry(parent, field, entry.name(), entry));
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
			String type = this.verifyTypeOrNull(parameters.get(index).getDescriptor());
			if (type == null) continue;

			var entry = this.getEntry(type);
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

	@Nullable
	private SimpleTypeFieldNamesRegistry.Entry getEntry(String type) {
		// Default to returning this if it is specified
		var entry = this.registry.getEntry(type);

		if (entry != null) {
			return entry;
		}

		// Check all parent classes for an entry. This goes in order of super/interface, supersuper/interfacesuper, etc
		return EntryUtil.streamAncestors(new ClassEntry(type), this.inheritance)
			.flatMap(ancestor -> Optional
				.ofNullable(this.registry.getEntry(ancestor.getFullName()))
				// Only return if the entry allows inheritance
				.filter(ancestorEntry -> ancestorEntry.inherit() == Inherit.Direct.INSTANCE)
				.stream()
			)
			.findFirst()
			.orElse(null);
	}

	@Nullable
	private String verifyTypeOrNull(String descriptor) {
		String type = getObjectTypeOrNull(descriptor);
		if (type == null) {
			return null;
		}

		this.unverifiedTypes.remove(type);

		return type;
	}

	private record FieldBuildingEntry(ClassEntry parent, FieldNode node, Name name, SimpleTypeFieldNamesRegistry.Entry entry) {
		static FieldBuildingEntry createNull(SimpleTypeFieldNamesRegistry.Entry entry) {
			return new FieldBuildingEntry(null, null, null, entry);
		}

		boolean isNull() {
			return this.node == null;
		}
	}

	private record ParameterBuildingEntry(ParameterNode node, int index, SimpleTypeFieldNamesRegistry.Entry entry) {
		static ParameterBuildingEntry createNull(SimpleTypeFieldNamesRegistry.Entry entry) {
			return new ParameterBuildingEntry(null, -1, entry);
		}

		boolean isNull() {
			return this.node == null;
		}
	}

	private enum VerificationLevel {
		NONE, WARN, THROW;

		static final VerificationLevel DEFAULT = WARN;
	}
}
