package com.example;

import java.util.Random;

public class DelegateParametersTest {
	record Test1(int val, int index, long j, String s) {
		public Test1() {
			this(-1);
		}

		public Test1(int val) {
			this(val, -1);
		}

		public Test1(int val, int index) {
			this(val, index, 0);
		}

		public Test1(int val, int index, long j) {
			this(val, index, j, "");
		}
	}

	public void foo(int i) {
		foo((long) i);
	}

	public int foo(long l) {
		var r = new Random(l);
		return (int) r.nextGaussian();
	}

	public void bar(long l) {
		bar(1 + foo(l));
	}

	public void bar(int val) {
		if (new Random(-1).nextGaussian() < 0.5) {
			new Test1(val);
		} else {
			foo(val);
		}
	}
}
