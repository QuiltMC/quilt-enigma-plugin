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
import org.quiltmc.enigmaplugin.Arguments;
import org.quiltmc.enigmaplugin.index.enumfields.EnumFieldsIndex;
import org.quiltmc.enigmaplugin.index.simple_type_single.SimpleTypeSingleIndex;

import java.util.List;
import java.util.Set;

public class JarIndexer implements JarIndexerService, Opcodes {
    private final RecordIndex recordIndex = new RecordIndex();
    private final EnumFieldsIndex enumFieldsIndex = new EnumFieldsIndex();
    private final CodecIndex codecIndex = new CodecIndex();
    private final LoggerIndex loggerIndex = new LoggerIndex();
    private final SimpleTypeSingleIndex simpleTypeSingleIndex = new SimpleTypeSingleIndex();
    private boolean disableRecordIndexing = false;
    private boolean disableEnumFieldsIndexing = false;
    private boolean disableCodecsIndexing = false;
    private boolean disableLoggerIndexing = false;

    public JarIndexer withContext(EnigmaServiceContext<JarIndexerService> context) {
        this.disableRecordIndexing = Arguments.isDisabled(context, Arguments.DISABLE_RECORDS);
        this.disableEnumFieldsIndexing = Arguments.isDisabled(context, Arguments.DISABLE_ENUM_FIELDS);
        this.disableCodecsIndexing = Arguments.isDisabled(context, Arguments.DISABLE_CODECS);
        this.disableLoggerIndexing = Arguments.isDisabled(context, Arguments.DISABLE_LOGGER);

        List<String> codecs = context.getArgument(Arguments.CUSTOM_CODECS).map(s -> List.of(s.split(",[\n ]*")))
                .orElse(List.of());
        this.codecIndex.addCustomCodecs(codecs);

        this.simpleTypeSingleIndex.loadRegistry(context.getArgument(Arguments.SIMPLE_TYPE_FIELD_NAMES_PATH).orElse(null));
        return this;
    }

    @Override
    public void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex) {
        for (String className : scope) {
            ClassNode node = classProvider.get(className);
            if (node != null) {
                this.visitClassNode(node);
                this.simpleTypeSingleIndex.visitClassNode(classProvider, node);
            }
        }

        if (!disableEnumFieldsIndexing) {
            enumFieldsIndex.findFieldNames();
        }

        this.simpleTypeSingleIndex.dropCache();
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
            codecIndex.visitClassNode(node);
        }
        if (!this.disableLoggerIndexing) {
            loggerIndex.visitClassNode(node);
        }
    }

    public RecordIndex getRecordIndex() {
        return this.recordIndex;
    }

    public EnumFieldsIndex getEnumFieldsIndex() {
        return this.enumFieldsIndex;
    }

    public CodecIndex getCodecIndex() {
        return this.codecIndex;
    }

    public LoggerIndex getLoggerIndex() {
        return this.loggerIndex;
    }

    public SimpleTypeSingleIndex getSimpleTypeSingleIndex() {
        return this.simpleTypeSingleIndex;
    }
}
