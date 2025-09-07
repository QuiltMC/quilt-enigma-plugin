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

package org.quiltmc.enigma_plugin.util;

import static org.quiltmc.enigma_plugin.util.TestUtil.javaLangDescOf;

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
}
