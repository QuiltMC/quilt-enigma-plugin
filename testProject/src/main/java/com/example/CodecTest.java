package com.example;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public class CodecTest {
    public static final Codec<CodecTest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("value").forGetter(CodecTest::getValue),
            Codec.DOUBLE.fieldOf("scale").orElse(1.0).forGetter(test -> test.getScale()),
            Codec.FLOAT.optionalFieldOf("factor").forGetter(CodecTest::getFactor),
            Codec.LONG.fieldOf("seed").forGetter(test -> test.seed)
    ).apply(instance, CodecTest::new));

    private final int value;
    private final double scale;
    private final Optional<Float> factor;
    public long seed;

    public CodecTest(int value, double scale, Optional<Float> factor, long seed) {
        this.value = value;
        this.scale = scale;
        this.factor = factor;
        this.seed = seed;
    }

    public int getValue() {
        return this.value;
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
