package com.example;

public class ClassTest {
	public static final Something FOO = create("foo");
	public static final Something BAR = create("foo/bar");
	public static final Something BAZ = create(create(create("foo/baz")));
	public static final Something LOREM_IPSUM = create("baz/lorem_ipsum");
	public static final Something AN_ID = create("example:an_id");
	public static final Something ANOTHER_ID = create("example:foo/another_id");
	public static final Something ONE = create(new Something("One"));
	public static final Something TWO = create(new Something(new Something("Two")));
	public static final Something THREE = create(new Something(new Something(new Something("Three"))));

	private static Something create(String value) {
		return new Something(value);
	}

	private static Something create(Something value) {
		return new Something(value);
	}
}
