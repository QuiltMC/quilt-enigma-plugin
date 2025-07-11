/*
 * Copyright 2024 QuiltMC
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

package org.quiltmc.enigma_plugin.index.entity_rendering;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.tinylog.Logger;

import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.Signature;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.index.Index;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonToken;

public class EntityModelIndex extends Index {
	private final Map<ClassEntry, List<ModelEntry>> modelTree = new HashMap<>();

	private String modelClassName;
	private String entityModelClassName;
	private InheritanceIndex inheritance;

	public EntityModelIndex() {
		super(null);
	}

	@Override
	public void withContext(EnigmaServiceContext<JarIndexerService> context) {
		super.withContext(context);

		read(context.getSingleArgument(Arguments.ENTITY_RENDERING).map(context::getPath).orElseThrow());
	}

	private void read(Path path) {
		try (var reader = JsonReader.json5(path)) {
			if (reader.peek() != JsonToken.BEGIN_OBJECT) {
				throw new IllegalArgumentException("entity rendering file must be a JSON object!");
			}

			reader.beginObject();

			while (reader.hasNext()) {
				String field = reader.nextName();

				if (field.equals("base_model")) {
					if (Objects.requireNonNull(reader.peek()) == JsonToken.STRING) {
						this.modelClassName = reader.nextString();
						continue;
					}

					throw new IllegalArgumentException("base_model must be a string to a class! Found " + reader.peek());
				} else if (field.equals("entity_model")) {
					if (Objects.requireNonNull(reader.peek()) == JsonToken.STRING) {
						this.entityModelClassName = reader.nextString();
						continue;
					}

					throw new IllegalArgumentException("entity_model must be a string to a class! Found " + reader.peek());
				}

				reader.skipValue();
			}

			reader.endObject();
		} catch (IOException e) {
			Logger.error(e, "Failed to read entity rendering file!");
		}
	}

	@Override
	public void setIndexingContext(Set<String> classes, JarIndex jarIndex) {
		this.inheritance = jarIndex.getIndex(InheritanceIndex.class);
	}

	@Override
	public void visitClassNode(ClassNode node) {
		ClassEntry entry = new ClassEntry(node.name);
		if (this.isModel(entry)) {
			ClassEntry parent = this.inheritance.getParents(entry).iterator().next();

			ModelEntry model = getModelInformation(node, entry, parent);

			this.modelTree
				.computeIfAbsent(parent, k -> new ArrayList<>())
				.add(model);
		} else if (this.modelClassName.equals(node.name)) {
			this.modelTree.computeIfAbsent(entry, k -> new ArrayList<>());
		}
	}

	private @NotNull ModelEntry getModelInformation(ClassNode node, ClassEntry entry, ClassEntry parent) {
		if (!this.inheritance.getAncestors(entry).contains(new ClassEntry(entityModelClassName)) && !this.entityModelClassName.equals(node.name)) {
			return new DefaultModel(entry);
		}

		String signature = node.signature;
		if (signature == null) {
			return new EntityModel(entry, new ParentRenderState(parent));
		}

		int lt = signature.indexOf('<');
		int gt = signature.indexOf('>');
		String modelType = signature.substring(lt + 2, gt - 1);
		if (modelType.startsWith(":L")) {
			modelType = modelType.substring(2);
		}
		return new EntityModel(entry, new KnownRenderState(new ClassEntry(modelType), lt == 0));
	}

	private boolean isModel(ClassEntry entry) {
		Set<ClassEntry> ancestors = this.inheritance.getAncestors(entry);
		return ancestors.contains(new ClassEntry(modelClassName));
	}

	@Override
	public void onIndexingEnded() {
		System.out.println(this.modelTree);
	}

	@Override
	public void reset() {
		this.modelTree.clear();
	}

	record KnownRenderState(ClassEntry clazz, boolean extend) implements RenderState {}
	record ParentRenderState(ClassEntry parent) implements RenderState {}
	record EntityModel(ClassEntry entry, RenderState renderState) implements ModelEntry {}
	record DefaultModel(ClassEntry entry) implements ModelEntry {}

	sealed interface ModelEntry permits EntityModel, DefaultModel {
		ClassEntry entry();
	}

	sealed interface RenderState permits KnownRenderState, ParentRenderState {
	}
}
