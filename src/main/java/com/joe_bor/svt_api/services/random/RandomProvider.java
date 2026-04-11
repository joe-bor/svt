package com.joe_bor.svt_api.services.random;

// Small seam over randomness so services can stay deterministic in tests.
public interface RandomProvider {

    int nextInt(int bound);

    boolean nextBoolean();

    double nextDouble(double origin, double bound);
}
