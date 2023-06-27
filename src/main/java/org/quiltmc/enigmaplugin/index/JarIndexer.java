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
	private final ConstructorParametersIndex constructorParametersIndex = new ConstructorParametersIndex();
	private final GetterSetterIndex getterSetterIndex = new GetterSetterIndex();
	private boolean disableRecordIndexing = false;
	private boolean disableEnumFieldsIndexing = false;
	private boolean disableCodecsIndexing = false;
	private boolean disableLoggerIndexing = false;
	private boolean disableConstructorParametersIndexing = false;
	private boolean disableGetterSetterIndexing = false;

	public JarIndexer withContext(EnigmaServiceContext<JarIndexerService> context) {
		this.disableRecordIndexing = Arguments.isDisabled(context, Arguments.DISABLE_RECORDS);
		this.disableEnumFieldsIndexing = Arguments.isDisabled(context, Arguments.DISABLE_ENUM_FIELDS);
		this.disableCodecsIndexing = Arguments.isDisabled(context, Arguments.DISABLE_CODECS);
		this.disableLoggerIndexing = Arguments.isDisabled(context, Arguments.DISABLE_LOGGER);
		this.disableConstructorParametersIndexing = Arguments.isDisabled(context, Arguments.DISABLE_CONSTRUCTOR_PARAMS);
		this.disableGetterSetterIndexing = Arguments.isDisabled(context, Arguments.DISABLE_GETTER_SETTER);

		List<String> codecs = context.getArgument(Arguments.CUSTOM_CODECS).map(s -> List.of(s.split(",[\n ]*")))
				.orElse(List.of());
		this.codecIndex.addCustomCodecs(codecs);

		this.simpleTypeSingleIndex.loadRegistry(context.getArgument(Arguments.SIMPLE_TYPE_FIELD_NAMES_PATH)
				.map(context::getPath).orElse(null));
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

		if (!this.disableEnumFieldsIndexing) {
			this.enumFieldsIndex.findFieldNames();
		}

		this.simpleTypeSingleIndex.dropCache();
	}

	private void visitClassNode(ClassNode node) {
		boolean isRecord = (node.access & ACC_RECORD) != 0 || node.superName.equals("java/lang/Record");

		if (!this.disableRecordIndexing) {
			if (isRecord) {
				this.recordIndex.visitClassNode(node);
			}
		}

		if (!this.disableEnumFieldsIndexing) {
			this.enumFieldsIndex.visitClassNode(node);
		}
		if (!this.disableCodecsIndexing) {
			this.codecIndex.visitClassNode(node);
		}
		if (!this.disableLoggerIndexing) {
			this.loggerIndex.visitClassNode(node);
		}
		if (!this.disableConstructorParametersIndexing) {
			this.constructorParametersIndex.visitClassNode(node);
		}
		if (!this.disableGetterSetterIndexing) {
			this.getterSetterIndex.visitClassNode(node);
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

	public ConstructorParametersIndex getConstructorParametersIndex() {
		return this.constructorParametersIndex;
	}

	public GetterSetterIndex getGetterSetterIndex() {
		return this.getterSetterIndex;
	}
}
