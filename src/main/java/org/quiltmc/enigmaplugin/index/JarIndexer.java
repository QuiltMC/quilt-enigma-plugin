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

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.service.EnigmaServiceContext;
import cuchaz.enigma.api.service.JarIndexerService;
import cuchaz.enigma.classprovider.ClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigmaplugin.index.enumfields.EnumFieldsIndex;

import java.util.Set;

public class JarIndexer implements JarIndexerService, Opcodes {
    public static final String DISABLE_RECORDS_ARG = "disable_records";
    public static final String DISABLE_ENUM_FIELDS_ARG = "disable_enum_fields";
    public static final String DISABLE_CODECS_ARG = "disable_codecs";
    private final RecordIndex recordIndex = new RecordIndex();
    private final EnumFieldsIndex enumFieldsIndex = new EnumFieldsIndex();
    private final CodecIndex codecFieldsIndex = new CodecIndex();
    private boolean disableRecordIndexing = false;
    private boolean disableEnumFieldsIndexing = false;
    private boolean disableCodecsIndexing = false;

    public JarIndexer withContext(EnigmaServiceContext<JarIndexerService> context) {
        disableRecordIndexing = context.getArgument(DISABLE_RECORDS_ARG).map(Boolean::parseBoolean).orElse(false);
        disableEnumFieldsIndexing = context.getArgument(DISABLE_ENUM_FIELDS_ARG).map(Boolean::parseBoolean).orElse(false);
        disableCodecsIndexing = context.getArgument(DISABLE_CODECS_ARG).map(Boolean::parseBoolean).orElse(false);
        return this;
    }

    @Override
    public void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex) {
        for (String className : scope) {
            ClassNode node = classProvider.get(className);
            if (node != null) {
                visitClassNode(node);
            }
        }

        if (!disableEnumFieldsIndexing) {
            enumFieldsIndex.findFieldNames();
        }
    }

    private void visitClassNode(ClassNode node) {
        if (!disableRecordIndexing) {
            if ((node.access & ACC_RECORD) != 0 || node.superName.equals("java/lang/Record")) {
                recordIndex.visitClassNode(node);
            }
        }

        if (!disableEnumFieldsIndexing) {
            enumFieldsIndex.visitClassNode(node);
        }
        if (!disableCodecsIndexing) {
            codecFieldsIndex.visitClassNode(node);
        }
    }

    public RecordIndex getRecordIndex() {
        return this.recordIndex;
    }

    public EnumFieldsIndex getEnumFieldsIndex() {
        return this.enumFieldsIndex;
    }

    public CodecIndex getCodecIndex() {
        return this.codecFieldsIndex;
    }
}
