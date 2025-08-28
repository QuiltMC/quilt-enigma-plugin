package com.example;

@FunctionalInterface
public interface ZFunction {
	ZFunction SIMPLE = food -> {};

	void eat(String food);

	static ZFunction ofInline(boolean val) {
		return food -> { boolean b = true; };
	}

	static ZFunction ofLocal(int val) {
		final ZFunction simple = food -> { int i = 0; };
		return simple;
	}

	static ZFunction ofInlineReference(long val) {
		return SIMPLE::eat;
	}

	static ZFunction ofLocalReference(float val) {
		final ZFunction reference = SIMPLE::eat;
		return reference;
	}

	static ZFunction ofArg(ZFunction function) {
		int i = 0;
		return function;
	}

	static ZFunction ofOfArg(char c) {
		return ofArg(food -> { String string = "food"; });
	}

	static ZFunction ofArgAnd(char c1, ZFunction function, char c2) {
		return function;
	}

	static ZFunction ofOfArgAnd(byte b) {
		return ofArgAnd('c', food -> { char c = 'c'; }, 'c');
	}

	static ZFunction ofExtras(boolean[] bs) {
		boolean b = false;
		int i = 0;
		return food -> { int j = b ? i : 1; };
	}
}
