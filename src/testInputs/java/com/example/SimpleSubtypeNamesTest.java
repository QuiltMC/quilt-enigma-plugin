package com.example;

import com.example.simple_subtype_names.block_entity_renderer.BlockEntityRenderer;
import com.example.simple_subtype_names.block_entity_renderer.DisplayBlockEntityRenderer;
import com.example.simple_subtype_names.block_entity_renderer.DuplicateBlockEntityRenderer;
import com.example.simple_subtype_names.block_entity_renderer.PaintingBlockEntityRenderer;
import com.example.simple_subtype_names.entity.BirdEntity;
import com.example.simple_subtype_names.entity.Entity;
import com.example.simple_subtype_names.entity.EntityNonSuffixed;
import com.example.simple_subtype_names.entity.LivingEntity;

public class SimpleSubtypeNamesTest {
	static final Entity ENTITY = new Entity();
	static final LivingEntity LIVING_ENTITY = null;
	static final BirdEntity BIRD = new BirdEntity();
	static final EntityNonSuffixed NON_SUFFIXED = new EntityNonSuffixed();

	static final BlockEntityRenderer BLOCK_ENTITY_RENDERER = new BlockEntityRenderer();
	static final DisplayBlockEntityRenderer DISPLAY_RENDERER = new DisplayBlockEntityRenderer();
	static final PaintingBlockEntityRenderer PAINTING_RENDERER = new PaintingBlockEntityRenderer();

	static final DuplicateBlockEntityRenderer DUPLICATE_1 = new DuplicateBlockEntityRenderer();
	static final DuplicateBlockEntityRenderer DUPLICATE_2 = new DuplicateBlockEntityRenderer();

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

	static void renderStatic(BlockEntityRenderer blockEntityRenderer) { }

	static void renderStatic(DisplayBlockEntityRenderer displayRenderer) { }

	static void renderStatic(PaintingBlockEntityRenderer paintingRenderer) { }

	static void renderStatic(DuplicateBlockEntityRenderer duplicate1, DuplicateBlockEntityRenderer duplicate2) { }

	Entity entity = new Entity();
	LivingEntity livingEntity = null;
	BirdEntity bird = new BirdEntity();
	EntityNonSuffixed nonSuffixed = new EntityNonSuffixed();

	BlockEntityRenderer blockEntityRenderer = new BlockEntityRenderer();
	DisplayBlockEntityRenderer displayRenderer = new DisplayBlockEntityRenderer();
	PaintingBlockEntityRenderer paintingRenderer = new PaintingBlockEntityRenderer();

	DuplicateBlockEntityRenderer duplicate1 = new DuplicateBlockEntityRenderer();
	DuplicateBlockEntityRenderer duplicate2 = new DuplicateBlockEntityRenderer();

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

	void render(BlockEntityRenderer blockEntityRenderer) { }

	void render(DisplayBlockEntityRenderer displayRenderer) { }

	void render(PaintingBlockEntityRenderer paintingRenderer) { }

	void render(DuplicateBlockEntityRenderer duplicate1, DuplicateBlockEntityRenderer duplicate2) { }
}
