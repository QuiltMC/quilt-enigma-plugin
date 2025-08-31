package org.quiltmc.enigma_plugin.index;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.util.AsmUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DelegatingMethodsIndex extends Index {
	private final Map<MethodEntry, List<MethodEntry>> delegatersByDelegate = new HashMap<>();

	private final GetterSetterIndex getterSetterIndex;
	// includes methods that return constants or static fields
	private final Map<MethodNode, Boolean> getterCache = new HashMap<>();

	public DelegatingMethodsIndex(GetterSetterIndex getterSetterIndex) {
		super(Arguments.DISABLE_DELEGATING_METHODS);
		this.getterSetterIndex = getterSetterIndex;
	}

	@Override
	public void visitClassNode(ClassProvider classProvider, ClassNode clazz) {
		// DEBUG TODO
		if (clazz.name.equals("com/a/f")) {
			final ClassEntry classEntry = new ClassEntry(clazz.name);
			for (final MethodNode method : clazz.methods) {
				this.getDelegate(clazz, method).ifPresent(delegate -> this.delegatersByDelegate
						.computeIfAbsent(entryOf(classEntry, delegate), ignored -> new ArrayList<>())
						.add(entryOf(classEntry, method))
				);
			}
		}
	}

	private Optional<MethodNode> getDelegate(ClassNode clazz, MethodNode method) {
		if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
			return Optional.empty();
		}

		final int lastOp = method.instructions.getLast().getOpcode();
		if (lastOp < IRETURN || lastOp > RETURN) {
			return Optional.empty();
		}

		if (!(method.instructions.getLast().getPrevious() instanceof MethodInsnNode lastCall)) {
			return Optional.empty();
		}

		final MethodNode delegate = AsmUtil.getMethod(clazz, lastCall.name, lastCall.desc).orElse(null);
		if (delegate == null) {
			return Optional.empty();
		}

		final int delegaterParamCount = getParamCount(method);
		final int delegateParamCount = getParamCount(delegate);
		if (delegaterParamCount > delegateParamCount || (delegaterParamCount == 0 && delegateParamCount > 0)) {
			return Optional.empty();
		}

		if (!haveDelegationCompatibleDescriptors(method, delegate)) {
			return Optional.empty();
		}

		// DEBUG TODO
		final List<AbstractInsnNode> instructions = new ArrayList<>();
		method.instructions.forEach(instructions::add);

		AbstractInsnNode prevInstruction = lastCall.getPrevious();
		while (prevInstruction != null) {
			if (prevInstruction instanceof VarInsnNode var) {
				final int varOp = var.getOpcode();
				if (varOp >= ILOAD && varOp <= ALOAD) {
					if (var.var >= delegaterParamCount) {
						// TODO
						return Optional.empty();
					}
					// else loading param to pass to finalCallMethod: OK
				} else {
					// TODO
					return Optional.empty();
				}
			} else {
				// TODO
				return Optional.empty();
			}

			prevInstruction = prevInstruction.getPrevious();
		}
		// DEBUG TODO
		instructions.get(0);

		return Optional.of(delegate);
	}

	private static MethodEntry entryOf(ClassEntry parent, MethodNode node) {
		return new MethodEntry(parent, node.name, new MethodDescriptor(node.desc));
	}

	private static boolean haveDelegationCompatibleDescriptors(MethodNode method1, MethodNode method2) {
		final int returnStart1 = method1.desc.lastIndexOf(')');
		final int returnStart2 = method2.desc.lastIndexOf(')');

		// same return, different params
		return method1.desc.substring(returnStart1).equals(method2.desc.substring(returnStart2))
				&& !method1.desc.substring(0, returnStart1).equals(method2.desc.substring(0, returnStart2));
	}

	private static String getReturnDesc(MethodNode method) {
		return method.desc.substring(method.desc.lastIndexOf(')'));
	}

	private boolean isGetter(MethodNode method) {
		return this.getterCache.computeIfAbsent(method, m -> {
			// this.getterSetterIndex.getLinkedField(method) != null ||
			return isNonInstanceGetter(m);
		});
	}

	private static boolean isNonInstanceGetter(MethodNode method) {
		if (method.instructions.size() == 2 && getParamCount(method) == 0) {
			final int firstOp = method.instructions.getFirst().getOpcode();
			if ((firstOp >= ACONST_NULL && firstOp <= LDC) || firstOp == GETSTATIC || firstOp == GETFIELD) {
				// loading static field or constant
				final int secondOp = method.instructions.getLast().getOpcode();
				// returning constant
				return secondOp >= IRETURN && secondOp <= ARETURN;
			}
		}

		return false;
	}

	private static int getParamCount(MethodNode method) {
		return method.parameters == null ? 0 : method.parameters.size();
	}
}
