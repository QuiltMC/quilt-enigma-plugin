/*
 * Copyright 2025 QuiltMC
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

import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.representation.ArgumentDescriptor;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.quiltmc.enigma_plugin.util.AsmUtil.matchAccess;

public class LambdaParametersIndex extends Index {
	// param descriptors of non-final java.lang.Object instance methods
	private static final Map<String, String> NON_FUNCTIONAL_METHOD_PARAMS_DESCRIPTORS = Map.of(
			"hashCode", "()",
			"equals", "(Ljava/lang/Object;)",
			"clone", "()",
			"toString", "()",
			"finalize", "()"
	);

	private static final String RETURN_TYPE_GROUP = "returnType";
	private static final Pattern RETURN_TYPE_PATTERN = Pattern.compile("\\([^\\)]*\\)L(?<" + RETURN_TYPE_GROUP + ">.+);");

	private final Map<LocalVariableEntry, List<LocalVariableEntry>> lambdaParamsByFunctionalParam = new HashMap<>();
	private final Map<MethodNode, List<LocalVariableEntry>> functionalMethodParams = new HashMap<>();

	public LambdaParametersIndex() {
		super(Arguments.DISABLE_LAMBDA_PARAMS);
	}

	public Stream<LocalVariableEntry> streamLambdaParams(LocalVariableEntry functionalParam) {
		return this.lambdaParamsByFunctionalParam.getOrDefault(functionalParam, List.of()).stream();
	}

	public void forEachFunctionalParam(BiConsumer<LocalVariableEntry, Stream<LocalVariableEntry>> action) {
		this.lambdaParamsByFunctionalParam.forEach((functionalParam, lambdaParams) -> {
			action.accept(functionalParam, lambdaParams.stream());
		});
	}

	@Override
	public void visitClassNode(ClassProvider provider, ClassNode node) {
		for (final MethodNode method : node.methods) {
			final List<AbstractInsnNode> instructions = new ArrayList<>();
			method.instructions.forEach(instructions::add);
			for (final AbstractInsnNode instruction : instructions) {
				if (instruction instanceof InvokeDynamicInsnNode invokeDynamic) {
					if (!isLambdaMetaFactory(invokeDynamic.bsm)) {
						continue;
					}

					getReturnType(invokeDynamic.desc).map(provider::get).ifPresent(invokeDynamicReturnType -> {
						getFunctionalMethod(invokeDynamicReturnType).ifPresent(functionalMethod -> Arrays
								.stream(invokeDynamic.bsmArgs)
								.flatMap(arg -> arg instanceof Handle handle ? Stream.of(handle) : Stream.empty())
								// TODO confirm LambdaMetaFactory bsm extra args contain at most one Handle
								.findAny()
								.ifPresent(handle -> {
									final ClassNode owner = provider.get(handle.getOwner());
									if (owner != null) {
										owner.methods.stream()
												.filter(handleMethod -> handleMethod.name.equals(handle.getName()))
												.filter(handleMethod -> handleMethod.desc.equals(handle.getDesc()))
												.filter(handleMethod -> matchAccess(handleMethod, ACC_PRIVATE, ACC_SYNTHETIC))
												.findAny()
												.ifPresent(lambda -> {
													final List<LocalVariableEntry> functionalParams = this
															.functionalMethodParams
															.computeIfAbsent(functionalMethod, funcMethod ->
																	createParamEntries(invokeDynamicReturnType, funcMethod)
															);

													final List<LocalVariableEntry> lambdaParams =
															createParamEntries(owner, lambda);

													final int lambdaParamOffset =
															lambdaParams.size() - functionalParams.size();
													assert lambdaParamOffset >= 0;

													for (int i = 0; i < functionalParams.size(); i++) {
														this.lambdaParamsByFunctionalParam
																.computeIfAbsent(
																	functionalParams.get(i),
																	ignored -> new ArrayList<>()
																)
																.add(lambdaParams.get(i + lambdaParamOffset));
													}
												});
									}
								})
						);
					});
				}
			}
		}
	}

	private static MethodEntry entryOf(ClassNode parent, MethodNode node) {
		return entryOf(new ClassEntry(parent.name), node);
	}

	private static MethodEntry entryOf(ClassEntry parent, MethodNode node) {
		return new MethodEntry(parent, node.name, new MethodDescriptor(node.desc));
	}

	@Unmodifiable
	private static List<LocalVariableEntry> createParamEntries(ClassNode parent, MethodNode funcMethod) {
		final List<LocalVariableEntry> params = new ArrayList<>();
		final MethodEntry parentEntry = entryOf(parent, funcMethod);

		// TODO unnecessary? always static?
		int i = matchAccess(funcMethod, ACC_STATIC) ? 0 : 1;
		for (final ArgumentDescriptor paramDesc : parentEntry.getDesc().getArgumentDescs()) {
			params.add(new LocalVariableEntry(parentEntry, i));
			i += paramDesc.getSize();
		}

		return Collections.unmodifiableList(params);
	}

	private static boolean isFunctionalInterface(ClassNode clazz) {
		return clazz.visibleAnnotations != null && clazz.visibleAnnotations.stream()
				.map(annotation -> annotation.desc)
				.anyMatch(desc -> desc.equals("Ljava/lang/FunctionalInterface;"));
	}

	private static Optional<MethodNode> getFunctionalMethod(ClassNode clazz) {
		if (isFunctionalInterface(clazz)) {
			return clazz.methods.stream()
					.filter(method -> matchAccess(method, Opcodes.ACC_ABSTRACT))
					.filter(method -> {
						final String paramsDesc = NON_FUNCTIONAL_METHOD_PARAMS_DESCRIPTORS.get(method.name);
						return paramsDesc == null || !method.desc.startsWith(paramsDesc);
					})
					.findAny();
		} else {
			return Optional.empty();
		}
	}

	private static Optional<String> getReturnType(String desc) {
		final Matcher matcher = RETURN_TYPE_PATTERN.matcher(desc);
		if (matcher.find()) {
			return Optional.of(matcher.group(RETURN_TYPE_GROUP));
		} else {
			return Optional.empty();
		}
	}

	private static boolean isLambdaMetaFactory(Handle handle) {
		return handle.getOwner().equals("java/lang/invoke/LambdaMetafactory")
				&& (handle.getName().equals("metafactory") || handle.getName().equals("altMetafactory"));
	}
}
