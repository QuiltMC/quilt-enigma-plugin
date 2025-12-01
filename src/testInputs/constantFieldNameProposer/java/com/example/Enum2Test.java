package com.example;

import java.util.Random;

public enum Enum2Test {
	CONSTANT,
	INT {
		@Override
		public double randomValue(Random random) {
			return random.nextInt();
		}
	},
	DOUBLE {
		@Override
		public double randomValue(Random random) {
			return random.nextDouble();
		}
	},
	EXPONENTIAL {
		@Override
		public double randomValue(Random random) {
			return random.nextExponential();
		}
	},
	GAUSSIAN {
		@Override
		public double randomValue(Random random) {
			return random.nextGaussian();
		}
	};

	public double randomValue(Random random) {
		return 0;
	}
}
