package com.example;

public enum EnumTestDelegateParamCopy {
	NORTH(true),
	EAST(false),
	WEST(false),
	SOUTH(true);

	private final boolean vertical;

	EnumTestDelegateParamCopy(boolean vertical) {
		this.vertical = vertical;
	}
}
