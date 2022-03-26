package com.example;

import java.util.Optional;

public record RecordNamingTest(int value, double scale, Optional<String> s) {
    public static final RecordNamingTest INSTANCE = new RecordNamingTest(0, 0.0, Optional.empty());
}
