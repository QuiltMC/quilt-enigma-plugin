package com.example;

public class Class3Test {
	public static final Something FOO = create(Class2Test.FOO);
	public static final Something BAR = create(new Something(ClassTest.BAR));

	private static Something create(Something value) {
		return new Something(value);
	}
}
