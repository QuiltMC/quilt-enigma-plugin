package org.quiltmc.enigma_plugin.util;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

public final class EntryUtil {
	private EntryUtil() {
		throw new UnsupportedOperationException();
	}

	public static EntryMapping getMappingOrNonHashed(Entry<?> entry, EntryRemapper remapper, TokenType type, String sourcePluginId) {
		final EntryMapping mapping = remapper.getMapping(entry);
		return mappingOrNonHashed(entry, mapping, type, sourcePluginId);
	}

	public static EntryMapping mappingOrNonHashed(Entry<?> entry, @Nullable EntryMapping mapping, TokenType type, String sourcePluginId) {
		if (mapping == null) {
			return EntryMapping.OBFUSCATED;
		} else if (mapping.targetName() == null) {
			String name = getNonHashedNameOrNull(entry);
			return name == null ? mapping : new EntryMapping(name, null, type, sourcePluginId);
		} else {
			return mapping;
		}
	}

	@Nullable
	public static String getNonHashedNameOrNull(Entry<?> entry) {
		String name = entry.getName();
		if (entry instanceof FieldEntry) {
			return name.startsWith("f_") ? null : name;
		} else if (entry instanceof MethodEntry method) {
			return name.startsWith("m_") || method.isConstructor() ? null : name;
		} else if (entry instanceof ClassEntry clazz) {
			return clazz.getSimpleName().startsWith("C_") ? null : getClassName(clazz);
		} else {
			return null;
		}
	}

	private static String getClassName(ClassEntry clazz) {
		if (clazz.getParent() == null) {
			return clazz.getFullName();
		} else {
			return clazz.getSimpleName();
		}
	}
}
