package com.example;

public class ZDelegatingMethodsTest {
	static Object O = new Object();
	static byte B = 20;

	int I = 0;

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

	static void staticVoidNoParamRoot() { }

	static void staticVoidNoParamDelegating() {
		staticVoidNoParamRoot();
	}

	void voidRoot(Object o, int i, long l) { }

	void voidDelegating(Object o, int i) {
		this.voidRoot(o, i, 0);
	}

	String root(Object o, int i, long l) {
		return "root";
	}

	String delegatingInlineLiteral(Object o, long l) {
		return this.root(o, 2, l);
	}

	String doublyDelegating(Object o, int i) {
		return this.delegatingInlineLiteral(o, i);
	}

	String delegatingLocalLiteral(Object o, int i) {
		long l = 3;
		return this.root(o, i, l);
	}

	String delegatingFinalLocalLiteral(Object o, int i) {
		final long l = 4;
		return this.root(o, i, l);
	}

	String delegatingInlineStaticField(int i, long l) {
		return this.root(O, i, l);
	}

	String delegatingLocalStaticField(int i, long l) {
		Object o = O;
		return this.root(o, i, l);
	}

	String delegatingInlineField(Object o, long l) {
		return this.root(o, this.I, l);
	}

	String delegatingLocalField(Object o, long l) {
		int i = this.I;
		return this.root(o, i, l);
	}

	String delegatingInlineGetter(Object o, long l) {
		return this.root(o, this.getI(), l);
	}

	String delegatingLocalGetter(Object o, long l) {
		int i = this.getI();
		return this.root(o, i, l);
	}

	String delegatingInlineLiteralGetter(Object o, int i) {
		return this.root(o, i, this.getLiteralL1());
	}

	String delegatingLocalLiteralGetter(Object o, int i) {
		final long l = this.getLiteralL1();
		return this.root(o, i, l);
	}

	String delegatingInlineStaticFieldGetter(int i, long l) {
		return this.root(this.getStaticO(), i, l);
	}

	String delegatingLocalStaticFieldGetter(int i, long l) {
		final Object o = this.getStaticO();
		return this.root(o, i, l);
	}

	String delegatingMixed1(Object o) {
		long l = this.getLiteralL1();
		return this.root(o, this.I, l);
	}

	String delegatingMixed2(long l) {
		return this.root(O, this.getI(), l);
	}

	String delegatingMixed3(int i) {
		return this.root(this.getLiteralS(), i, this.I);
	}

	// NON-DELEGATING

	String conflictingSignature(Object o, int i, long l) {
		return this.root(o, i, l);
	}

	void wrongReturn(int i, long l) {
		this.root(O, i, l);
	}

	String extraCall(Object o, long l) {
		this.getI();
		return this.root(o, 1, l);
	}

	String nonGetterCall(int i, long l) {
		return this.root(this.toString(), i, l);
	}

	String extraArithmetic(Object o, int i) {
		return this.root(o, i, i + i);
	}

	String extraCheck(int i , long l) {
		return this.root(O, i, i < l ? l : i);
	}

	String noParamsToParams() {
		return this.root(null, 0, 0);
	}

	void moreParams(int i) {
		staticVoidNoParamRoot();
	}
}
