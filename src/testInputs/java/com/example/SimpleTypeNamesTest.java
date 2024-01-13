package com.example;

import com.example.simple_type_names.*;

public class SimpleTypeNamesTest {
	public static class Fields {
		public static class Duplicate {
			public final ValueA unnamedValueA = new ValueA();
			public final ValueA unnamedValueA2 = new ValueA();
		}

		public static class Fallbacks {
			public static final Pos POS = new Pos();
			public final Position position = new Position();
			public final RandomPosition randomPosition = new RandomPosition();

			public static final StateA STATIC_STATE_A = new StateA();
			public static final StateB STATIC_STATE_B = new StateB();

			public static final ValueA VALUE_A = new ValueA();
			public static final ValueB VALUE_B = new ValueB();
			public final ValueC valueC = new ValueC();
		}

		public static class Unique {
			public static final Config CONFIG = new Config();
			public static final StateA STATIC_STATE = new StateA();
			public final ValueC value = new ValueC();
		}
	}

	public static class Parameters {
		public static void config(Config config) {
		}

		public void pos(Pos pos) {
		}

		public void pos(Pos pos, Position position) {
		}

		public void pos(Pos pos, RandomPosition position) {
		}

		public void pos(Pos pos, Position position, RandomPosition randomPosition) {
		}

		public void state(StateA state) {
		}

		public static void state(StateA stateA, StateB stateB) {
		}

		public static void value(ValueA valueA, ValueB valueB, ValueC valueC) {
		}

		public static void value(ValueA unnamedValueA, ValueA unnamedValueA2) {
		}
	}
}
