// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Open-WBO -- Copyright (c) 2013-2015, Ruben Martins, Vasco Manquinho, Ines
 * Lynce <p> Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions: <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. <p> THE SOFTWARE IS
 * PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.encodings.cc;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * At-Most-One encodings.
 * @version 3.0.0
 * @since 1.0.0
 */
public final class CcAmo {
    private CcAmo() {
        // only static methods
    }

    /**
     * Naive encoding without introduction of new variables but with quadratic
     * size.
     * @param result the encoding result
     * @param vars   the variables for the at-most-one constraint
     */
    public static void pure(final EncodingResult result, final Variable... vars) {
        final FormulaFactory f = result.getFactory();
        for (int i = 0; i < vars.length; i++) {
            for (int j = i + 1; j < vars.length; j++) {
                result.addClause(vars[i].negate(f), vars[j].negate(f));
            }
        }
    }

    /**
     * Ladder/Regular encoding.
     * @param result the encoding result
     * @param vars   the variables for the at-most-one constraint
     */
    public static void ladder(final EncodingResult result, final Variable... vars) {
        final FormulaFactory f = result.getFactory();
        final Variable[] seqAuxiliary = new Variable[vars.length - 1];
        for (int i = 0; i < vars.length - 1; i++) {
            seqAuxiliary[i] = result.newCCVariable();
        }
        for (int i = 0; i < vars.length; i++) {
            if (i == 0) {
                result.addClause(vars[0].negate(f), seqAuxiliary[0]);
            } else if (i == vars.length - 1) {
                result.addClause(vars[i].negate(f), seqAuxiliary[i - 1].negate(f));
            } else {
                result.addClause(vars[i].negate(f), seqAuxiliary[i]);
                result.addClause(seqAuxiliary[i - 1].negate(f), seqAuxiliary[i]);
                result.addClause(vars[i].negate(f), seqAuxiliary[i - 1].negate(f));
            }
        }
    }

    /**
     * 2-product encoding due to Chen.
     * @param result         the encoding result
     * @param recursiveBound the recursive bound
     * @param vars           the variables for the at-most-one constraint
     */
    public static void product(final EncodingResult result, final int recursiveBound, final Variable... vars) {
        final FormulaFactory f = result.getFactory();
        final int n = vars.length;
        final int p = (int) Math.ceil(Math.sqrt(n));
        final int q = (int) Math.ceil((double) n / (double) p);
        final Variable[] us = new Variable[p];
        for (int i = 0; i < us.length; i++) {
            us[i] = result.newCCVariable();
        }
        final Variable[] vs = new Variable[q];
        for (int i = 0; i < vs.length; i++) {
            vs[i] = result.newCCVariable();
        }
        if (us.length <= recursiveBound) {
            pure(result, us);
        } else {
            product(result, recursiveBound, us);
        }
        if (vs.length <= recursiveBound) {
            pure(result, vs);
        } else {
            product(result, recursiveBound, vs);
        }
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < q; j++) {
                final int k = i * q + j;
                if (k >= 0 && k < n) {
                    result.addClause(vars[k].negate(f), us[i]);
                    result.addClause(vars[k].negate(f), vs[j]);
                }
            }
        }
    }

    /**
     * Binary encoding due to Doggett, Frisch, Peugniez, and Nightingale.
     * @param result the encoding result
     * @param vars   the variables for the at-most-one constraint
     */
    public static void binary(final EncodingResult result, final Variable... vars) {
        final FormulaFactory f = result.getFactory();
        final int numberOfBits = (int) Math.ceil(Math.log(vars.length) / Math.log(2));
        final int twoPowNBits = (int) Math.pow(2, numberOfBits);
        final int k = (twoPowNBits - vars.length) * 2;
        final Variable[] bits = new Variable[numberOfBits];
        for (int i = 0; i < numberOfBits; i++) {
            bits[i] = result.newCCVariable();
        }
        int grayCode;
        int nextGray;
        int i = 0;
        int index = -1;
        while (i < k) {
            index++;
            grayCode = i ^ (i >> 1);
            i++;
            nextGray = i ^ (i >> 1);
            for (int j = 0; j < numberOfBits; ++j) {
                if ((grayCode & (1 << j)) == (nextGray & (1 << j))) {
                    if ((grayCode & (1 << j)) != 0) {
                        result.addClause(vars[index].negate(f), bits[j]);
                    } else {
                        result.addClause(vars[index].negate(f), bits[j].negate(f));
                    }
                }
            }
            i++;
        }
        while (i < twoPowNBits) {
            index++;
            grayCode = i ^ (i >> 1);
            for (int j = 0; j < numberOfBits; ++j) {
                if ((grayCode & (1 << j)) != 0) {
                    result.addClause(vars[index].negate(f), bits[j]);
                } else {
                    result.addClause(vars[index].negate(f), bits[j].negate(f));
                }
            }
            i++;
        }
    }

    /**
     * Commander encoding due to Klieber and Kwon.
     * @param result    the encoding result
     * @param groupSize the group size
     * @param vars      the variables for the at-most-one constraint
     */
    public static void commander(final EncodingResult result, final int groupSize, final Variable... vars) {
        commanderRec(result, groupSize, new LNGVector<>(vars));
    }

    private static void commanderRec(final EncodingResult result, final int groupSize,
                                     final LNGVector<Literal> currentLiterals) {
        final FormulaFactory f = result.getFactory();
        boolean isExactlyOne = false;
        while (currentLiterals.size() > groupSize) {
            final LNGVector<Literal> literals = new LNGVector<>();
            final LNGVector<Literal> nextLiterals = new LNGVector<>();
            for (int i = 0; i < currentLiterals.size(); i++) {
                literals.push(currentLiterals.get(i));
                if (i % groupSize == groupSize - 1 || i == currentLiterals.size() - 1) {
                    pure(result, literals);
                    literals.push(result.newCCVariable());
                    nextLiterals.push(literals.back().negate(f));
                    if (isExactlyOne && literals.size() > 0) {
                        result.addClause(literals);
                    }
                    for (int j = 0; j < literals.size() - 1; j++) {
                        result.addClause(literals.back().negate(f), literals.get(j).negate(f));
                    }
                    literals.clear();
                }
            }
            currentLiterals.replaceInplace(nextLiterals);
            isExactlyOne = true;
        }
        pure(result, currentLiterals);
        if (isExactlyOne && currentLiterals.size() > 0) {
            result.addClause(currentLiterals);
        }
    }

    /**
     * Nested encoding.
     * @param result    the encoding result
     * @param groupSize the group size
     * @param vars      the variables for the at-most-one constraint
     */
    public static void nested(final EncodingResult result, final int groupSize, final Variable... vars) {
        nestedRec(result, groupSize, new LNGVector<>(vars));
    }

    private static void nestedRec(final EncodingResult result, final int groupSize, final LNGVector<Literal> vars) {
        final FormulaFactory f = result.getFactory();
        if (vars.size() <= groupSize) {
            for (int i = 0; i + 1 < vars.size(); i++) {
                for (int j = i + 1; j < vars.size(); j++) {
                    result.addClause(vars.get(i).negate(f), vars.get(j).negate(f));
                }
            }
        } else {
            final LNGVector<Literal> l1 = new LNGVector<>(vars.size() / 2);
            final LNGVector<Literal> l2 = new LNGVector<>(vars.size() / 2);
            int i = 0;
            for (; i < vars.size() / 2; i++) {
                l1.push(vars.get(i));
            }
            for (; i < vars.size(); i++) {
                l2.push(vars.get(i));
            }
            final Variable newVariable = result.newCCVariable();
            l1.push(newVariable);
            l2.push(newVariable.negate(f));
            nestedRec(result, groupSize, l1);
            nestedRec(result, groupSize, l2);
        }
    }

    /**
     * Bimander encoding due to Hölldobler and Nguyen.
     * @param result    the encoding result
     * @param groupSize the group size
     * @param vars      the variables for the at-most-one constraint
     */
    public static void bimander(final EncodingResult result, final int groupSize, final Variable... vars) {
        bimanderIntern(result, groupSize, new LNGVector<>(vars));
    }

    private static void bimanderIntern(final EncodingResult result, final int groupSize,
                                       final LNGVector<Literal> vars) {
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

    private static void handleGrayCode(final EncodingResult result, final LNGVector<LNGVector<Literal>> groups,
                                       final BimanderBits bits,
                                       final int grayCode, final int index, final int j) {
        if ((grayCode & (1 << j)) != 0) {
            for (int p = 0; p < groups.get(index).size(); ++p) {
                result.addClause(groups.get(index).get(p).negate(result.getFactory()), bits.bits.get(j));
            }
        } else {
            for (int p = 0; p < groups.get(index).size(); ++p) {
                result.addClause(groups.get(index).get(p).negate(result.getFactory()),
                        bits.bits.get(j).negate(result.getFactory()));
            }
        }
    }

    private static LNGVector<LNGVector<Literal>> initializeGroups(final EncodingResult result, final int groupSize,
                                                                  final LNGVector<Literal> vars) {
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
            pure(result, groups.get(i));
        }
        return groups;
    }

    private static BimanderBits initializeBits(final EncodingResult result, final int groupSize) {
        final var bits = new BimanderBits();

        bits.numberOfBits = (int) Math.ceil(Math.log(groupSize) / Math.log(2));
        bits.twoPowNBits = (int) Math.pow(2, bits.numberOfBits);
        bits.k = (bits.twoPowNBits - groupSize) * 2;
        for (int i = 0; i < bits.numberOfBits; ++i) {
            bits.bits.push(result.newCCVariable());
        }
        return bits;
    }

    private static void pure(final EncodingResult result, final LNGVector<Literal> vars) {
        final FormulaFactory f = result.getFactory();
        for (int i = 0; i < vars.size(); i++) {
            for (int j = i + 1; j < vars.size(); j++) {
                result.addClause(vars.get(i).negate(f), vars.get(j).negate(f));
            }
        }
    }

    private static final class BimanderBits {
        private final LNGVector<Literal> bits = new LNGVector<>();
        private int numberOfBits;
        private int twoPowNBits;
        private int k;
    }
}
