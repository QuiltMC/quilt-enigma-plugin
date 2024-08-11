package com.example;

import com.example.simple_type_names.Config;
import com.example.simple_type_names.Pos;
import com.example.simple_type_names.Position;
import com.example.simple_type_names.RandomPosition;
import com.example.simple_type_names.StateA;
import com.example.simple_type_names.StateB;
import com.example.simple_type_names.ValueA;
import com.example.simple_type_names.ValueB;
import com.example.simple_type_names.ValueC;
import com.example.simple_type_names.ValueD;
import com.example.simple_type_names.ValueDD;
import com.example.simple_type_names.ValueE;
import com.example.simple_type_names.ValueEE;

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

		public static class Inheritance {
			public final ValueDD valueD = new ValueDD();
			public final ValueEE valueE = new ValueEE();
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

		public static void value(ValueD valueD) {
		}

		public static void value(ValueDD valueD) {
		}

		public static void value(ValueE valueD) {
		}

		public static void value(ValueEE valueD) {
		}
	}
}
