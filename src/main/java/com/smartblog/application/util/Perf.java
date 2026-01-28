
package com.smartblog.application.util;

import java.util.function.Supplier;

public final class Perf {

    public static <T> T measure(String label, Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        long end = System.nanoTime();
        System.out.println(label + " took " + (end - start) / 1_000_000 + " ms");
        return result;
    }
}
