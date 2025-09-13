package com.example;

public enum EnumTest {
	NORTH(true),
	EAST(false),
	WEST(false),
	SOUTH(true);

	private final boolean vertical;

	EnumTest(boolean vertical) {
		this.vertical = vertical;
	}
}
