package com.kartoush.platform.ulid;

import com.github.f4b6a3.ulid.UlidFactory;

import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

public final class DefaultUlidGenerator implements UlidGenerator {

    private final UlidFactory factory;

    public DefaultUlidGenerator() {
        this(RandomGenerator.getDefault());
    }

    public DefaultUlidGenerator(RandomGenerator random) {
        Objects.requireNonNull(random, "random must not be null");
        LongSupplier longSupplier = random::nextLong;

        this.factory = UlidFactory.newMonotonicInstance(longSupplier);
    }

    @Override
    public String next() {
        return factory.create().toString();
    }
}
