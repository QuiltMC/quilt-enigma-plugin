package com.example;

public class ConflictTest {
	public static void delegateParamsSource(int integer, Identified identified) { }

	public static void delegateParams(int integer, Identified identified) {
		delegateParamsSource(integer, identified);
	}

	public final int integer;
	public final Identifiable idAble;

	public ConflictTest(int integer, Identifiable idAble) {
		this.integer = integer;
		this.idAble = idAble;
	}

	public ConflictTest(int integer, Identified identified) {
		this.integer = integer;
		this.idAble = identified;
	}
}
