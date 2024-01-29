package com.example;

import java.util.Optional;

public record RecordNamingTest(int value, double scale, Optional<String> s) {
	public static final RecordNamingTest INSTANCE = new RecordNamingTest(0, 0.0, Optional.empty());

	public record Inner1(double factor, double scale, int value) {
		@Override
		public double scale() {
			return this.factor;
		}

		@Override
		public double factor() {
			return this.factor * this.scale;
		}

		// Only value should be automapped
	}
}
