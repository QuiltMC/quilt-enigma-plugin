package com.example;

public enum EnumTestCopy {
	NORTH(true),
	EAST(false),
	WEST(false),
	SOUTH(true);

	private final boolean vertical;

	EnumTestCopy(boolean vertical) {
		this.vertical = vertical;
	}
}
