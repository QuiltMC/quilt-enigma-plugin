package com.example;

import java.util.Random;

public class DelegateParametersTest {
	public record Test1(int val, long j, int index, String s) {
		public Test1() {
			this(-1);
		}

		public Test1(int val) {
			this(val, -1);
		}

		public Test1(int val, long j) {
			this(val, j, 0);
		}

		public Test1(int val, long j, int index) {
			this(val, j, index, "");
		}

		public static Test1 create(int val) {
			return create(val, -1);
		}

		public static Test1 create(int val, long j) {
			return new Test1(val, j);
		}
	}

	public void foo(int i) {
		this.foo((long) i);
	}

	public int foo(long l) {
		var r = new Random(l);
		return (int) r.nextGaussian();
	}

	public void bar(long l) {
		this.bar(1 + this.foo(l));
	}

	public void bar(int val) {
		if (new Random(-1).nextGaussian() < 0.5) {
			new Test1(val);
		} else {
			this.foo(val);
		}
	}
}
