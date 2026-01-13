// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings.cc;

import static com.booleworks.logicng.encodings.cc.CcSorting.ImplicationDirection.BOTH;
import static com.booleworks.logicng.encodings.cc.CcSorting.ImplicationDirection.INPUT_TO_OUTPUT;
import static com.booleworks.logicng.encodings.cc.CcSorting.ImplicationDirection.OUTPUT_TO_INPUT;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * Implementation of a sorting network.
 * @version 3.0.0
 * @since 1.1
 */
public final class CcSorting {

    private CcSorting() {
        // Only static methods
    }

    /**
     * The implication direction.
     */
    public enum ImplicationDirection {
        INPUT_TO_OUTPUT,
        OUTPUT_TO_INPUT,
        BOTH
    }

    private static int counterSorterValue(final int m, final int n) {
        return 2 * n + (m - 1) * (2 * (n - 1) - 1) - (m - 2) - 2 * ((m - 1) * (m - 2) / 2);
    }

    private static int directSorterValue(final int n) {
        if (n > 30) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.pow(2, n) - 1;
    }

    /**
     * Generates a sorter encoding for the given input.
     * @param f         the formula factory to generate new formulas
     * @param m         the counter
     * @param input     the input literals
     * @param result    the result of the encoding
     * @param output    the output literals
     * @param direction the sorting direction
     */
    public static void sort(final FormulaFactory f, final int m, final LngVector<Literal> input,
                            final EncodingResult result, final LngVector<Literal> output,
                            final ImplicationDirection direction) {
        assert m >= 0;
        if (m == 0) {
            output.clear();
            return;
        }
        final int n = input.size();
        int m2 = m;
        if (m2 > n) {
            m2 = n;
        }
        if (n == 0) {
            output.clear();
            return;
        }
        if (n == 1) {
            output.clear();
            output.push(input.get(0));
            return;
        }
        if (n == 2) {
            output.clear();
            final Variable o1 = result.newCcVariable();
            if (m2 == 2) {
                final Variable o2 = result.newCcVariable();
                comparator(result.getFactory(), input.get(0), input.get(1), o1, o2, result, direction);
                output.push(o1);
                output.push(o2);
            } else {
                comparator(result.getFactory(), input.get(0), input.get(1), o1, result, direction);
                output.push(o1);
            }
            return;
        }
        if (direction != INPUT_TO_OUTPUT) {
            recursiveSorter(f, m2, input, result, output, direction);
            return;
        }
        final int counter = counterSorterValue(m2, n);
        final int direct = directSorterValue(n);

        if (counter < direct) {
            counterSorter(m2, input, result, output, direction);
        } else {
            directSorter(m2, input, result, output, direction);
        }
    }

    private static void comparator(final FormulaFactory f, final Literal x1, final Literal x2, final Literal y,
                                   final EncodingResult result,
                                   final ImplicationDirection direction) {
        assert !x1.equals(x2);
        if (direction == INPUT_TO_OUTPUT || direction == BOTH) {
            result.addClause(x1.negate(f), y);
            result.addClause(x2.negate(f), y);
        }
        if (direction == OUTPUT_TO_INPUT || direction == BOTH) {
            result.addClause(y.negate(f), x1, x2);
        }
    }

    private static void comparator(final FormulaFactory f, final Literal x1, final Literal x2, final Literal y1,
                                   final Literal y2, final EncodingResult result,
                                   final ImplicationDirection direction) {
        assert !x1.equals(x2);
        assert !y1.equals(y2);
        if (direction == INPUT_TO_OUTPUT || direction == BOTH) {
            result.addClause(x1.negate(f), y1);
            result.addClause(x2.negate(f), y1);
            result.addClause(x1.negate(f), x2.negate(f), y2);
        }
        if (direction == OUTPUT_TO_INPUT || direction == BOTH) {
            result.addClause(y1.negate(f), x1, x2);
            result.addClause(y2.negate(f), x1);
            result.addClause(y2.negate(f), x2);
        }
    }

    private static void recursiveSorter(final FormulaFactory f, final int m, final int l,
                                        final LngVector<Literal> input, final EncodingResult result,
                                        final LngVector<Literal> output, final ImplicationDirection direction) {
        final int n = input.size();
        assert output.isEmpty();
        assert n > 1;
        assert m <= n;
        final LngVector<Literal> tmpLitsA = new LngVector<>();
        final LngVector<Literal> tmpLitsB = new LngVector<>();
        final LngVector<Literal> tmpLitsO1 = new LngVector<>();
        final LngVector<Literal> tmpLitsO2 = new LngVector<>();

        for (int i = 0; i < l; i++) {
            tmpLitsA.push(input.get(i));
        }
        for (int i = l; i < n; i++) {
            tmpLitsB.push(input.get(i));
        }

        assert tmpLitsA.size() + tmpLitsB.size() == n;

        sort(f, m, tmpLitsA, result, tmpLitsO1, direction);
        sort(f, m, tmpLitsB, result, tmpLitsO2, direction);
        merge(f, m, tmpLitsO1, tmpLitsO2, result, output, direction);

        assert tmpLitsO1.size() == Math.min(l, m);
        assert tmpLitsO2.size() == Math.min(n - l, m);
        assert output.size() == m;
    }

    private static void recursiveSorter(final FormulaFactory f, final int m, final LngVector<Literal> input,
                                        final EncodingResult result,
                                        final LngVector<Literal> output, final ImplicationDirection direction) {
        assert m > 0;
        assert !input.isEmpty();
        output.clear();
        final int n = input.size();
        assert n > 1;
        final int l = n / 2;
        recursiveSorter(f, m, l, input, result, output, direction);
    }

    private static void counterSorter(final int k, final LngVector<Literal> x, final EncodingResult result,
                                      final LngVector<Literal> output, final ImplicationDirection direction) {
        final FormulaFactory f = result.getFactory();
        final LngVector<LngVector<Literal>> auxVars = new LngVector<>();
        final int n = x.size();
        for (int i = 0; i < n; i++) {
            auxVars.push(new LngVector<>(k));
        }

        for (int j = 0; j < k; j++) {
            for (int i = j; i < n; i++) {
                auxVars.get(i).set(j, result.newCcVariable());
            }
        }
        if (direction == INPUT_TO_OUTPUT || direction == BOTH) {
            for (int i = 0; i < n; i++) {
                result.addClause(x.get(i).negate(f), auxVars.get(i).get(0));
                if (i > 0) {
                    result.addClause(auxVars.get(i - 1).get(0).negate(f), auxVars.get(i).get(0));
                }
            }
            for (int j = 1; j < k; j++) {
                for (int i = j; i < n; i++) {
                    result.addClause(x.get(i).negate(f), auxVars.get(i - 1).get(j - 1).negate(f),
                            auxVars.get(i).get(j));
                    if (i > j) {
                        result.addClause(auxVars.get(i - 1).get(j).negate(f), auxVars.get(i).get(j));
                    }
                }
            }
        }
        assert direction == INPUT_TO_OUTPUT;
        output.clear();
        for (int i = 0; i < k; i++) {
            output.push(auxVars.get(n - 1).get(i));
        }
    }

    private static void directSorter(final int m, final LngVector<Literal> input, final EncodingResult result,
                                     final LngVector<Literal> output, final ImplicationDirection direction) {
        assert direction == INPUT_TO_OUTPUT;
        final int n = input.size();
        assert n < 20;
        int bitmask = 1;
        final LngVector<Literal> clause = new LngVector<>();
        output.clear();
        for (int i = 0; i < m; i++) {
            output.push(result.newCcVariable());
        }
        while (bitmask < Math.pow(2, n)) {
            int count = 0;
            clause.clear();
            for (int i = 0; i < n; i++) {
                if (((1 << i) & bitmask) != 0) {
                    count++;
                    if (count > m) {
                        break;
                    }
                    clause.push(input.get(i).negate(result.getFactory()));
                }
            }
            assert count > 0;
            if (count <= m) {
                clause.push(output.get(count - 1));
                result.addClause(clause);
            }
            bitmask++;
        }
    }

    /**
     * Merges to input vectors.
     * @param f         the formula factory to generate new formulas
     * @param m         parameter m
     * @param inputA    the first input vector
     * @param inputB    the second input vector
     * @param formula   the result formula
     * @param output    the output vector
     * @param direction the sorting direction
     */
    public static void merge(final FormulaFactory f, final int m, final LngVector<Literal> inputA,
                             final LngVector<Literal> inputB, final EncodingResult formula,
                             final LngVector<Literal> output, final ImplicationDirection direction) {
        assert m >= 0;
        if (m == 0) {
            output.clear();
            return;
        }
        final int a = inputA.size();
        final int b = inputB.size();
        final int n = a + b;
        int m2 = m;
        if (m2 > n) {
            m2 = n;
        }
        if (a == 0 || b == 0) {
            if (a == 0) {
                output.replaceInplace(inputB);
            } else {
                output.replaceInplace(inputA);
            }
            return;
        }
        if (direction != INPUT_TO_OUTPUT) {
            recursiveMerger(f, m2, inputA, inputA.size(), inputB, inputB.size(), formula, output, direction);
            return;
        }
        directMerger(f, m2, inputA, inputB, formula, output, direction);
    }

    private static void recursiveMerger(final FormulaFactory f, final int c, final LngVector<Literal> inputA,
                                        final int a, final LngVector<Literal> inputB, final int b,
                                        final EncodingResult formula, final LngVector<Literal> output,
                                        final ImplicationDirection direction) {
        assert !inputA.isEmpty();
        assert !inputB.isEmpty();
        assert c > 0;
        output.clear();
        int a2 = a;
        int b2 = b;
        if (a2 > c) {
            a2 = c;
        }
        if (b2 > c) {
            b2 = c;
        }
        if (c == 1) {
            final Variable y = formula.newCcVariable();
            comparator(f, inputA.get(0), inputB.get(0), y, formula, direction);
            output.push(y);
            return;
        }
        if (a2 == 1 && b2 == 1) {
            assert c == 2;
            final Variable y1 = formula.newCcVariable();
            final Variable y2 = formula.newCcVariable();
            comparator(f, inputA.get(0), inputB.get(0), y1, y2, formula, direction);
            output.push(y1);
            output.push(y2);
            return;
        }
        final LngVector<Literal> oddMerge = new LngVector<>();
        final LngVector<Literal> evenMerge = new LngVector<>();
        final LngVector<Literal> tmpLitsOddA = new LngVector<>();
        final LngVector<Literal> tmpLitsOddB = new LngVector<>();
        final LngVector<Literal> tmpLitsEvenA = new LngVector<>();
        final LngVector<Literal> tmpLitsEvenB = new LngVector<>();

        for (int i = 0; i < a2; i = i + 2) {
            tmpLitsOddA.push(inputA.get(i));
        }
        for (int i = 0; i < b2; i = i + 2) {
            tmpLitsOddB.push(inputB.get(i));
        }
        for (int i = 1; i < a2; i = i + 2) {
            tmpLitsEvenA.push(inputA.get(i));
        }
        for (int i = 1; i < b2; i = i + 2) {
            tmpLitsEvenB.push(inputB.get(i));
        }

        merge(f, c / 2 + 1, tmpLitsOddA, tmpLitsOddB, formula, oddMerge, direction);
        merge(f, c / 2, tmpLitsEvenA, tmpLitsEvenB, formula, evenMerge, direction);

        assert !oddMerge.isEmpty();

        output.push(oddMerge.get(0));

        int i = 1;
        int j = 0;
        while (true) {
            if (i < oddMerge.size() && j < evenMerge.size()) {
                if (output.size() + 2 <= c) {
                    final Variable z0 = formula.newCcVariable();
                    final Variable z1 = formula.newCcVariable();
                    comparator(f, oddMerge.get(i), evenMerge.get(j), z0, z1, formula, direction);
                    output.push(z0);
                    output.push(z1);
                    if (output.size() == c) {
                        break;
                    }
                } else if (output.size() + 1 == c) {
                    final Variable z0 = formula.newCcVariable();
                    comparator(f, oddMerge.get(i), evenMerge.get(j), z0, formula, direction);
                    output.push(z0);
                    break;
                }
            } else if (i >= oddMerge.size() && j >= evenMerge.size()) {
                break;
            } else if (i >= oddMerge.size()) {
                assert j == evenMerge.size() - 1;
                output.push(evenMerge.back());
                break;
            } else {
                assert i == oddMerge.size() - 1;
                output.push(oddMerge.back());
                break;
            }
            i++;
            j++;
        }
        assert output.size() == a2 + b2 || output.size() == c;
    }

    private static void directMerger(final FormulaFactory f, final int m, final LngVector<Literal> inputA,
                                     final LngVector<Literal> inputB, final EncodingResult formula,
                                     final LngVector<Literal> output, final ImplicationDirection direction) {
        assert direction == INPUT_TO_OUTPUT;
        final int a = inputA.size();
        final int b = inputB.size();
        for (int i = 0; i < m; i++) {
            output.push(formula.newCcVariable());
        }
        int j = Math.min(m, a);
        for (int i = 0; i < j; i++) {
            formula.addClause(inputA.get(i).negate(f), output.get(i));
        }
        j = Math.min(m, b);
        for (int i = 0; i < j; i++) {
            formula.addClause(inputB.get(i).negate(f), output.get(i));
        }
        for (int i = 0; i < a; i++) {
            for (int k = 0; k < b; k++) {
                if (i + k + 1 < m) {
                    formula.addClause(inputA.get(i).negate(f), inputB.get(k).negate(f), output.get(i + k + 1));
                }
            }
        }
    }
}
