/*
 * Copyright 2023 QuiltMC
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

package org.quiltmc.enigmaplugin.util;

import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class Descriptors {
	public static final TypeDescriptor VOID_TYPE = new TypeDescriptor("V");
	public static final TypeDescriptor BOOLEAN_TYPE = new TypeDescriptor("Z");

	public static MethodDescriptor getDescriptor(MethodNode node) {
		return new MethodDescriptor(node.desc);
	}

	public static List<Type> getParameterTypes(MethodNode node) {
		return List.of(Type.getArgumentTypes(node.desc));
	}

	public static List<ParameterEntry> getParameters(MethodNode node) {
		int lvtIndex = AsmUtil.matchAccess(node, Opcodes.ACC_STATIC) ? 0 : 1;
		var parameters = new ArrayList<ParameterEntry>();

		for (var type : getParameterTypes(node)) {
			parameters.add(new ParameterEntry(node, lvtIndex, type));

			lvtIndex += type.getSize();
		}

		return parameters;
	}

	public record ParameterEntry(MethodNode method, int lvtIndex, Type type) {
		public int getSize() {
			return this.type.getSize();
		}

		public String getDescriptor() {
			return this.type.getDescriptor();
		}
	}
}
