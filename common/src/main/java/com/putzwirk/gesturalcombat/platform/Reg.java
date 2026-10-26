package com.putzwirk.gesturalcombat.platform;

import java.util.function.Supplier;

public final class Reg<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    public Reg(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Reg<T> of(T value) {
        return new Reg<>(() -> value);
    }

    @Override
    public T get() {
        return supplier.get();
    }
}
