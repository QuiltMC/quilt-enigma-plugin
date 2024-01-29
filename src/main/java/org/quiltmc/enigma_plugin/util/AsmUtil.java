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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.Optional;
import java.util.function.Predicate;

public class AsmUtil implements Opcodes {
	public static int getLocalIndex(MethodNode node, int local) {
		boolean isStatic = AsmUtil.matchAccess(node, ACC_STATIC);
		return getLocalIndex(isStatic, node.desc, local);
	}

	public static int getLocalIndex(boolean isStatic, String desc, int local) {
		var args = Type.getArgumentTypes(desc);
		int size = isStatic ? 0 : 1;

		for (int i = 0; i < args.length; i++) {
			if (local == size) {
				return i;
			} else if (size > local) {
				return -1;
			}

			size += args[i].getSize();
		}

		return -1;
	}

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

	public static boolean matchAccess(ParameterNode node, int... masks) {
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
		if (node.instructions.get(0).getOpcode() != ALOAD) return Optional.empty();
		var getFieldNode = node.instructions.get(1);
		if (getFieldNode.getOpcode() != GETFIELD) return Optional.empty();

		var fieldInsnNode = (FieldInsnNode) getFieldNode;

		int expectedReturnOpcode = switch (fieldInsnNode.desc) {
			case "I" -> IRETURN;
			case "L" -> LRETURN;
			case "F" -> FRETURN;
			case "D" -> DRETURN;
			default -> ARETURN;
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
		if (node.instructions.get(0).getOpcode() != ALOAD) return Optional.empty();
		if (node.instructions.get(3).getOpcode() != RETURN) return Optional.empty();
		var putFieldNode = node.instructions.get(2);
		if (putFieldNode.getOpcode() != PUTFIELD) return Optional.empty();

		var fieldInsnNode = (FieldInsnNode) putFieldNode;

		int expectedLoadOpcode = switch (fieldInsnNode.desc) {
			case "I" -> ILOAD;
			case "L" -> LLOAD;
			case "F" -> FLOAD;
			case "D" -> DLOAD;
			default -> ALOAD;
		};

		if (node.instructions.get(1).getOpcode() != expectedLoadOpcode) return Optional.empty();

		if (fieldInsnNode.owner.equals(classNode.name)) {
			return getField(classNode, fieldInsnNode.name, fieldInsnNode.desc);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Search for an instruction matching the given predicate in the stack of a method.
	 *
	 * @see #searchInsnInStack(InsnList, AbstractInsnNode, Frame[], Predicate, boolean)
	 */
	public static AbstractInsnNode searchInsnInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames, Predicate<AbstractInsnNode> insnPredicate) {
		return searchInsnInStack(insns, frameInsn, frames, insnPredicate, false);
	}

	/**
	 * Search for an instruction matching the given predicate in the stack of a method.
	 *
	 * @see #searchInsnInStack(InsnList, AbstractInsnNode, Frame[], Predicate)
	 */
	public static AbstractInsnNode searchInsnInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames, Predicate<AbstractInsnNode> insnPredicate, boolean shallow) {
		int frameIndex = insns.indexOf(frameInsn);
		var frame = frames[frameIndex];

		AbstractInsnNode lastStackInsn = null;
		for (int i = 0; i < frame.getStackSize(); i++) {
			var value = frame.getStack(i);
			for (var stackInsn : value.insns) {
				if (insnPredicate.test(stackInsn)) {
					return stackInsn;
				} else if (stackInsn.getOpcode() == INVOKESTATIC && !shallow) {
					if (!(frameInsn instanceof MethodInsnNode mInsn) || mInsn.owner.equals(((MethodInsnNode) stackInsn).owner)) {
						return searchInsnInStack(insns, stackInsn, frames, insnPredicate, false);
					}
				}

				lastStackInsn = stackInsn;
			}
		}

		if (!shallow && lastStackInsn != null && lastStackInsn.getOpcode() == NEW && lastStackInsn.getNext() != null && lastStackInsn.getNext().getOpcode() == DUP) {
			// Find the last frame containing the DUP instruction
			var dup = lastStackInsn.getNext();
			int searchFrameIndex = insns.indexOf(dup) + 1;
			var searchFrame = frames[searchFrameIndex];

			while (searchFrame != null && searchFrameIndex <= frameIndex) {
				boolean contains = false;
				for (int j = 0; j < searchFrame.getStackSize(); j++) {
					if (frame.getStack(j).insns.contains(dup)) {
						contains = true;
						break;
					}
				}

				if (contains) {
					searchFrameIndex++;
					if (searchFrameIndex < frames.length) {
						searchFrame = frames[searchFrameIndex];
					} else {
						searchFrame = null;
					}
				} else {
					var insn = insns.get(searchFrameIndex - 1); // This was the last instruction with a frame with a dup
					if (insn != frameInsn) {
						return searchInsnInStack(insns, insn, frames, insnPredicate, false);
					}
				}
			}
		}

		return null;
	}

	/**
	 * Search a non-blank string constant in the stack of a method.
	 *
	 * @see #searchInsnInStack(InsnList, AbstractInsnNode, Frame[], Predicate)
	 */
	public static String searchStringCstInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames) {
		var insn = searchInsnInStack(insns, frameInsn, frames,
				insnNode -> insnNode instanceof LdcInsnNode ldc && ldc.cst instanceof String constant && !constant.isBlank());
		if (insn instanceof LdcInsnNode ldc) {
			return (String) ldc.cst;
		}

		return null;
	}

	/**
	 * Shallow search a non-blank string constant in the stack of a method.
	 *
	 * @see #searchInsnInStack(InsnList, AbstractInsnNode, Frame[], Predicate)
	 */
	public static String shallowSearchStringCstInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames) {
		var insn = searchInsnInStack(insns, frameInsn, frames,
				insnNode -> insnNode instanceof LdcInsnNode ldc && ldc.cst instanceof String constant && !constant.isBlank(),
				true);
		if (insn instanceof LdcInsnNode ldc) {
			return (String) ldc.cst;
		}

		return null;
	}

	/**
	 * Search a static field, the parent of which isn't the given class, in the stack of a method.
	 *
	 * @see #searchInsnInStack(InsnList, AbstractInsnNode, Frame[], Predicate)
	 */
	public static FieldInsnNode searchStaticFieldReferenceInStack(InsnList insns, AbstractInsnNode frameInsn, Frame<SourceValue>[] frames, String clazz) {
		var insn = searchInsnInStack(insns, frameInsn, frames,
				insnNode -> insnNode instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == GETSTATIC && !fieldInsn.owner.equals(clazz));
		if (insn instanceof FieldInsnNode fieldInsn) {
			return fieldInsn;
		}

		return null;
	}
}
