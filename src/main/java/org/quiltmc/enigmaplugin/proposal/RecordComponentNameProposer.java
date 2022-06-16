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

package org.quiltmc.enigmaplugin.proposal;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.*;
import org.quiltmc.enigmaplugin.index.RecordIndex;

import java.util.Optional;

public class RecordComponentNameProposer implements NameProposer<Entry<?>> {
    private final RecordIndex index;

    public RecordComponentNameProposer(RecordIndex index) {
        this.index = index;
    }

    @Override
    public Optional<String> doProposeName(Entry<?> entry, EntryRemapper remapper) {
        if (entry instanceof FieldEntry fieldEntry) {
            ClassEntry parent = fieldEntry.getParent();
            return Optional.ofNullable(index.getFieldName(parent, fieldEntry));
        } else if (entry instanceof LocalVariableEntry localVariableEntry) {
            MethodEntry parent = localVariableEntry.getParent();
            if (parent != null) {
                String methodName = parent.getName();
                if (methodName.equals("<init>")) {
                    ClassEntry parentClass = parent.getParent();
                    if (parent.getDesc().toString().equals(index.getCanonicalConstructorDescriptor(parentClass))) {
                        return Optional.ofNullable(index.getInitParamName(parentClass, localVariableEntry.getIndex()));
                    }
                }
            }
        } else if (entry instanceof MethodEntry methodEntry) {
            ClassEntry parent = methodEntry.getParent();
            return Optional.ofNullable(index.getAccessorMethodName(parent, methodEntry));
        }

        return Optional.empty();
    }

    @Override
    public boolean canPropose(Entry<?> entry) {
        ClassEntry classEntry;
        if (entry instanceof FieldEntry fieldEntry) {
            classEntry = fieldEntry.getParent();
        } else if (entry instanceof LocalVariableEntry localVariableEntry) {
            MethodEntry parent = localVariableEntry.getParent();
            if (parent == null) {
                return false;
            }

            classEntry = parent.getParent();
        } else if (entry instanceof MethodEntry methodEntry) {
            classEntry = methodEntry.getParent();
        } else {
            return false;
        }

        return index.isRecord(classEntry);
    }

    @Override
    public Entry<?> upcast(Entry<?> entry) {
        return entry;
    }
}
