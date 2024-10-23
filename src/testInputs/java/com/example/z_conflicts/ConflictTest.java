package com.example.z_conflicts;

public class ConflictTest {
	public final int a;
	public final CharSequence b;

	public ConflictTest(int a, CharSequence b) {
		this.a = a;
		this.b = b;
	}

	public ConflictTest(int a, StringBuilder string) {
		this.a = a;
		this.b = string.toString();
	}
}
