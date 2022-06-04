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

package org.quiltmc.enigmaplugin.index;

import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

public class RecordIndex implements Opcodes {
    private static final Handle TO_STRING_HANDLE = new Handle(H_INVOKESTATIC, "java/lang/runtime/ObjectMethods", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
    private static final Handle HASH_CODE_HANDLE = new Handle(H_INVOKESTATIC, "java/lang/runtime/ObjectMethods", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
    private static final Handle EQUALS_HANDLE = new Handle(H_INVOKESTATIC, "java/lang/runtime/ObjectMethods", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
    private final Map<ClassEntry, RecordComponentData> records = new HashMap<>();

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

    public void visitClassNode(ClassNode node) {
        ClassEntry classEntry = getClassEntry(node);
        if (records.containsKey(classEntry) && records.get(classEntry).hasComponents()) {
            return;
        }
        records.put(classEntry, new RecordComponentData());

        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals("hashCode") && methodNode.desc.equals("()I")) {
                visitHashCodeNode(methodNode, classEntry);
            } else if (methodNode.name.equals("toString") && methodNode.desc.equals("()Ljava/lang/String;")) {
                visitToStringNode(methodNode, classEntry);
            } else if (methodNode.name.equals("equals") && methodNode.desc.equals("(Ljava/lang/Object;)Z")) {
                visitEqualsNode(methodNode, classEntry);
            } else {
                visitMethodNode(methodNode, classEntry);
            }
        }
    }

    private void visitToStringNode(MethodNode node, ClassEntry classEntry) {
        InvokeDynamicInsnNode invokeDynamicNode = getDefaultToStringInvokeDynamic(node);
        if (invokeDynamicNode != null) {
            computeFieldNames(classEntry, invokeDynamicNode);
        }
    }

    private void visitHashCodeNode(MethodNode node, ClassEntry classEntry) {
        InvokeDynamicInsnNode invokeDynamicNode = getDefaultHashCodeInvokeDynamic(node);
        if (invokeDynamicNode != null) {
            computeFieldNames(classEntry, invokeDynamicNode);
        }
    }

    private void visitEqualsNode(MethodNode node, ClassEntry classEntry) {
        InvokeDynamicInsnNode invokeDynamicNode = getDefaultEqualsInvokeDynamic(node);
        if (invokeDynamicNode != null) {
            computeFieldNames(classEntry, invokeDynamicNode);
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
            records.computeIfAbsent(classEntry, k -> new RecordComponentData()).add(fieldEntry, unobfuscatedFieldName);
        }
    }

    private void visitMethodNode(MethodNode node, ClassEntry classEntry) {
        // Process default accessor methods
        MethodDescriptor methodDescriptor = new MethodDescriptor(node.desc);
        if (methodDescriptor.getArgumentDescs().size() > 0) {
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
        if (!records.containsKey(classEntry)) {
            return;
        }

        RecordComponentData data = records.get(classEntry);
        if (fieldEntry.getDesc().equals(methodDescriptor.getReturnDesc()) && data.isComponentField(fieldEntry)) {
            MethodEntry methodEntry = new MethodEntry(classEntry, node.name, methodDescriptor);
            data.addAccessorMethod(fieldEntry, methodEntry);
        }
    }

    public boolean isRecord(ClassEntry classEntry) {
        return records.containsKey(classEntry);
    }

    public String getFieldName(ClassEntry parent, FieldEntry field) {
        return records.containsKey(parent) ? records.get(parent).getName(field) : null;
    }

    public String getInitParamName(ClassEntry parent, int lvtIndex) {
        return records.containsKey(parent) ? records.get(parent).getInitParamName(lvtIndex) : null;
    }

    public String getCanonicalConstructorDescriptor(ClassEntry parent) {
        return records.containsKey(parent) ? records.get(parent).getCanonicalConstructorDescriptor() : null;
    }

    public String getAccessorMethodName(ClassEntry parent, MethodEntry method) {
        return records.containsKey(parent) ? records.get(parent).getAccessorMethodName(method) : null;
    }

    protected Map<FieldEntry, String> getAllFieldNames() {
        return records.keySet().stream().flatMap(c -> getFieldNames(c).entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Map<MethodEntry, String> getAllMethodNames() {
        return records.keySet().stream().flatMap(c -> getMethodNames(c).entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Map<FieldEntry, String> getFieldNames(ClassEntry parent) {
        return records.containsKey(parent) ? records.get(parent).fieldNames : null;
    }

    protected Map<MethodEntry, String> getMethodNames(ClassEntry parent) {
        if (!records.containsKey(parent)) {
            return null;
        }

        RecordComponentData data = records.get(parent);
        return data.fieldAccessorMethods.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> data.getName(e.getValue())));
    }

    static class RecordComponentData {
        private final List<String> unobfuscatedFieldNames = new ArrayList<>();
        private final List<FieldEntry> fieldEntries = new ArrayList<>();
        private final Map<FieldEntry, String> fieldNames = new HashMap<>();
        private final Map<FieldEntry, MethodEntry> accessorMethods = new HashMap<>();
        private final Map<MethodEntry, FieldEntry> fieldAccessorMethods = new HashMap<>();

        public void add(FieldEntry fieldEntry, String unobfuscatedFieldName) {
            if (fieldNames.containsKey(fieldEntry)) {
                return;
            }

            unobfuscatedFieldNames.add(unobfuscatedFieldName);
            fieldEntries.add(fieldEntry);
            fieldNames.put(fieldEntry, unobfuscatedFieldName);
        }

        public String getName(FieldEntry fieldEntry) {
            return fieldNames.get(fieldEntry);
        }

        public boolean isComponentField(FieldEntry fieldEntry) {
            return fieldEntries.contains(fieldEntry);
        }

        public String getInitParamName(int lvtIndex) {
            return getName(getFieldByInitLvtIndex(lvtIndex));
        }

        private FieldEntry getFieldByInitLvtIndex(int lvtIndex) {
            int i = 1;
            for (FieldEntry fieldEntry : fieldEntries) {
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
            for (FieldEntry fieldEntry : fieldEntries) {
                sb.append(fieldEntry.getDesc());
            }
            sb.append(")V");
            return sb.toString();
        }

        public void addAccessorMethod(FieldEntry fieldEntry, MethodEntry accessorMethod) {
            if (accessorMethods.containsKey(fieldEntry)) {
                return;
            }
            accessorMethods.put(fieldEntry, accessorMethod);
            fieldAccessorMethods.put(accessorMethod, fieldEntry);
        }

        private FieldEntry getAccessorMethodField(MethodEntry methodEntry) {
            return fieldAccessorMethods.get(methodEntry);
        }

        public String getAccessorMethodName(MethodEntry methodEntry) {
            return fieldNames.get(getAccessorMethodField(methodEntry));
        }

        public boolean hasComponents() {
            return !unobfuscatedFieldNames.isEmpty();
        }
    }
}
