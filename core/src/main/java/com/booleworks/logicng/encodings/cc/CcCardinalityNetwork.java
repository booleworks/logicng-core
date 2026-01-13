// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings.cc;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.encodings.CcIncrementalData;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * Implementation of cardinality networks due to Asín, Nieuwenhuis, Oliveras,
 * and Rodríguez-Carbonell.
 * @version 3.0.0
 * @since 1.1
 */
public final class CcCardinalityNetwork {

    private CcCardinalityNetwork() {
        // only static methods
    }

    public static void amk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.getFactory();
        final LngVector<Literal> input = new LngVector<>();
        final LngVector<Literal> output = new LngVector<>();
        if (rhs > vars.length / 2) {
            final int geq = vars.length - rhs;
            for (final Variable v : vars) {
                input.push(v.negate(f));
            }
            CcSorting.sort(f, geq, input, result, output, CcSorting.ImplicationDirection.OUTPUT_TO_INPUT);
            for (int i = 0; i < geq; i++) {
                result.addClause(output.get(i));
            }
        } else {
            for (final Variable v : vars) {
                input.push(v);
            }
            CcSorting.sort(f, rhs + 1, input, result, output, CcSorting.ImplicationDirection.INPUT_TO_OUTPUT);
            assert output.size() > rhs;
            result.addClause(output.get(rhs).negate(f));
        }
    }

    public static <T extends EncodingResult> CcIncrementalData<T> amkForIncremental(final T result,
                                                                                    final Variable[] vars,
                                                                                    final int rhs) {
        final FormulaFactory f = result.getFactory();
        final LngVector<Literal> input = new LngVector<>();
        final LngVector<Literal> output = new LngVector<>();
        for (final Variable var : vars) {
            input.push(var);
        }
        CcSorting.sort(f, rhs + 1, input, result, output, CcSorting.ImplicationDirection.INPUT_TO_OUTPUT);
        assert output.size() > rhs;
        result.addClause(output.get(rhs).negate(f));
        return new CcIncrementalData<>(result, EncoderConfig.AmkEncoder.CARDINALITY_NETWORK, rhs, output);
    }

    public static void alk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.getFactory();
        final LngVector<Literal> input = new LngVector<>();
        final LngVector<Literal> output = new LngVector<>();
        final int newRhs = vars.length - rhs;
        if (newRhs > vars.length / 2) {
            final int geq = vars.length - newRhs;
            for (final Variable v : vars) {
                input.push(v);
            }
            CcSorting.sort(f, geq, input, result, output, CcSorting.ImplicationDirection.OUTPUT_TO_INPUT);
            for (int i = 0; i < geq; i++) {
                result.addClause(output.get(i));
            }
        } else {
            for (final Variable v : vars) {
                input.push(v.negate(f));
            }
            CcSorting.sort(f, newRhs + 1, input, result, output, CcSorting.ImplicationDirection.INPUT_TO_OUTPUT);
            assert output.size() > newRhs;
            result.addClause(output.get(newRhs).negate(f));
        }
    }

    public static <T extends EncodingResult> CcIncrementalData<T> alkForIncremental(final T result,
                                                                                    final Variable[] vars,
                                                                                    final int rhs) {
        final FormulaFactory f = result.getFactory();
        final LngVector<Literal> input = new LngVector<>();
        final LngVector<Literal> output = new LngVector<>();
        for (final Variable var : vars) {
            input.push(var.negate(f));
        }
        final int newRhs = vars.length - rhs;
        CcSorting.sort(f, newRhs + 1, input, result, output, CcSorting.ImplicationDirection.INPUT_TO_OUTPUT);
        assert output.size() > newRhs;
        result.addClause(output.get(newRhs).negate(f));
        return new CcIncrementalData<>(result, EncoderConfig.AlkEncoder.CARDINALITY_NETWORK, rhs, vars.length, output);
    }

    public static void exk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.getFactory();
        final LngVector<Literal> input = new LngVector<>();
        final LngVector<Literal> output = new LngVector<>();
        for (final Variable var : vars) {
            input.push(var);
        }
        CcSorting.sort(f, rhs + 1, input, result, output, CcSorting.ImplicationDirection.BOTH);
        assert output.size() > rhs;
        result.addClause(output.get(rhs).negate(f));
        result.addClause(output.get(rhs - 1));
    }
}
