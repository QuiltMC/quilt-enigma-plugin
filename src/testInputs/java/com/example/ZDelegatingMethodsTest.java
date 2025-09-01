package com.example;

public class ZDelegatingMethodsTest {
	static Object O = new Object();
	static byte B = 20;

	int I = 0;
	long[] LONGS = new long[0];

	Integer I_BOXED = 100;

	int getI() {
		return this.I;
	}

	long getLiteralL1() {
		return 1;
	}

	long getLiteralL100() {
		return 100;
	}

	String getLiteralS() {
		return "s";
	}

	Object getStaticO() {
		return O;
	}

	int getStaticB() {
		return B;
	}

	static Object staticRoot(ZDelegatingMethodsTest instance, Object o, int i, long l) {
		return null;
	}

	static Object staticDelegating(ZDelegatingMethodsTest instance, int i, long l) {
		return staticRoot(instance, O, i, l);
	}

	Object instanceDelegating(Object o, long l) {
		return staticRoot(this, o, 30, l);
	}

	void voidRoot(Object o, int i, long l) { }

	void voidDelegating(Object o, int i) {
		this.voidRoot(o, i, 0);
	}

	String stringRoot(Object o, int i, long l) {
		return "root";
	}

	String delegatingReusedParam(Object o, int i) {
		return this.stringRoot(o, i, i);
	}

	String delegatingInlineLiteral(Object o, long l) {
		return this.stringRoot(o, 2, l);
	}

	String delegatingMixed1(Object o) {
		long l = this.getLiteralL1();
		return this.stringRoot(o, this.I, l);
	}

	String delegatingMixed2(long l) {
		return this.stringRoot(O, this.getI(), l);
	}

	String delegatingMixed3(int i) {
		return this.stringRoot(this.getLiteralS(), i, this.I);
	}

	Enum<?> enumRoot(Object o, int i, long l) {
		return null;
	}

	Enum<?> delegatingLocalLiteral(Object o, int i) {
		long l = 3;
		return this.enumRoot(o, i, l);
	}

	Enum<?> delegatingFinalLocalLiteral(Object o, long l) {
		final int i = 4;
		return this.enumRoot(o, i, l);
	}

	Enum<?> delegatingInlineStaticField(int i, long l) {
		return this.enumRoot(O, i, l);
	}

	int intRoot(Object o, int i, long l) {
		return 1000;
	}

	int delegatingLocalStaticField(int i, long l) {
		Object o = O;
		return this.intRoot(o, i, l);
	}

	int delegatingInlineField(Object o, long l) {
		return this.intRoot(o, this.I, l);
	}

	int delegatingLocalField(Object o, float f) {
		int i = this.I;
		return this.intRoot(o, i, (long) f);
	}

	int delegatingInlineGetter(Object o, byte b) {
		return this.intRoot(o, this.getI(), b);
	}

	char charRoot(Object o, int i, long l) {
		return 'c';
	}

	char delegatingLocalGetter(Object o, long l) {
		int i = this.getI();
		return this.charRoot(o, i, l);
	}

	char delegatingInlineLiteralGetter(Object o, int i) {
		return this.charRoot(o, i, this.getLiteralL1());
	}

	char delegatingLocalLiteralGetter(Object o, byte b) {
		final long l = this.getLiteralL1();
		return this.charRoot(o, b, l);
	}

	char delegatingInlineStaticFieldGetter(int i, long l) {
		return this.charRoot(this.getStaticO(), i, l);
	}

	char delegatingLocalStaticFieldGetter(int i) {
		final Object o = this.getStaticO();
		return this.charRoot(o, i, i);
	}

	char delegatingUnboxing(long l) {
		return this.charRoot(null, this.I_BOXED, l);
	}

	float floatRoot(Object o, int i, long l) {
		return 0.1f;
	}

	float delegatingArrayLoad(Object o, int i) {
		return this.floatRoot(o, i, this.LONGS[i]);
	}

	byte chainRoot(byte b, char c, int i, long l) {
		return 2;
	}

	byte delegatingChain1(byte b, char c, int i) {
		return this.chainRoot(b, c, i, i);
	}

	byte delegatingChain2(byte b, char c1, char c2) {
		return this.delegatingChain1(b, c1, c2);
	}

	// NON-DELEGATING

	String conflictWithDelegate(Object o, int i, long l) {
		return this.stringRoot(o, i, l);
	}

	String conflictWithCoDelegater1(String s) {
		return this.stringRoot(s, 1, 20);
	}

	String conflictWithCoDelegater2(String s) {
		return this.stringRoot(s, 100, -2);
	}

	byte conflictWithChainAncestor(byte b, char c, int i) {
		return this.delegatingChain2(b, c, (char) i);
	}

	byte childOfConflictWithChainAncestor(byte b) {
		return this.conflictWithChainAncestor(b, (char) b, b);
	}

	void wrongReturn(int i, long l) {
		this.stringRoot(O, i, l);
	}

	String extraCall(Object o, long l) {
		this.getI();
		return this.stringRoot(o, 1, l);
	}

	String nonGetterCall(int i, long l) {
		return this.stringRoot(this.toString(), i, l);
	}

	String extraArithmetic(Object o, int i) {
		return this.stringRoot(o, i, i + i);
	}

	String extraCheck(int i, long l) {
		return this.stringRoot(O, i, i < l ? l : i);
	}

	String noParamsToParams() {
		return this.stringRoot(null, 0, 0);
	}

	void moreParams(Object o, int i, long l, String s) {
		this.stringRoot(o, i, l);
	}

	Enum<?> extraSet(Object o) {
		this.I = 2;
		return this.enumRoot(o, 0, 0);
	}

	Enum<?> extraInlineSet(long l) {
		return this.enumRoot(l, this.I = 2, l);
	}

	float extraArraySet(Object o, long l) {
		this.LONGS[0] = l;
		return this.floatRoot(o, 0, l);
	}
}
