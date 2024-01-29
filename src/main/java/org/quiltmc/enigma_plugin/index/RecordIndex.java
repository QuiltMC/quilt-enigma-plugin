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

import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.objectweb.asm.Handle;
import org.quiltmc.enigma_plugin.Arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordIndex extends Index {
	private static final Handle TO_STRING_HANDLE = new Handle(H_INVOKESTATIC, "java/lang/runtime/ObjectMethods", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
	private static final Handle HASH_CODE_HANDLE = new Handle(H_INVOKESTATIC, "java/lang/runtime/ObjectMethods", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
	private static final Handle EQUALS_HANDLE = new Handle(H_INVOKESTATIC, "java/lang/runtime/ObjectMethods", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
	private final Map<ClassEntry, RecordComponentData> records = new HashMap<>();

	public RecordIndex() {
		super(Arguments.DISABLE_RECORDS);
	}

	private static ClassEntry getClassEntry(ClassNode node) {
		return new ClassEntry(node.name);
	}

	private static FieldEntry createFieldEntry(ClassEntry classEntry, Handle fieldHandle) {
		int tag = fieldHandle.getTag();
		if (tag == H_GETFIELD || tag == H_GETSTATIC || tag == H_PUTFIELD || tag == H_PUTSTATIC) {
			String className = fieldHandle.getOwner();
			if (classEntry.getFullName().equals(className)) {
				String fieldName = fieldHandle.getName();
				String fieldDesc = fieldHandle.getDesc();
				return new FieldEntry(classEntry, fieldName, new TypeDescriptor(fieldDesc));
			}
		}

		throw new IllegalArgumentException("Invalid field handle");
	}

	private static int findFirstALoad(InsnList instructions) {
		ListIterator<AbstractInsnNode> iterator = instructions.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			AbstractInsnNode insn = iterator.next();
			if (insn.getOpcode() == ALOAD) {
				return i;
			}

			i++;
		}

		return -1;
	}

	private static InvokeDynamicInsnNode getDefaultToStringInvokeDynamic(MethodNode node) {
		if (node.access != (ACC_PUBLIC | ACC_FINAL)) {
			return null;
		}

		InsnList instructions = node.instructions;
		if (instructions.size() < 3) {
			return null;
		}

		int offset = findFirstALoad(instructions);
		if (offset == -1 || offset + 1 >= instructions.size()) {
			return null;
		}

		AbstractInsnNode first = instructions.get(offset);
		if (((VarInsnNode) first).var != 0) {
			return null;
		}

		AbstractInsnNode second = instructions.get(offset + 1);
		if (second.getOpcode() == INVOKEDYNAMIC) {
			InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) second;
			if (insn.name.equals("toString") && insn.bsm.equals(TO_STRING_HANDLE)) {
				return instructions.get(offset + 2).getOpcode() == ARETURN ? insn : null;
			}
		}

		return null;
	}

	private static InvokeDynamicInsnNode getDefaultHashCodeInvokeDynamic(MethodNode node) {
		if (node.access != (ACC_PUBLIC | ACC_FINAL)) {
			return null;
		}

		InsnList instructions = node.instructions;
		if (instructions.size() < 3) {
			return null;
		}

		int offset = findFirstALoad(instructions);
		if (offset == -1 || offset + 1 >= instructions.size()) {
			return null;
		}

		AbstractInsnNode first = instructions.get(offset);
		if (((VarInsnNode) first).var != 0) {
			return null;
		}

		AbstractInsnNode second = instructions.get(offset + 1);
		if (second.getOpcode() == INVOKEDYNAMIC) {
			InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) second;
			if (insn.name.equals("hashCode") && insn.bsm.equals(HASH_CODE_HANDLE)) {
				return instructions.get(offset + 2).getOpcode() == IRETURN ? insn : null;
			}
		}

		return null;
	}

	private static InvokeDynamicInsnNode getDefaultEqualsInvokeDynamic(MethodNode node) {
		if (node.access != (ACC_PUBLIC | ACC_FINAL)) {
			return null;
		}

		InsnList instructions = node.instructions;
		if (instructions.size() < 4) {
			return null;
		}

		int offset = findFirstALoad(instructions);
		if (offset == -1 || offset + 1 >= instructions.size()) {
			return null;
		}

		AbstractInsnNode first = instructions.get(offset);
		if (((VarInsnNode) first).var != 0) {
			return null;
		}

		AbstractInsnNode second = instructions.get(offset + 1);
		if (second.getOpcode() != ALOAD || ((VarInsnNode) second).var != 1) {
			return null;
		}

		AbstractInsnNode third = instructions.get(offset + 2);
		if (third.getOpcode() == INVOKEDYNAMIC) {
			InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) third;
			if (insn.name.equals("equals") && insn.bsm.equals(EQUALS_HANDLE)) {
				return instructions.get(offset + 3).getOpcode() == IRETURN ? insn : null;
			}
		}

		return null;
	}

	@Override
	public void visitClassNode(ClassNode node) {
		if ((node.access & ACC_RECORD) == 0 && !node.superName.equals("java/lang/Record")) {
			return;
		}

		ClassEntry classEntry = getClassEntry(node);
		if (this.records.containsKey(classEntry) && this.records.get(classEntry).hasComponents()) {
			return;
		}

		this.records.put(classEntry, new RecordComponentData());

		for (MethodNode methodNode : node.methods) {
			if (methodNode.name.equals("hashCode") && methodNode.desc.equals("()I")) {
				this.visitHashCodeNode(methodNode, classEntry);
			} else if (methodNode.name.equals("toString") && methodNode.desc.equals("()Ljava/lang/String;")) {
				this.visitToStringNode(methodNode, classEntry);
			} else if (methodNode.name.equals("equals") && methodNode.desc.equals("(Ljava/lang/Object;)Z")) {
				this.visitEqualsNode(methodNode, classEntry);
			} else {
				this.visitMethodNode(methodNode, classEntry);
			}
		}
	}

	private void visitToStringNode(MethodNode node, ClassEntry classEntry) {
		InvokeDynamicInsnNode invokeDynamicNode = getDefaultToStringInvokeDynamic(node);
		if (invokeDynamicNode != null) {
			this.computeFieldNames(classEntry, invokeDynamicNode);
		}
	}

	private void visitHashCodeNode(MethodNode node, ClassEntry classEntry) {
		InvokeDynamicInsnNode invokeDynamicNode = getDefaultHashCodeInvokeDynamic(node);
		if (invokeDynamicNode != null) {
			this.computeFieldNames(classEntry, invokeDynamicNode);
		}
	}

	private void visitEqualsNode(MethodNode node, ClassEntry classEntry) {
		InvokeDynamicInsnNode invokeDynamicNode = getDefaultEqualsInvokeDynamic(node);
		if (invokeDynamicNode != null) {
			this.computeFieldNames(classEntry, invokeDynamicNode);
		}
	}

	private void computeFieldNames(ClassEntry classEntry, InvokeDynamicInsnNode invokeDynamicNode) {
		Object[] bsmArgs = invokeDynamicNode.bsmArgs;
		if (bsmArgs.length == 2) {
			// No record components
			return;
		}

		if (bsmArgs.length < 2) {
			throw new IllegalStateException("Expected at least 2 bootstrap method arguments");
		}

		String[] unobfuscatedFieldNames = ((String) bsmArgs[1]).split(";");
		if (bsmArgs.length != 2 + unobfuscatedFieldNames.length) {
			throw new IllegalStateException("Expected " + (2 + unobfuscatedFieldNames.length) + " bootstrap method arguments");
		}

		for (int i = 0; i < unobfuscatedFieldNames.length; i++) {
			String unobfuscatedFieldName = unobfuscatedFieldNames[i];
			Handle fieldHandle = (Handle) bsmArgs[2 + i];
			FieldEntry fieldEntry = createFieldEntry(classEntry, fieldHandle);
			this.records.computeIfAbsent(classEntry, k -> new RecordComponentData()).add(fieldEntry, unobfuscatedFieldName);
		}
	}

	private void visitMethodNode(MethodNode node, ClassEntry classEntry) {
		// Process default accessor methods
		MethodDescriptor methodDescriptor = new MethodDescriptor(node.desc);
		if (!methodDescriptor.getArgumentDescs().isEmpty()) {
			return;
		}

		InsnList instructions = node.instructions;
		int offset = findFirstALoad(instructions);
		if (offset == -1) {
			return;
		}

		if (offset + 2 >= instructions.size()) {
			return;
		}

		AbstractInsnNode first = instructions.get(offset);
		if (((VarInsnNode) first).var != 0) {
			return;
		}

		AbstractInsnNode second = instructions.get(offset + 1);
		if (second.getOpcode() != GETFIELD || !((FieldInsnNode) second).owner.equals(classEntry.getFullName())) {
			return;
		}

		AbstractInsnNode third = instructions.get(offset + 2);
		if (third.getOpcode() != ARETURN && third.getOpcode() != IRETURN && third.getOpcode() != LRETURN && third.getOpcode() != FRETURN && third.getOpcode() != DRETURN) {
			return;
		}

		FieldInsnNode field = (FieldInsnNode) second;
		FieldEntry fieldEntry = new FieldEntry(classEntry, field.name, new TypeDescriptor(field.desc));
		if (!this.records.containsKey(classEntry)) {
			return;
		}

		RecordComponentData data = this.records.get(classEntry);
		if (fieldEntry.getDesc().equals(methodDescriptor.getReturnDesc()) && data.isComponentField(fieldEntry)) {
			MethodEntry methodEntry = new MethodEntry(classEntry, node.name, methodDescriptor);
			data.addAccessorMethod(fieldEntry, methodEntry);
		}
	}

	public boolean isRecord(ClassEntry classEntry) {
		return this.records.containsKey(classEntry);
	}

	public String getFieldName(ClassEntry parent, FieldEntry field) {
		return this.records.containsKey(parent) ? this.records.get(parent).getName(field) : null;
	}

	public String getInitParamName(ClassEntry parent, int lvtIndex) {
		return this.records.containsKey(parent) ? this.records.get(parent).getInitParamName(lvtIndex) : null;
	}

	public String getCanonicalConstructorDescriptor(ClassEntry parent) {
		return this.records.containsKey(parent) ? this.records.get(parent).getCanonicalConstructorDescriptor() : null;
	}

	public String getAccessorMethodName(ClassEntry parent, MethodEntry method) {
		return this.records.containsKey(parent) ? this.records.get(parent).getAccessorMethodName(method) : null;
	}

	@TestOnly
	protected Map<FieldEntry, String> getAllFieldNames() {
		return this.records.keySet().stream().flatMap(c -> this.getFieldNames(c).entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@TestOnly
	protected Map<MethodEntry, String> getAllMethodNames() {
		return this.records.keySet().stream().flatMap(c -> this.getMethodNames(c).entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@TestOnly
	protected Map<FieldEntry, String> getFieldNames(ClassEntry parent) {
		return this.records.containsKey(parent) ? this.records.get(parent).fieldNames : null;
	}

	@TestOnly
	protected Map<MethodEntry, String> getMethodNames(ClassEntry parent) {
		if (!this.records.containsKey(parent)) {
			return null;
		}

		RecordComponentData data = this.records.get(parent);
		return data.fieldAccessorMethods.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> data.getName(e.getValue())));
	}

	public Set<ClassEntry> getRecordClasses() {
		return this.records.keySet();
	}

	public Set<FieldEntry> getFields(ClassEntry parent) {
		if (this.records.containsKey(parent)) {
			return this.records.get(parent).fieldNames.keySet();
		}

		return Collections.emptySet();
	}

	public Set<MethodEntry> getMethods(ClassEntry parent) {
		if (this.records.containsKey(parent)) {
			return this.records.get(parent).fieldAccessorMethods.keySet();
		}

		return Collections.emptySet();
	}

	static class RecordComponentData {
		private final List<String> unobfuscatedFieldNames = new ArrayList<>();
		private final List<FieldEntry> fieldEntries = new ArrayList<>();
		private final Map<FieldEntry, String> fieldNames = new HashMap<>();
		private final Map<FieldEntry, MethodEntry> accessorMethods = new HashMap<>();
		private final Map<MethodEntry, FieldEntry> fieldAccessorMethods = new HashMap<>();

		public void add(FieldEntry fieldEntry, String unobfuscatedFieldName) {
			if (this.fieldNames.containsKey(fieldEntry)) {
				return;
			}

			this.unobfuscatedFieldNames.add(unobfuscatedFieldName);
			this.fieldEntries.add(fieldEntry);
			this.fieldNames.put(fieldEntry, unobfuscatedFieldName);
		}

		public String getName(FieldEntry fieldEntry) {
			return this.fieldNames.get(fieldEntry);
		}

		public boolean isComponentField(FieldEntry fieldEntry) {
			return this.fieldEntries.contains(fieldEntry);
		}

		public String getInitParamName(int lvtIndex) {
			return this.getName(this.getFieldByInitLvtIndex(lvtIndex));
		}

		private FieldEntry getFieldByInitLvtIndex(int lvtIndex) {
			int i = 1;
			for (FieldEntry fieldEntry : this.fieldEntries) {
				if (i == lvtIndex) {
					return fieldEntry;
				}

				i += fieldEntry.getDesc().getSize();
			}

			return null;
		}

		public String getCanonicalConstructorDescriptor() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (FieldEntry fieldEntry : this.fieldEntries) {
				sb.append(fieldEntry.getDesc());
			}

			sb.append(")V");
			return sb.toString();
		}

		public void addAccessorMethod(FieldEntry fieldEntry, MethodEntry accessorMethod) {
			if (this.accessorMethods.containsKey(fieldEntry)) {
				return;
			}

			this.accessorMethods.put(fieldEntry, accessorMethod);
			this.fieldAccessorMethods.put(accessorMethod, fieldEntry);
		}

		private FieldEntry getAccessorMethodField(MethodEntry methodEntry) {
			return this.fieldAccessorMethods.get(methodEntry);
		}

		public String getAccessorMethodName(MethodEntry methodEntry) {
			return this.fieldNames.get(this.getAccessorMethodField(methodEntry));
		}

		public boolean hasComponents() {
			return !this.unobfuscatedFieldNames.isEmpty();
		}
	}
}
