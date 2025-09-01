package org.quiltmc.enigma_plugin.index;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma_plugin.Arguments;
import org.quiltmc.enigma_plugin.util.AsmUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class DelegatingMethodIndex extends Index {
	private final Map<MethodEntry, List<MethodEntry>> delegatersByDelegate = new HashMap<>();

	private final GetterSetterIndex getterSetterIndex;
	// includes methods that return constants or static fields
	private final Map<MethodNode, Boolean> getterCache = new HashMap<>();
	private EntryIndex entryIndex;

	public DelegatingMethodIndex(GetterSetterIndex getterSetterIndex) {
		super(Arguments.DISABLE_DELEGATING_METHODS);
		this.getterSetterIndex = getterSetterIndex;
	}

	public Stream<MethodEntry> streamDelegaters(MethodEntry method) {
		return this.delegatersByDelegate.getOrDefault(method, List.of()).stream();
	}

	public void forEachDelegation(BiConsumer<MethodEntry, Stream<MethodEntry>> action) {
		this.delegatersByDelegate.forEach((delegate, delegaters) -> action.accept(delegate, delegaters.stream()));
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
			final Map<MethodEntry, Set<List<TypeDescriptor>>> conflictingDelegaterParamDescriptorsByDelegate = new HashMap<>();
			for (final MethodNode method : clazz.methods) {
				this.getDelegation(clazz, classEntry, method).ifPresent(delegation -> {
					final List<MethodEntry> delegaters = this.delegatersByDelegate.get(delegation.delegate);
					if (delegaters != null) {
						if (delegaters.removeIf(delegation.delegater::canConflictWith)) {
							// new conflict
							conflictingDelegaterParamDescriptorsByDelegate
									.computeIfAbsent(delegation.delegate, ignored -> new HashSet<>())
									.add(delegation.delegater.getDesc().getTypeDescs());
						} else {
							final Set<List<TypeDescriptor>> conflictParamsDescriptors =
									conflictingDelegaterParamDescriptorsByDelegate.getOrDefault(delegation.delegate, Set.of());
							if (!conflictParamsDescriptors.contains(delegation.delegater.getDesc().getTypeDescs())) {
								delegaters.add(delegation.delegater);
							}
							// else additional conflict
						}
					} else {
						final List<MethodEntry> newDelegaters = new ArrayList<>();
						newDelegaters.add(delegation.delegater);
						this.delegatersByDelegate.put(delegation.delegate, newDelegaters);
					}
				});
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
		if (method.name.equals("a") && method.desc.equals("(IJ)Ljava/lang/String;")) {
			int i = 0;
		}

		if (method.name.equals("e") && method.desc.equals("(Ljava/lang/Object;I)F")) {
			int i = 0;
		}

		AbstractInsnNode prevInstruction = lastCall.getPrevious();
		while (prevInstruction != null) {
			final int prevOp = prevInstruction.getOpcode();
			// all loading: OK
			if (prevOp < ILOAD || prevOp > SALOAD) {
				if (prevOp >= ISTORE && prevOp <= SASTORE) {
					if (
						// do not allow storing to any array
						prevOp > ASTORE
							// only allow storing to locals
							|| !(prevInstruction instanceof VarInsnNode)
					) {
						return Optional.empty();
					}
					// else storing local: OK
				} else if (prevInstruction instanceof MethodInsnNode call) {
					// TODO allow getters
					if (!(isPrimitiveBoxOrUnbox(call))) {
						return Optional.empty();
					}
				} else {
					if (!(
						isConstantLoad(prevOp)
							|| prevOp == GETSTATIC
							|| prevOp == GETFIELD
							// primitive cast
							|| (prevOp >= I2L && prevOp <= I2S)
					)) {
						// TODO
						return Optional.empty();
					}
				}
			}

			prevInstruction = prevInstruction.getPrevious();
		}
		// DEBUG TODO
		instructions.get(0);

		return Optional.of(new Delegation(delegateEntry, delegaterEntry));
	}

	private static boolean isPrimitiveBoxOrUnbox(MethodInsnNode call) {
		return switch (call.owner) {
			case "java/lang/Boolean" -> isBoxerOr(call, 'Z', "booleanValue");
			case "java/lang/Byte" -> isBoxerOr(call, 'B', "byteValue");
			case "java/lang/Character" -> isBoxerOr(call, 'C', "charValue");
			case "java/lang/Integer" -> isBoxerOr(call, 'I', "intValue");
			case "java/lang/Long" -> isBoxerOr(call, 'J', "longValue");
			case "java/lang/Float" -> isBoxerOr(call, 'F', "floatValue");
			case "java/lang/Double" -> isBoxerOr(call, 'D', "doubleValue");
			default -> false;
		};
	}

	private static boolean isBoxerOr(MethodInsnNode call, char primitiveDesc, String unboxer) {
		return (call.name.equals("valueOf") && call.desc.startsWith("(" + primitiveDesc + ")"))
				|| (call.name.equals(unboxer) && call.desc.startsWith("()"));
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
			if (isConstantLoad(firstOp) || firstOp == GETSTATIC) {
				// loading static field or constant
				final int secondOp = method.instructions.getLast().getOpcode();
				// returning value
				return secondOp >= IRETURN && secondOp <= ARETURN;
			}
		}

		return false;
	}

	private static boolean isConstantLoad(int opcode) {
		return opcode >= ACONST_NULL && opcode <= LDC;
	}

	private static int getParamCount(MethodNode method) {
		return method.parameters == null ? 0 : method.parameters.size();
	}

	record Delegation(MethodEntry delegate, MethodEntry delegater) { }
}
