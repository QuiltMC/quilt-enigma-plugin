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

package org.quiltmc.enigma_plugin.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;

public class AsmUtil {
	public static boolean maskMatch(int value, int... masks) {
		boolean matched = true;

		for (int mask : masks) {
			matched &= (value & mask) != 0;
		}

		return matched;
	}

	public static boolean matchAccess(FieldNode node, int... masks) {
		return maskMatch(node.access, masks);
	}

	public static boolean matchAccess(MethodNode node, int... masks) {
		return maskMatch(node.access, masks);
	}

	public static Optional<FieldNode> getField(ClassNode node, String name, String desc) {
		for (var field : node.fields) {
			if (field.name.equals(name) && field.desc.equals(desc)) {
				return Optional.of(field);
			}
		}

		return Optional.empty();
	}

	public static Optional<MethodNode> getMethod(ClassNode node, String name, String desc) {
		for (var method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(desc)) {
				return Optional.of(method);
			}
		}

		return Optional.empty();
	}

	public static Optional<FieldNode> getFieldFromGetter(ClassNode classNode, MethodNode node) {
		if (!Descriptors.getDescriptor(node).getArgumentDescs().isEmpty()) return Optional.empty();
		if (node.instructions.size() != 3) return Optional.empty();
		if (node.instructions.get(0).getOpcode() != Opcodes.ALOAD) return Optional.empty();
		var getFieldNode = node.instructions.get(1);
		if (getFieldNode.getOpcode() != Opcodes.GETFIELD) return Optional.empty();

		var fieldInsnNode = (FieldInsnNode) getFieldNode;

		int expectedReturnOpcode = switch (fieldInsnNode.desc) {
			case "I" -> Opcodes.IRETURN;
			case "L" -> Opcodes.LRETURN;
			case "F" -> Opcodes.FRETURN;
			case "D" -> Opcodes.DRETURN;
			default -> Opcodes.ARETURN;
		};

		if (node.instructions.get(2).getOpcode() != expectedReturnOpcode) return Optional.empty();

		if (fieldInsnNode.owner.equals(classNode.name)) {
			return getField(classNode, fieldInsnNode.name, fieldInsnNode.desc);
		} else {
			return Optional.empty();
		}
	}

	public static Optional<FieldNode> getFieldFromSetter(ClassNode classNode, MethodNode node) {
		if (Descriptors.getDescriptor(node).getArgumentDescs().size() != 1) return Optional.empty();
		if (node.instructions.size() != 4) return Optional.empty();
		if (node.instructions.get(0).getOpcode() != Opcodes.ALOAD) return Optional.empty();
		if (node.instructions.get(3).getOpcode() != Opcodes.RETURN) return Optional.empty();
		var putFieldNode = node.instructions.get(2);
		if (putFieldNode.getOpcode() != Opcodes.PUTFIELD) return Optional.empty();

		var fieldInsnNode = (FieldInsnNode) putFieldNode;

		int expectedLoadOpcode = switch (fieldInsnNode.desc) {
			case "I" -> Opcodes.ILOAD;
			case "L" -> Opcodes.LLOAD;
			case "F" -> Opcodes.FLOAD;
			case "D" -> Opcodes.DLOAD;
			default -> Opcodes.ALOAD;
		};

		if (node.instructions.get(1).getOpcode() != expectedLoadOpcode) return Optional.empty();

		if (fieldInsnNode.owner.equals(classNode.name)) {
			return getField(classNode, fieldInsnNode.name, fieldInsnNode.desc);
		} else {
			return Optional.empty();
		}
	}
}
