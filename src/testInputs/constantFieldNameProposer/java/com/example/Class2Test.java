package com.example;

public class Class2Test {
	public static final Something FOO = create(ClassTest.FOO);
	public static final Something BAR = create(new Something(ClassTest.BAR));
	public static final Something AN_ID = create(ClassTest.AN_ID);
	public static final Something NORTH = create(new Something(new Something(EnumTest.NORTH)));
	public static final Something EAST = create(create(EnumTest.EAST));

	private static Something create(Something value) {
		return new Something(value);
	}

	private static Something create(EnumTest value) {
		return new Something(value);
	}
}
