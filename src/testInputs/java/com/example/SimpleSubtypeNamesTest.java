package com.example;

import com.example.simple_subtype_names.BirdEntity;
import com.example.simple_subtype_names.Entity;
import com.example.simple_subtype_names.EntityNonSuffixed;
import com.example.simple_subtype_names.LivingEntity;

public class SimpleSubtypeNamesTest {
	static final Entity ENTITY = new Entity();
	static final LivingEntity LIVING = new LivingEntity();
	static final BirdEntity BIRD = new BirdEntity();
	static final EntityNonSuffixed NON_SUFFIXED = new EntityNonSuffixed();

	static void eatStatic(Entity entity) { }
	static boolean eatLivingStatic(LivingEntity living) {
		return true;
	}

	static int eatBirdStatic(int count, BirdEntity bird) {
		return count;
	}

	static Object eatNonSuffixedStatic(EntityNonSuffixed nonSuffixed, Object arg) {
		return arg;
	}

	Entity entity = new Entity();
	LivingEntity living = new LivingEntity();
	BirdEntity bird = new BirdEntity();
	EntityNonSuffixed nonSuffixed = new EntityNonSuffixed();

	void eat(Entity entity) { }
	boolean eatLiving(LivingEntity living) {
		return true;
	}

	int eatBird(int count, BirdEntity bird) {
		return count;
	}

	Object eatNonSuffixed(EntityNonSuffixed nonSuffixed, Object arg) {
		return arg;
	}
}
