// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

import java.util.Random;

/**
 * Prime number calculations
 * @version 2.0.0
 * @since 1.4.0
 */
public final class BddPrime {

    static final int CHECKTIMES = 20;
    private final Random rng;

    /**
     * Private constructor.
     */
    public BddPrime() {
        rng = new Random();
    }

    /**
     * Returns the next prime greater than the given number.
     * @param num the number
     * @return the next prime greater than the given number
     */
    public int primeGte(int num) {
        if (isEven(num)) {
            ++num;
        }
        while (!isPrime(num)) {
            num += 2;
        }
        return num;
    }

    /**
     * Returns the next prime less than the given number.
     * @param num the number
     * @return the next prime less than the given number
     */
    public int primeLte(int num) {
        if (isEven(num)) {
            --num;
        }
        while (!isPrime(num)) {
            num -= 2;
        }
        return num;
    }

    static boolean isEven(final int src) {
        return (src & 0x1) == 0;
    }

    boolean isPrime(final int src) {
        return !hasEasyFactors(src) && isMillerRabinPrime(src);
    }

    static boolean hasEasyFactors(final int src) {
        return hasFactor(src, 3) || hasFactor(src, 5) || hasFactor(src, 7) || hasFactor(src, 11) || hasFactor(src, 13);
    }

    boolean isMillerRabinPrime(final int src) {
        for (int n = 0; n < CHECKTIMES; ++n) {
            final int witness = random(src - 1);
            if (isWitness(witness, src)) {
                return false;
            }
        }
        return true;
    }

    static boolean isWitness(final int witness, final int src) {
        final int bitNum = numberOfBits(src - 1) - 1;
        int d = 1;
        for (int i = bitNum; i >= 0; --i) {
            final int x = d;
            d = mulmod(d, d, src);
            if (d == 1 && x != 1 && x != src - 1) {
                return true;
            }
            if (bitIsSet(src - 1, i)) {
                d = mulmod(d, witness, src);
            }
        }
        return d != 1;
    }

    static int numberOfBits(final int src) {
        int b;
        if (src == 0) {
            return 0;
        }
        for (b = 31; b > 0; --b) {
            if (bitIsSet(src, b)) {
                return b + 1;
            }
        }
        return 1;
    }

    static boolean bitIsSet(final int src, final int b) {
        return (src & (1 << b)) != 0;
    }

    static int mulmod(final int a, final int b, final int c) {
        return (int) (((long) a * (long) b) % (long) c);
    }

    int random(final int i) {
        return rng.nextInt(i) + 1;
    }

    static boolean hasFactor(final int src, final int n) {
        return (src != n) && (src % n == 0);
    }
}
