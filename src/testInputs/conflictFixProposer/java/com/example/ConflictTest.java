package com.example;

public class ConflictTest {
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
