package org.quiltmc.enigma_plugin.index;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.mapping.IndexEntryResolver;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.util.AsmUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DelegatingMethodsIndex extends Index {
	private final Map<MethodEntry, List<MethodEntry>> delegatersByDelegate = new HashMap<>();

	private final GetterSetterIndex getterSetterIndex;
	// includes methods that return constants or static fields
	private final Map<MethodNode, Boolean> getterCache = new HashMap<>();
	private EntryIndex entryIndex;

	public DelegatingMethodsIndex(GetterSetterIndex getterSetterIndex) {
		super(Arguments.DISABLE_DELEGATING_METHODS);
		this.getterSetterIndex = getterSetterIndex;
	}

	@Override
	public void setIndexingContext(Set<String> classes, JarIndex jarIndex) {
		this.entryIndex = jarIndex.getIndex(EntryIndex.class);
	}

	@Override
	public void visitClassNode(ClassProvider classProvider, ClassNode clazz) {
		// DEBUG TODO
		if (clazz.name.equals("com/a/f")) {
			final ClassEntry classEntry = new ClassEntry(clazz.name);
			for (final MethodNode method : clazz.methods) {
				this.getDelegation(clazz, classEntry, method).ifPresent(delegation -> this.delegatersByDelegate
						.computeIfAbsent(delegation.delegate, ignored -> new ArrayList<>())
						.add(delegation.delegater)
				);
			}
			// DEBUG TODO
			int i = 0;
			byte b = 0;
		}
	}

	private Optional<Delegation> getDelegation(ClassNode clazz, ClassEntry classEntry, MethodNode method) {
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

		final MethodEntry delegaterEntry = entryOf(classEntry, method);
		final MethodEntry delegateEntry = entryOf(classEntry, delegate);

		final List<LocalVariableEntry> delegaterParams = delegaterEntry.getParameters(this.entryIndex);
		final List<LocalVariableEntry> delegateParams = delegateEntry.getParameters(this.entryIndex);

		if (delegaterParams.size() > delegateParams.size() || (delegaterParams.isEmpty() && !delegateParams.isEmpty())) {
			return Optional.empty();
		}

		if (!delegateEntry.getDesc().getReturnDesc().equals(delegaterEntry.getDesc().getReturnDesc())) {
			return Optional.empty();
		}

		if (delegateEntry.canConflictWith(delegaterEntry)) {
			return Optional.empty();
		}

		final int lastDelegateParamIndex = delegateParams.get(delegateParams.size() - 1).getIndex();

		// DEBUG TODO
		final List<AbstractInsnNode> instructions = new ArrayList<>();
		method.instructions.forEach(instructions::add);

		AbstractInsnNode prevInstruction = lastCall.getPrevious();
		while (prevInstruction != null) {
			if (prevInstruction instanceof VarInsnNode var) {
				final int varOp = var.getOpcode();
				if (varOp >= ILOAD && varOp <= ALOAD) {
					if (var.var >= lastDelegateParamIndex) {
						// TODO
						return Optional.empty();
					}
					// else loading param to pass to finalCallMethod: OK
				} else {
					// TODO
					return Optional.empty();
				}
			} else {
				final int prevOp = prevInstruction.getOpcode();
				if (prevOp < I2L || prevOp > I2S) {
					// TODO
					return Optional.empty();
				}
				// else primitives cast: OK
			}

			prevInstruction = prevInstruction.getPrevious();
		}
		// DEBUG TODO
		instructions.get(0);

		return Optional.of(new Delegation(delegateEntry, delegaterEntry));
	}

	private static MethodEntry entryOf(ClassEntry parent, MethodNode node) {
		return new MethodEntry(parent, node.name, new MethodDescriptor(node.desc));
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
			if ((firstOp >= ACONST_NULL && firstOp <= LDC) || firstOp == GETSTATIC) {
				// loading static field or constant
				final int secondOp = method.instructions.getLast().getOpcode();
				// returning value
				return secondOp >= IRETURN && secondOp <= ARETURN;
			}
		}

		return false;
	}

	private static int getParamCount(MethodNode method) {
		return method.parameters == null ? 0 : method.parameters.size();
	}

	private record Delegation(MethodEntry delegate, MethodEntry delegater) { }
}
