package com.example;

import java.util.function.Consumer;

//  bsm=java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; (6)
@FunctionalInterface
public interface ZFunction {
	// InvokeDynamic name=eat desc=()Lcom/a/f;
	//		0 = {Type@4188} "(Ljava/lang/String;)V"
	// 		1 = {Handle@4189} "com/a/f.e(Ljava/lang/String;)V (6 itf)"
	// 		2 = {Type@4190} "(Ljava/lang/String;)V"
	// Field PUTSTATIC
	// RETURN
	ZFunction SIMPLE = food -> {};

	void eat(String food);

	static ZFunction ofInline(boolean val) {
		// InvokeDynamic name=eat desc=()Lcom/a/f;
		//		0 = {Type@4034} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4035} "com/a/f.d(Ljava/lang/String;)V (6 itf)"
		// 		2 = {Type@4036} "(Ljava/lang/String;)V"
		// ARETURN
		return food -> { boolean b = true; };
	}

	static ZFunction ofLocal(int val) {
		// InvokeDynamic name=eat desc=()Lcom/a/f;
		//		0 = {Type@4050} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4051} "com/a/f.c(Ljava/lang/String;)V (6 itf)"
		// 		2 = {Type@4052} "(Ljava/lang/String;)V"
		// VarInsn ASTORE
		// VarInsn ALOAD
		// ARETURN
		final ZFunction simple = food -> { int i = 0; };
		return simple;
	}

	static ZFunction ofInlineReference(long val) {
		// Field GETSTATIC
		// DUP
		// Method INVOKESTATIC
		// POP
		// InvokeDynamic name=eat desc=(Lcom/a/f;)Lcom/a/f;
		//		0 = {Type@4080} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4081} "com/a/f.eat(Ljava/lang/String;)V (9 itf)"
		// 		2 = {Type@4082} "(Ljava/lang/String;)V"
		// ARETURN
		return SIMPLE::eat;
	}

	static ZFunction ofLocalReference(float val) {
		// Field GETSTATIC
		// DUP
		// Method INVOKESTATIC
		// POP
		// InvokeDynamic name=eat desc=(Lcom/a/f;)Lcom/a/f;
		//		0 = {Type@4104} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4105} "com/a/f.eat(Ljava/lang/String;)V (9 itf)"
		// 		2 = {Type@4106} "(Ljava/lang/String;)V"
		// Var ASTORE
		// Var ALOAD
		// ARETURN
		final ZFunction reference = SIMPLE::eat;
		return reference;
	}

	static ZFunction ofArg(ZFunction function) {
		int i = 0;
		return function;
	}

	static ZFunction ofOfArg(char c) {
		// InvokeDynamic name=eat desc=()Lcom/a/f;
		//		0 = {Type@4123} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4124} "com/a/f.b(Ljava/lang/String;)V (6 itf)"
		// 		2 = {Type@4125} "(Ljava/lang/String;)V"
		// Method INVOKESTATIC desc=(Lcom/a/f;)Lcom/a/f;
		// ARETURN
		return ofArg(food -> { String string = "food"; });
	}

	static ZFunction ofArgAnd(char c1, ZFunction function, char c2) {
		return function;
	}

	static ZFunction ofOfArgAnd(byte b) {
		// Int 16
		// InvokeDynamic name=eat desc=()Lcom/a/f;
		//		0 = {Type@4144} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4145} "com/a/f.a(Ljava/lang/String;)V (6 itf)"
		// 		2 = {Type@4146} "(Ljava/lang/String;)V"
		// Int 16
		// Method 184 desc=(CLcom/a/f;C)Lcom/a/f;
		// 176
		return ofArgAnd('c', food -> { char c = 'c'; }, 'c');
	}

	static ZFunction ofExtras(boolean[] bs) {
		// ICONST_0
		// Var ISTORE
		// ICONST_0
		// Var ISTORE
		// Var ILOAD
		// Var ILOAD
		// InvokeDynamic name=eat desc=(ZI)Lcom/a/f;
		//		0 = {Type@4169} "(Ljava/lang/String;)V"
		// 		1 = {Handle@4170} "com/a/f.a(ZILjava/lang/String;)V (6 itf)"
		// 		2 = {Type@4171} "(Ljava/lang/String;)V"
		// ARETURN
		boolean b = false;
		int i = 0;
		return food -> { int j = b ? i : 1; };
	}
}
