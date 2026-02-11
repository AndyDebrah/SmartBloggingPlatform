
package com.smartblog.application.util;

import java.util.function.Supplier;

/**
 * Small helper utility for quickly measuring execution time of a Supplier.
 * Intended for lightweight local measurement and debugging.
 */
public final class Perf {

    private Perf() {}

    /**
     * Measure the supplied operation and return its result. Prints a simple
     * timing message to stdout.
     *
     * @param label descriptive label for the measured operation
     * @param supplier operation to execute and measure
     * @param <T> result type
     * @return the supplier's result
     */
    public static <T> T measure(String label, Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        long end = System.nanoTime();
        System.out.println(label + " took " + (end - start) / 1_000_000 + " ms");
        return result;
    }
}
