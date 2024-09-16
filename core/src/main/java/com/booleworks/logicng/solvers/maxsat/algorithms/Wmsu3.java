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

package com.booleworks.logicng.solvers.maxsat.algorithms;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.io.PrintStream;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The weighted MSU3 algorithm.
 * @version 3.0.0
 * @since 1.0
 */
public class Wmsu3 extends MaxSat {

    protected final boolean bmoStrategy;
    protected Encoder encoder;
    protected final MaxSatConfig.IncrementalStrategy incrementalStrategy;
    protected final PrintStream output;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public Wmsu3(final FormulaFactory f) {
        this(f, MaxSatConfig.builder().incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE).build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public Wmsu3(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
        verbosity = config.verbosity;
        incrementalStrategy = config.incrementalStrategy;
        bmoStrategy = config.bmo;
        output = config.output;
    }

    protected static boolean subsetSum(final LngIntVector set, final int sum) {
        final int n = set.size();
        final boolean[][] subset = new boolean[sum + 1][];
        for (int i = 0; i <= sum; i++) {
            subset[i] = new boolean[n + 1];
        }
        for (int i = 0; i <= n; i++) {
            subset[0][i] = true;
        }
        for (int i = 1; i <= sum; i++) {
            subset[i][0] = false;
        }
        for (int i = 1; i <= sum; i++) {
            for (int j = 1; j <= n; j++) {
                subset[i][j] = subset[i][j - 1];
                if (i >= set.get(j - 1)) {
                    subset[i][j] = subset[i][j] || subset[i - set.get(j - 1)][j - 1];
                }
            }
        }
        return subset[sum][n];
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        if (problemType == ProblemType.UNWEIGHTED) {
            throw new IllegalStateException(
                    "Error: Currently algorithm WMSU3 does not support unweighted MaxSAT instances.");
        }
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPbEncoding(config.pbEncoding);
        final boolean isBmo = bmoStrategy && isBmo(true);
        if (!isBmo) {
            currentWeight = 1;
        }
        switch (incrementalStrategy) {
            case NONE:
                return none(handler);
            case ITERATIVE:
                if (isBmo) {
                    if (encoder.cardEncoding() != MaxSatConfig.CardinalityEncoding.TOTALIZER) {
                        throw new IllegalStateException(
                                "Error: Currently iterative encoding in WMSU3 only supports the Totalizer encoding.");
                    }
                    return iterativeBmo(handler);
                } else {
                    return iterative(handler);
                }
            default:
                throw new IllegalArgumentException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    protected LngResult<MaxSatResult> iterative(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        final LngCoreSolver solver = rebuildSolver();
        encoder.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector coeffs = new LngIntVector();
        final LngIntVector fullObjFunction = new LngIntVector();
        final LngIntVector fullCoeffsFunction = new LngIntVector();
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(solver.model());
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("o " + newCost);
                    }
                    ubCost = newCost;
                }
                if (ubCost == 0 || lbCost == ubCost || (currentWeight == 1 && nbSatisfiable > 1)) {
                    assert lbCost == ubCost;
                    assert nbSatisfiable > 0;
                    return optimum();
                } else {
                    final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LngResult.canceled(upperBoundEvent);
                    }
                }
                for (int i = 0; i < softClauses.size(); i++) {
                    if (softClauses.get(i).weight() >= currentWeight && !activeSoft.get(i)) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
            } else {
                nbCores++;
                if (nbSatisfiable == 0) {
                    return unsat();
                } else if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return optimum();
                } else {
                    final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LngResult.canceled(lowerBoundEvent);
                    }
                }
                sumSizeCores += solver.assumptionsConflict().size();
                final LngIntVector objFunction = new LngIntVector();
                coeffs.clear();
                assumptions.clear();
                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    if (!coreMapping.containsKey(solver.assumptionsConflict().get(i))) {
                        continue;
                    }
                    final int indexSoft = coreMapping.get(solver.assumptionsConflict().get(i));
                    if (!activeSoft.get(indexSoft)) {
                        activeSoft.set(indexSoft, true);
                        objFunction.push(softClauses.get(indexSoft).relaxationVars().get(0));
                        coeffs.push(softClauses.get(indexSoft).weight());
                    }
                }
                for (int i = 0; i < softClauses.size(); i++) {
                    if (!activeSoft.get(i) && softClauses.get(i).weight() >= currentWeight) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                for (int i = 0; i < coeffs.size(); i++) {
                    fullCoeffsFunction.push(coeffs.get(i));
                    fullObjFunction.push(objFunction.get(i));
                }
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", fullCoeffsFunction.size(), softClauses.size());
                }
                lbCost++;
                while (!subsetSum(fullCoeffsFunction, lbCost)) {
                    lbCost++;
                }
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (!encoder.hasPbEncoding()) {
                    encoder.incEncodePb(solver, objFunction, coeffs, lbCost, assumptions, softClauses.size());
                } else {
                    encoder.incUpdatePb(solver, objFunction, coeffs, lbCost);
                    encoder.incUpdatePbAssumptions(assumptions);
                }
            }
        }
    }

    protected LngResult<MaxSatResult> none(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        LngCoreSolver solver = rebuildSolver();
        encoder.setIncremental(MaxSatConfig.IncrementalStrategy.NONE);
        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector coeffs = new LngIntVector();
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(solver.model());
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("o " + newCost);
                    }
                    ubCost = newCost;
                }
                if (ubCost == 0 || lbCost == ubCost || (currentWeight == 1 && nbSatisfiable > 1)) {
                    assert nbSatisfiable > 0;
                    return optimum();
                } else {
                    final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LngResult.canceled(upperBoundEvent);
                    }
                }
                for (int i = 0; i < softClauses.size(); i++) {
                    if (softClauses.get(i).weight() >= currentWeight && !activeSoft.get(i)) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
            } else {
                nbCores++;
                if (nbSatisfiable == 0) {
                    return unsat();
                } else if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return optimum();
                } else {
                    final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LngResult.canceled(lowerBoundEvent);
                    }
                }
                sumSizeCores += solver.assumptionsConflict().size();
                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    final int indexSoft = coreMapping.get(solver.assumptionsConflict().get(i));
                    assert !activeSoft.get(indexSoft);
                    activeSoft.set(indexSoft, true);
                }
                final LngIntVector objFunction = new LngIntVector();
                coeffs.clear();
                assumptions.clear();
                for (int i = 0; i < softClauses.size(); i++) {
                    if (activeSoft.get(i)) {
                        objFunction.push(softClauses.get(i).relaxationVars().get(0));
                        coeffs.push(softClauses.get(i).weight());
                    } else if (softClauses.get(i).weight() >= currentWeight) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", objFunction.size(), softClauses.size());
                }
                solver = rebuildSolver();
                lbCost++;
                while (!subsetSum(coeffs, lbCost)) {
                    lbCost++;
                }
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                encoder.encodePb(solver, objFunction, coeffs, lbCost);
            }
        }
    }

    protected LngResult<MaxSatResult> iterativeBmo(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        final LngCoreSolver solver = rebuildSolver();
        encoder.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
        final LngIntVector joinObjFunction = new LngIntVector();
        final LngIntVector encodingAssumptions = new LngIntVector();
        final LngIntVector joinCoeffs = new LngIntVector();
        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        int minWeight = 0;
        int posWeight = 0;
        int localCost = 0;
        final LngVector<LngIntVector> functions = new LngVector<>();
        final LngIntVector weights = new LngIntVector();
        final LngVector<Encoder> bmoEncodings = new LngVector<>();
        final LngBooleanVector firstEncoding = new LngBooleanVector();
        functions.push(new LngIntVector());
        weights.push(0);
        final LngIntVector objFunction = new LngIntVector();
        Encoder e = new Encoder(MaxSatConfig.CardinalityEncoding.TOTALIZER);
        e.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
        bmoEncodings.push(e);
        firstEncoding.push(true);
        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector coeffs = new LngIntVector();
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(solver.model());
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("o " + newCost);
                    }
                    ubCost = newCost;
                }
                if (nbSatisfiable == 1) {
                    if (ubCost == 0) {
                        return optimum();
                    } else {
                        final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LngResult.canceled(upperBoundEvent);
                        }
                    }
                    assert orderWeights.size() > 0;
                    assert orderWeights.get(0) > 1;
                    minWeight = orderWeights.get(orderWeights.size() - 1);
                    currentWeight = orderWeights.get(0);
                    for (int i = 0; i < softClauses.size(); i++) {
                        if (softClauses.get(i).weight() >= currentWeight) {
                            assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                        }
                    }
                } else {
                    if (currentWeight == 1 || currentWeight == minWeight) {
                        return optimum();
                    } else {
                        final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LngResult.canceled(upperBoundEvent);
                        }
                        assumptions.clear();
                        final int previousWeight = currentWeight;
                        posWeight++;
                        assert posWeight < orderWeights.size();
                        currentWeight = orderWeights.get(posWeight);
                        if (objFunction.size() > 0) {
                            functions.set(functions.size() - 1, new LngIntVector(objFunction));
                        }
                        functions.push(new LngIntVector());
                        weights.push(0);
                        localCost = 0;
                        e = new Encoder(MaxSatConfig.CardinalityEncoding.TOTALIZER);
                        e.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
                        bmoEncodings.push(e);
                        firstEncoding.push(true);
                        for (int i = 0; i < encodingAssumptions.size(); i++) {
                            solver.addClause(encodingAssumptions.get(i), null);
                        }
                        encodingAssumptions.clear();
                        for (int i = 0; i < softClauses.size(); i++) {
                            if (!activeSoft.get(i) && previousWeight == softClauses.get(i).weight()) {
                                solver.addClause(LngCoreSolver.not(softClauses.get(i).assumptionVar()), null);
                            }
                            if (currentWeight == softClauses.get(i).weight()) {
                                assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                            }
                            if (activeSoft.get(i)) {
                                assert softClauses.get(i).weight() == previousWeight;
                                activeSoft.set(i, false);
                            }
                        }
                    }
                }
            } else {
                localCost++;
                lbCost += currentWeight;
                nbCores++;
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return unsat();
                } else if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return optimum();
                } else {
                    final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LngResult.canceled(lowerBoundEvent);
                    }
                }
                sumSizeCores += solver.assumptionsConflict().size();
                joinObjFunction.clear();
                joinCoeffs.clear();
                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    if (coreMapping.containsKey(solver.assumptionsConflict().get(i))) {
                        if (activeSoft.get(coreMapping.get(solver.assumptionsConflict().get(i)))) {
                            continue;
                        }
                        assert softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i))).weight() ==
                                currentWeight;
                        activeSoft.set(coreMapping.get(solver.assumptionsConflict().get(i)), true);
                        joinObjFunction.push(softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i)))
                                .relaxationVars().get(0));
                        joinCoeffs.push(softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i))).weight());
                    }
                }
                objFunction.clear();
                coeffs.clear();
                assumptions.clear();
                for (int i = 0; i < softClauses.size(); i++) {
                    if (activeSoft.get(i)) {
                        assert softClauses.get(i).weight() == currentWeight;
                        objFunction.push(softClauses.get(i).relaxationVars().get(0));
                        coeffs.push(softClauses.get(i).weight());
                    } else if (currentWeight == softClauses.get(i).weight()) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", objFunction.size(), softClauses.size());
                }
                assert posWeight < functions.size();
                functions.set(posWeight, new LngIntVector(objFunction));
                weights.set(posWeight, localCost);
                if (firstEncoding.get(posWeight)) {
                    if (weights.get(posWeight) != objFunction.size()) {
                        bmoEncodings.get(posWeight).buildCardinality(solver, objFunction, weights.get(posWeight));
                        joinObjFunction.clear();
                        bmoEncodings.get(posWeight).incUpdateCardinality(solver, joinObjFunction, objFunction,
                                weights.get(posWeight), encodingAssumptions);
                        firstEncoding.set(posWeight, false);
                    }
                } else {
                    bmoEncodings.get(posWeight).incUpdateCardinality(solver, joinObjFunction, objFunction,
                            weights.get(posWeight), encodingAssumptions);
                }
                for (int i = 0; i < encodingAssumptions.size(); i++) {
                    assumptions.push(encodingAssumptions.get(i));
                }
            }
        }
    }

    protected LngCoreSolver rebuildSolver() {
        final LngCoreSolver s = newSatSolver();
        for (int i = 0; i < nVars(); i++) {
            newSatVariable(s);
        }
        for (int i = 0; i < hardClauses.size(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        LngIntVector clause;
        for (int i = 0; i < softClauses.size(); i++) {
            clause = new LngIntVector(softClauses.get(i).clause());
            for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                clause.push(softClauses.get(i).relaxationVars().get(j));
            }
            s.addClause(clause, null);
        }
        return s;
    }

    protected void initRelaxation() {
        for (int i = 0; i < softClauses.size(); i++) {
            final int l = newLiteral(false);
            softClauses.get(i).relaxationVars().push(l);
            softClauses.get(i).setAssumptionVar(l);
        }
    }
}
