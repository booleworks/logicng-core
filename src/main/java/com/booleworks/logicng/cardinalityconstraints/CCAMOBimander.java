// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * PBLib       -- Copyright (c) 2012-2013  Peter Steinke
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * Encodes that at most one variable is assigned value true.  Uses the bimander encoding due to Hölldobler and Nguyen.
 * @version 3.0.0
 * @since 1.1
 */
public final class CCAMOBimander implements CCAtMostOne {

    private static final CCAMOBimander INSTANCE = new CCAMOBimander();

    private CCAMOBimander() {
        // Singleton pattern
    }

    public static CCAMOBimander get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final CCConfig config, final Variable... vars) {
        final int groupSize = computeGroupSize(config, vars.length);
        encodeIntern(result, groupSize, new LNGVector<>(vars));
    }

    private static void encodeIntern(final EncodingResult result, final int groupSize, final LNGVector<Literal> vars) {
        final LNGVector<LNGVector<Literal>> groups = initializeGroups(result, groupSize, vars);
        final BimanderBits bits = initializeBits(result, groupSize);
        int grayCode;
        int nextGray;
        int i = 0;
        int index = -1;
        for (; i < bits.k; i++) {
            index++;
            grayCode = i ^ (i >> 1);
            i++;
            nextGray = i ^ (i >> 1);
            for (int j = 0; j < bits.numberOfBits; j++) {
                if ((grayCode & (1 << j)) == (nextGray & (1 << j))) {
                    handleGrayCode(result, groups, bits, grayCode, index, j);
                }
            }
        }
        for (; i < bits.twoPowNBits; i++) {
            index++;
            grayCode = i ^ (i >> 1);
            for (int j = 0; j < bits.numberOfBits; j++) {
                handleGrayCode(result, groups, bits, grayCode, index, j);
            }
        }
    }

    private static void handleGrayCode(final EncodingResult result, final LNGVector<LNGVector<Literal>> groups, final BimanderBits bits,
                                       final int grayCode, final int index, final int j) {
        if ((grayCode & (1 << j)) != 0) {
            for (int p = 0; p < groups.get(index).size(); ++p) {
                result.addClause(groups.get(index).get(p).negate(result.factory()), bits.bits.get(j));
            }
        } else {
            for (int p = 0; p < groups.get(index).size(); ++p) {
                result.addClause(groups.get(index).get(p).negate(result.factory()), bits.bits.get(j).negate(result.factory()));
            }
        }
    }

    private static LNGVector<LNGVector<Literal>> initializeGroups(final EncodingResult result, final int groupSize, final LNGVector<Literal> vars) {
        final LNGVector<LNGVector<Literal>> groups = new LNGVector<>();
        final int n = vars.size();
        for (int i = 0; i < groupSize; i++) {
            groups.push(new LNGVector<>());
        }

        int g = (int) Math.ceil((double) n / groupSize);
        int ig = 0;
        for (int i = 0; i < vars.size(); ) {
            while (i < g) {
                groups.get(ig).push(vars.get(i));
                i++;
            }
            ig++;
            g = g + (int) Math.ceil((double) (n - i) / (groupSize - ig));
        }

        for (int i = 0; i < groups.size(); i++) {
            CCAtMostOne.encodeNaive(result, groups.get(i));
        }
        return groups;
    }

    private static BimanderBits initializeBits(final EncodingResult result, final int groupSize) {
        final var bits = new BimanderBits();

        bits.numberOfBits = (int) Math.ceil(Math.log(groupSize) / Math.log(2));
        bits.twoPowNBits = (int) Math.pow(2, bits.numberOfBits);
        bits.k = (bits.twoPowNBits - groupSize) * 2;
        for (int i = 0; i < bits.numberOfBits; ++i) {
            bits.bits.push(result.newVariable());
        }
        return bits;
    }

    private static int computeGroupSize(final CCConfig config, final int numVars) {
        switch (config.bimanderGroupSize) {
            case FIXED:
                return config.bimanderFixedGroupSize;
            case HALF:
                return numVars / 2;
            case SQRT:
                return (int) Math.sqrt(numVars);
            default:
                throw new IllegalStateException("Unknown bimander group size: " + config.bimanderGroupSize);
        }
    }

    private static final class BimanderBits {
        private final LNGVector<Literal> bits = new LNGVector<>();
        private int numberOfBits;
        private int twoPowNBits;
        private int k;
    }
}
