package org.quiltmc.enigma_plugin.util;

import static org.quiltmc.enigma_plugin.util.TestUtil.typeDescOf;

public interface CommonDescriptors {
	/**
	 * void
	 */
	String V = "V";

	/**
	 * boolean
	 */
	String Z = "Z";
	/**
	 * byte
	 */
	String B = "B";
	/**
	 * char
	 */
	String C = "C";
	/**
	 * int
	 */
	String I = "I";
	/**
	 * long
	 */
	String J = "J";
	/**
	 * float
	 */
	String F = "F";
	/**
	 * double
	 */
	String D = "D";

	String OBJ = javaLangDescOf("Object");
	String STR = javaLangDescOf("String");

	String VOID = javaLangDescOf("Void");

	String BOOL = javaLangDescOf("Boolean");
	String BYTE = javaLangDescOf("Byte");
	String CHAR = javaLangDescOf("Character");
	String INT = javaLangDescOf("Integer");
	String LONG = javaLangDescOf("Long");
	String FLOAT = javaLangDescOf("Float");
	String DOUBLE = javaLangDescOf("Double");

	static String javaLangDescOf(String simpleName) {
		return typeDescOf("java/lang/" + simpleName);
	}
}
