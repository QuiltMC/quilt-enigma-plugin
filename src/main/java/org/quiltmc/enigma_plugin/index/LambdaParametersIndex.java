package org.quiltmc.enigma_plugin.index;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private final Map<ClassEntry, FunctionalMethodLambdas> functionalMethodLambdasByInterface = new HashMap<>();

	public LambdaParametersIndex() {
		super(Arguments.DISABLE_LAMBDA_PARAMS);
	}

	public void forEachFunctionalMethodLambdaImplementing(ClassEntry functionalInterface, BiConsumer<MethodEntry, MethodEntry> action) {
		final FunctionalMethodLambdas functionalMethodLambdas = this.functionalMethodLambdasByInterface.get(functionalInterface);
		if (functionalMethodLambdas != null) {
			for (final MethodEntry lambda : functionalMethodLambdas.lambdas()) {
				action.accept(functionalMethodLambdas.functionalMethod(), lambda);
			}
		}
	}

	public void forEachFunctionalMethodLambda(BiConsumer<MethodEntry, MethodEntry> action) {
		for (final FunctionalMethodLambdas functionalMethodLambdas : this.functionalMethodLambdasByInterface.values()) {
			for (final MethodEntry lambda : functionalMethodLambdas.lambdas()) {
				action.accept(functionalMethodLambdas.functionalMethod(), lambda);
			}
		}
	}

	@Override
	public void visitClassNode(ClassProvider provider, ClassNode node) {
		for (final MethodNode method : node.methods) {
			final List<AbstractInsnNode> instructions = new ArrayList<>();
			method.instructions.forEach(instructions::add);
			for (final AbstractInsnNode instruction : instructions) {
				if (instruction instanceof InvokeDynamicInsnNode invokeDynamic) {
					if (isLambdaMetaFactory(invokeDynamic.bsm)) {
						getReturnType(invokeDynamic.desc)
							.map(provider::get)
							.ifPresent(invokeDynamicReturnType -> {
								getFunctionalMethod(invokeDynamicReturnType).ifPresent(functionalMethod -> {
									// TODO confirm LambdaMetaFactory bsm extra args contain at most one Handle
									Arrays.stream(invokeDynamic.bsmArgs)
										.<Handle>mapMulti((arg, addHandler) -> {
											if (arg instanceof Handle handle) {
												addHandler.accept(handle);
											}
										})
										.findAny()
										.ifPresent(handle -> {
											final ClassNode owner = provider.get(handle.getOwner());
											if (owner != null) {
												owner.methods.stream()
													.filter(handleMethod -> handleMethod.name.equals(handle.getName()))
													.filter(handleMethod -> handleMethod.desc.equals(handle.getDesc()))
													.filter(handleMethod -> matchAccess(handleMethod, ACC_SYNTHETIC))
													.findAny()
													.ifPresent(lambda -> {
														// this.functionalMethodsByLambda.put(
														// 	entryOf(owner, lambda),
														// 	entryOf(invokeDynamicReturnType, functionalMethod)
														// );
														final ClassEntry functionalInterface = new ClassEntry(invokeDynamicReturnType.name);
														this.functionalMethodLambdasByInterface
															.computeIfAbsent(functionalInterface, type -> FunctionalMethodLambdas.of(entryOf(type, functionalMethod)))
															.lambdas()
															.add(entryOf(owner, lambda));
													});
											}
										});
								});
							});
					}
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
		final Pattern pattern = Pattern.compile("\\([^\\)]*\\)L(.+);");
		final Matcher matcher = pattern.matcher(desc);
		if (matcher.find()) {
			return Optional.of(matcher.group(1));
		} else {
			return Optional.empty();
		}
	}

	private static boolean isLambdaMetaFactory(Handle handle) {
		return handle.getOwner().equals("java/lang/invoke/LambdaMetafactory")
				&& (handle.getName().equals("metafactory") || handle.getName().equals("altMetafactory"));
	}

	private record FunctionalMethodLambdas(MethodEntry functionalMethod, List<MethodEntry> lambdas) {
		static FunctionalMethodLambdas of(MethodEntry functionalMethod) {
			return new FunctionalMethodLambdas(functionalMethod, new ArrayList<>());
		}
	}
}
