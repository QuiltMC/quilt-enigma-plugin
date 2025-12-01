package com.example;

@FunctionalInterface
public interface LambdaParametersTest {
	LambdaParametersTest SIMPLE = food -> {};

	void eat(String food);

	static LambdaParametersTest ofInline(boolean val) {
		return food -> {
			boolean b = true;
		};
	}

	static LambdaParametersTest ofLocal(int val) {
		final LambdaParametersTest simple = food -> {
			int i = 0;
		};
		return simple;
	}

	static LambdaParametersTest ofInlineReference(long val) {
		return SIMPLE::eat;
	}

	static LambdaParametersTest ofLocalReference(float val) {
		final LambdaParametersTest reference = SIMPLE::eat;
		return reference;
	}

	static LambdaParametersTest ofArg(LambdaParametersTest function) {
		int i = 0;
		return function;
	}

	static LambdaParametersTest ofOfArg(char c) {
		return ofArg(food -> {
			String string = "food";
		});
	}

	static LambdaParametersTest ofArgAnd(char c1, LambdaParametersTest function, char c2) {
		return function;
	}

	static LambdaParametersTest ofOfArgAnd(byte b) {
		return ofArgAnd(
				'c',
				food -> {
					char c = 'c';
				},
				'c'
		);
	}

	static LambdaParametersTest ofExtras(boolean[] bs) {
		boolean b = false;
		int i = 0;
		return food -> {
			int j = b ? i : 1;
		};
	}
}
