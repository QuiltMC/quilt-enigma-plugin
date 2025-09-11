package com.example;

import java.util.Random;

public class GetterSetterTestDelegateParamCopy {
	public int x;
	public String y;

	public GetterSetterTestDelegateParamCopy(int x, String y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return this.x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void foo(int a) {
		this.x += new Random().nextInt(a);
	}

	public void foo(String s) {
		System.out.println(s);
		this.y = "foo";
	}

	public String getY() {
		return this.y;
	}

	public void setY(String y) {
		this.y = y;
	}
}
