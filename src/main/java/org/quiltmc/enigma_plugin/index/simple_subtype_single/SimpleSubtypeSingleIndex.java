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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
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
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.index.Index;
import org.quiltmc.enigma_plugin.index.simple_subtype_single.SimpleSubtypeFieldNamesRegistry.Entry;
import org.quiltmc.enigma_plugin.util.AsmUtil;
import org.quiltmc.enigma_plugin.util.Descriptors;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.quiltmc.enigma_plugin.util.AsmUtil.getObjectTypeOrNull;

/**
 * Index of fields/local variables that are of a rather simple type (as-in easy to guess the variable name) and which
 * they are entirely unique within their context (no other fields/local vars in the same scope have the same type).
 */
public class SimpleSubtypeSingleIndex extends Index {
	private final Map<LocalVariableEntry, ParamInfo> parameters = new HashMap<>();
	private final Map<FieldEntry, FieldInfo> fields = new HashMap<>();
	private final Map<ClassNode, Map<FieldEntry, FieldInfo>> fieldCache = new HashMap<>();
	private SimpleSubtypeFieldNamesRegistry registry;

	private InheritanceIndex inheritance;

	public SimpleSubtypeSingleIndex() {
		super(null);
	}

	@Override
	public void withContext(EnigmaServiceContext<JarIndexerService> context) {
		super.withContext(context);

		this.loadRegistry(context.getSingleArgument(Arguments.SIMPLE_SUBTYPE_FIELD_NAMES_PATH)
				.map(context::getPath).orElse(null));
	}

	@Override
	public void setIndexingContext(Set<String> classes, JarIndex jarIndex) {
		this.inheritance = jarIndex.getIndex(InheritanceIndex.class);
	}

	public void loadRegistry(Path path) {
		if (path == null) {
			this.registry = null;
			return;
		}

		this.registry = new SimpleSubtypeFieldNamesRegistry(path);
		this.registry.read();
	}

	@Override
	public boolean isEnabled() {
		return this.registry != null;
	}

	public void dropCache() {
		this.fieldCache.clear();
	}

	public Map<FieldEntry, FieldInfo> getFields() {
		return this.fields;
	}

	public Map<LocalVariableEntry, ParamInfo> getParams() {
		return this.parameters;
	}

	@Override
	public void onIndexingEnded() {
		this.dropCache();
	}

	@Override
	public void visitClassNode(ClassProvider provider, ClassNode node) {
		if (!this.isEnabled()) return;

		var parentEntry = new ClassEntry(node.name);

		this.fields.putAll(this.collectMatchingFields(provider, node, parentEntry));

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
			var bannedTypes = types.entrySet().stream()
					.filter(entry -> entry.getValue() > 1)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());

			this.parameters.putAll(this.collectMatchingParameters(method, bannedTypes, parameters, methodEntry));
		}
	}

	private Map<FieldEntry, FieldInfo> collectMatchingFields(ClassProvider classProvider, ClassNode classNode, ClassEntry parentEntry) {
		var existing = this.fieldCache.get(classNode);

		if (existing != null) return existing;

		final Map<FieldEntry, FieldInfo> matchingFields = this.buildMatchingFields(classProvider, classNode, parentEntry).build();

		this.fieldCache.put(classNode, matchingFields);

		return matchingFields;
	}

	private FieldBuilders buildMatchingFields(ClassProvider classProvider, ClassNode classNode, ClassEntry parentEntry) {
		var builders = FieldBuilders.of();

		for (var field : classNode.fields) {
			// Collect names from the outer class as initial context
			if (classNode.outerClass != null) {
				ClassNode outerClass = classProvider.get(classNode.outerClass);

				if (outerClass != null) {
					final FieldBuilders outerBuilders = this.buildMatchingFields(classProvider, outerClass, new ClassEntry(outerClass.name));
					builders.fieldsByType.putAll(outerBuilders.fieldsByType);
					builders.constantFieldsByType.putAll(outerBuilders.fieldsByType);
				}
			}

			String type = getObjectTypeOrNull(field.desc);
			if (type == null) {
				continue;
			}

			var entry = this.getEntry(type);
			if (entry != null) {
				boolean isConstant = AsmUtil.matchAccess(field, ACC_STATIC, ACC_FINAL);
				Map<String, FieldBuilderEntry> destination = isConstant ? builders.constantFieldsByType : builders.fieldsByType;
				if (destination.containsKey(type)) {
					destination.put(type, FieldBuilderEntry.DUPLICATE);
				} else {
					destination.put(type, new FieldBuilderEntry(parentEntry, field, type, entry, isConstant));
				}
			}
		}

		return builders;
	}

	private Map<LocalVariableEntry, ParamInfo> collectMatchingParameters(
			MethodNode method, Set<Type> bannedTypes,
			List<Descriptors.ParameterEntry> parameters,
			MethodEntry methodEntry
	) {
		var matchingParameters = new HashMap<LocalVariableEntry, ParamInfo>();

		for (int index = 0, lvtIndex = 0; index < method.parameters.size(); index++) {
			if (index > 0) {
				lvtIndex += parameters.get(index - 1).getSize();
			}

			Descriptors.ParameterEntry paramEntry = parameters.get(index);
			if (bannedTypes.contains(paramEntry.type())) {
				continue;
			}

			String descriptor = paramEntry.getDescriptor();
			String type = getObjectTypeOrNull(descriptor);
			if (type == null) {
				continue;
			}

			var entry = this.getEntry(type);
			if (entry != null) {
				boolean isStatic = AsmUtil.maskMatch(method.access, ACC_STATIC);
				matchingParameters.put(new LocalVariableEntry(methodEntry, lvtIndex + (isStatic ? 0 : 1)), new ParamInfo(entry, type));
			}
		}

		return matchingParameters;
	}

	@Nullable
	private Entry getEntry(String type) {
		// TODO filter out abstract classes
		if (this.registry.getEntry(type) != null) {
			// do not propose names for the super type
			return null;
		}

		// Check all parent classes for an entry. This goes in order of super/interface, supersuper/interfacesuper, etc
		for (ClassEntry ancestor : this.inheritance.getAncestors(new ClassEntry(type))) {
			var entry = this.registry.getEntry(ancestor.getFullName());

			if (entry != null) {
				return entry;
			}
		}

		return null;
	}

	public record FieldInfo(Entry subtypeEntry, String type, boolean isConstant) { }

	public record ParamInfo(Entry subtypeEntry, String type) { }

	private record FieldBuilderEntry(ClassEntry parent, FieldNode field, String type, Entry subtypeEntry, boolean isConstant) {
		static final FieldBuilderEntry DUPLICATE = new FieldBuilderEntry(null, null, null, null, false);

		static boolean isNotDuplicate(FieldBuilderEntry entry) {
			return entry != DUPLICATE;
		}

		FieldInfo toInfo() {
			return new FieldInfo(this.subtypeEntry, this.type, this.isConstant);
		}

		FieldEntry toEntry() {
			return new FieldEntry(this.parent, this.field.name, new TypeDescriptor(this.field.desc));
		}
	}

	private record FieldBuilders(Map<String, FieldBuilderEntry> fieldsByType, Map<String, FieldBuilderEntry> constantFieldsByType) {
		static FieldBuilders of() {
			return new FieldBuilders(new HashMap<>(), new HashMap<>());
		}

		Map<FieldEntry, FieldInfo> build() {
			return Stream
				.concat(this.fieldsByType.values().stream(), this.constantFieldsByType.values().stream())
				.filter(FieldBuilderEntry::isNotDuplicate)
				.collect(Collectors.toMap(
					FieldBuilderEntry::toEntry,
					FieldBuilderEntry::toInfo
				));
		}
	}
}
