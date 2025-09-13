package com.example;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public class ConstructorParamsTest {
	public static final Codec<ConstructorParamsTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("value").forGetter(ConstructorParamsTest::getValue),
			Codec.DOUBLE.fieldOf("scale").orElse(1.0).forGetter(test -> test.getScale()),
			Codec.FLOAT.optionalFieldOf("factor").forGetter(ConstructorParamsTest::getFactor),
			Codec.LONG.fieldOf("seed").forGetter(test -> test.seed)
	).apply(instance, ConstructorParamsTest::new));

	private int value;
	private final double scale;
	private final Optional<Float> factor;
	public long seed;

	public ConstructorParamsTest(int value, double scale, Optional<Float> factor, long seed) {
		this.value = value;
		this.scale = scale;
		this.factor = factor;
		this.seed = seed;
	}

	public ConstructorParamsTest() {
		this(1, 2.0, Optional.empty(), 0);
	}

	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public double getScale() {
		return this.scale;
	}

	public Optional<Float> getFactor() {
		return this.factor;
	}

	record ExampleRecord(int value) {
		public static final Codec<ExampleRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("v").forGetter(r -> r.value)
		).apply(instance, ExampleRecord::new));
	}
}
