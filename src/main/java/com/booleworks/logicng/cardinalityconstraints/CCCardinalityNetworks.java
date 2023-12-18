// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * PBLib -- Copyright (c) 2012-2013 Peter Steinke <p> Permission is hereby
 * granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions: <p> The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.cardinalityconstraints;

import static com.booleworks.logicng.cardinalityconstraints.CCSorting.ImplicationDirection.BOTH;
import static com.booleworks.logicng.cardinalityconstraints.CCSorting.ImplicationDirection.INPUT_TO_OUTPUT;
import static com.booleworks.logicng.cardinalityconstraints.CCSorting.ImplicationDirection.OUTPUT_TO_INPUT;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * Implementation of cardinality networks due to Asín, Nieuwenhuis, Oliveras,
 * and Rodríguez-Carbonell.
 * @version 3.0.0
 * @since 1.1
 */
public final class CCCardinalityNetworks {

    private CCCardinalityNetworks() {
        // Only static methods
    }

    static void buildAMK(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.factory();
        final LNGVector<Literal> input = new LNGVector<>();
        final LNGVector<Literal> output = new LNGVector<>();
        if (rhs > vars.length / 2) {
            final int geq = vars.length - rhs;
            for (final Variable v : vars) {
                input.push(v.negate(f));
            }
            CCSorting.sort(f, geq, input, result, output, OUTPUT_TO_INPUT);
            for (int i = 0; i < geq; i++) {
                result.addClause(output.get(i));
            }
        } else {
            for (final Variable v : vars) {
                input.push(v);
            }
            CCSorting.sort(f, rhs + 1, input, result, output, INPUT_TO_OUTPUT);
            assert output.size() > rhs;
            result.addClause(output.get(rhs).negate(f));
        }
    }

    static CCIncrementalData buildAMKForIncremental(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.factory();
        final LNGVector<Literal> input = new LNGVector<>();
        final LNGVector<Literal> output = new LNGVector<>();
        for (final Variable var : vars) {
            input.push(var);
        }
        CCSorting.sort(f, rhs + 1, input, result, output, INPUT_TO_OUTPUT);
        assert output.size() > rhs;
        result.addClause(output.get(rhs).negate(f));
        return new CCIncrementalData(result, CCConfig.AMK_ENCODER.CARDINALITY_NETWORK, rhs, output);
    }

    static void buildALK(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.factory();
        final LNGVector<Literal> input = new LNGVector<>();
        final LNGVector<Literal> output = new LNGVector<>();
        final int newRHS = vars.length - rhs;
        if (newRHS > vars.length / 2) {
            final int geq = vars.length - newRHS;
            for (final Variable v : vars) {
                input.push(v);
            }
            CCSorting.sort(f, geq, input, result, output, OUTPUT_TO_INPUT);
            for (int i = 0; i < geq; i++) {
                result.addClause(output.get(i));
            }
        } else {
            for (final Variable v : vars) {
                input.push(v.negate(f));
            }
            CCSorting.sort(f, newRHS + 1, input, result, output, INPUT_TO_OUTPUT);
            assert output.size() > newRHS;
            result.addClause(output.get(newRHS).negate(f));
        }
    }

    static CCIncrementalData buildALKForIncremental(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.factory();
        final LNGVector<Literal> input = new LNGVector<>();
        final LNGVector<Literal> output = new LNGVector<>();
        for (final Variable var : vars) {
            input.push(var.negate(f));
        }
        final int newRHS = vars.length - rhs;
        CCSorting.sort(f, newRHS + 1, input, result, output, INPUT_TO_OUTPUT);
        assert output.size() > newRHS;
        result.addClause(output.get(newRHS).negate(f));
        return new CCIncrementalData(result, CCConfig.ALK_ENCODER.CARDINALITY_NETWORK, rhs, vars.length, output);
    }

    static void buildEXK(final EncodingResult result, final Variable[] vars, final int rhs) {
        final FormulaFactory f = result.factory();
        final LNGVector<Literal> input = new LNGVector<>();
        final LNGVector<Literal> output = new LNGVector<>();
        for (final Variable var : vars) {
            input.push(var);
        }
        CCSorting.sort(f, rhs + 1, input, result, output, BOTH);
        assert output.size() > rhs;
        result.addClause(output.get(rhs).negate(f));
        result.addClause(output.get(rhs - 1));
    }
}
