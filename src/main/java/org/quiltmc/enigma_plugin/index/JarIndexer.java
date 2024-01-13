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

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.QuiltEnigmaPlugin;
import org.quiltmc.enigma_plugin.index.constant_fields.ConstantFieldIndex;
import org.quiltmc.enigma_plugin.index.simple_type_single.SimpleTypeSingleIndex;

import java.util.List;
import java.util.Set;

public class JarIndexer implements JarIndexerService, Opcodes {
	private final RecordIndex recordIndex = new RecordIndex();
	private final ConstantFieldIndex constantFieldIndex = new ConstantFieldIndex();
	private final CodecIndex codecIndex = new CodecIndex();
	private final LoggerIndex loggerIndex = new LoggerIndex();
	private final SimpleTypeSingleIndex simpleTypeSingleIndex = new SimpleTypeSingleIndex();
	private final ConstructorParametersIndex constructorParametersIndex = new ConstructorParametersIndex();
	private final GetterSetterIndex getterSetterIndex = new GetterSetterIndex();
	private boolean disableRecordIndexing = false;
	private boolean disableConstantFieldIndexing = false;
	private boolean disableCodecsIndexing = false;
	private boolean disableLoggerIndexing = false;
	private boolean disableConstructorParametersIndexing = false;
	private boolean disableGetterSetterIndexing = false;

	public JarIndexer withContext(EnigmaServiceContext<JarIndexerService> context) {
		this.disableRecordIndexing = Arguments.getBoolean(context, Arguments.DISABLE_RECORDS);
		this.disableConstantFieldIndexing = Arguments.getBoolean(context, Arguments.DISABLE_CONSTANT_FIELDS);
		this.disableCodecsIndexing = Arguments.getBoolean(context, Arguments.DISABLE_CODECS);
		this.disableLoggerIndexing = Arguments.getBoolean(context, Arguments.DISABLE_LOGGER);
		this.disableConstructorParametersIndexing = Arguments.getBoolean(context, Arguments.DISABLE_CONSTRUCTOR_PARAMS);
		this.disableGetterSetterIndexing = Arguments.getBoolean(context, Arguments.DISABLE_GETTER_SETTER);

		List<String> codecs = context.getMultipleArguments(Arguments.CUSTOM_CODECS)
				.orElse(List.of());
		this.codecIndex.addCustomCodecs(codecs);

		this.simpleTypeSingleIndex.loadRegistry(context.getSingleArgument(Arguments.SIMPLE_TYPE_FIELD_NAMES_PATH)
				.map(context::getPath).orElse(null));
		return this;
	}

	@Override
	public void acceptJar(Set<String> scope, ClassProvider classProvider, JarIndex jarIndex) {
		this.constantFieldIndex.clear();

		for (String className : scope) {
			ClassNode node = classProvider.get(className);
			if (node != null) {
				this.visitClassNode(node);
				this.simpleTypeSingleIndex.visitClassNode(classProvider, node);
			}
		}

		if (!this.disableConstantFieldIndexing) {
			this.constantFieldIndex.findFieldNames();
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

		if (!this.disableConstantFieldIndexing) {
			this.constantFieldIndex.visitClassNode(node);
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

	public ConstantFieldIndex getConstantFieldIndex() {
		return this.constantFieldIndex;
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

	@Override
	public String getId() {
		return QuiltEnigmaPlugin.INDEX_SERVICE_ID;
	}
}
