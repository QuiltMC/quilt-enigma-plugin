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

package org.quiltmc.enigmaplugin.index.enumfields;

import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigmaplugin.index.Index;

import java.util.*;

public class EnumFieldsIndex implements Index {
	private final Map<String, Set<String>> enumFields = new HashMap<>();
	private final Map<String, List<MethodNode>> staticInitializers = new HashMap<>();
	private Map<FieldEntry, String> fieldNames;

	@Override
	public void visitClassNode(ClassNode node) {
		for (FieldNode field : node.fields) {
			if ((field.access & ACC_ENUM) != 0) {
				if (!this.enumFields.computeIfAbsent(node.name, k -> new HashSet<>()).add(field.name + ":" + field.desc)) {
					throw new IllegalStateException("Found a duplicate enum field with name \"" + field.name + "\"");
				}
			}
		}

		for (MethodNode method : node.methods) {
			if (method.name.equals("<clinit>")) {
				this.staticInitializers.computeIfAbsent(node.name, k -> new ArrayList<>()).add(method);
			}
		}
	}

	public void findFieldNames() {
		try {
			this.fieldNames = new FieldNameFinder().findNames(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean hasName(FieldEntry field) {
		return this.fieldNames.containsKey(field);
	}

	public String getName(FieldEntry field) {
		return this.fieldNames.get(field);
	}

	public Set<FieldEntry> getFields() {
		return this.fieldNames.keySet();
	}

	protected Map<String, Set<String>> getEnumFields() {
		return this.enumFields;
	}

	protected Map<String, List<MethodNode>> getStaticInitializers() {
		return this.staticInitializers;
	}
}
