package com.example.field_names;

public class ClassTest {
	public static final Something FOO = create("foo");
	public static final Something BAR = create("foo/bar");
	public static final Something LOREM_IPSUM = create("baz/lorem_ipsum");
	public static final Something AN_ID = create("example:an_id");
	public static final Something ANOTHER_ID = create("example:foo/another_id");

	private static Something create(String value) {
		return new Something(value);
	}
}
