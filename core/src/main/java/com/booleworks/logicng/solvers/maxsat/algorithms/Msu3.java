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

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Verbosity;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Non-incremental MSU3 solver.
 * @version 3.0.0
 * @since 1.0
 */
public class Msu3 extends MaxSat {

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public Msu3(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(config.cardinalityEncoding);
        if (problemType == ProblemType.WEIGHTED) {
            throw new IllegalStateException(
                    "Error: Currently algorithm MSU3 does not support weighted MaxSAT instances.");
        }
        switch (incrementalStrategy) {
            case NONE:
                return none(handler);
            case ITERATIVE:
                if (encoder.cardEncoding() != CardinalityEncoding.TOTALIZER) {
                    throw new IllegalStateException(
                            "Error: Currently iterative encoding in MSU3 only supports the Totalizer encoding.");
                }
                return iterative(handler);
            default:
                throw new IllegalArgumentException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    protected LngResult<MaxSatResult> none(final ComputationHandler handler) {
        nbInitialVariables = nVars();
        final LngIntVector objFunction = new LngIntVector();
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        initRelaxation(objFunction);
        LngCoreSolver solver = rebuildSolver();
        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector currentObjFunction = new LngIntVector();
        encoder.setIncremental(IncrementalStrategy.NONE);
        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LngResult.canceled(upperBoundEvent);
                    }
                    for (int i = 0; i < objFunction.size(); i++) {
                        assumptions.push(LngCoreSolver.not(objFunction.get(i)));
                    }
                } else {
                    return optimum();
                }
            } else {
                lbCost++;
                nbCores++;
                if (verbosity != Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return unsat();
                } else if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != Verbosity.NONE) {
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
                    assert !activeSoft.get(coreMapping.get(solver.assumptionsConflict().get(i)));
                    activeSoft.set(coreMapping.get(solver.assumptionsConflict().get(i)), true);
                }
                currentObjFunction.clear();
                assumptions.clear();
                for (int i = 0; i < softClauses.size(); i++) {
                    if (activeSoft.get(i)) {
                        currentObjFunction.push(softClauses.get(i).relaxationVars().get(0));
                    } else {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                if (verbosity != Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", currentObjFunction.size(), objFunction.size());
                }
                solver = rebuildSolver();
                encoder.encodeCardinality(solver, currentObjFunction, lbCost);
            }
        }
    }

    protected LngResult<MaxSatResult> iterative(final ComputationHandler handler) {
        if (encoder.cardEncoding() != CardinalityEncoding.TOTALIZER) {
            throw new IllegalStateException(
                    "Error: Currently algorithm MSU3 with iterative encoding only supports the totalizer encoding.");
        }
        nbInitialVariables = nVars();
        final LngIntVector objFunction = new LngIntVector();
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        initRelaxation(objFunction);
        final LngCoreSolver solver = rebuildSolver();
        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector joinObjFunction = new LngIntVector();
        final LngIntVector currentObjFunction = new LngIntVector();
        final LngIntVector encodingAssumptions = new LngIntVector();
        encoder.setIncremental(IncrementalStrategy.ITERATIVE);
        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LngResult.canceled(upperBoundEvent);
                    }
                    for (int i = 0; i < objFunction.size(); i++) {
                        assumptions.push(LngCoreSolver.not(objFunction.get(i)));
                    }
                } else {
                    assert lbCost == newCost;
                    return optimum();
                }
            } else {
                lbCost++;
                nbCores++;
                if (verbosity != Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return unsat();
                }
                if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return optimum();
                }
                sumSizeCores += solver.assumptionsConflict().size();
                if (solver.assumptionsConflict().isEmpty()) {
                    return unsat();
                }
                final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LngResult.canceled(lowerBoundEvent);
                }
                joinObjFunction.clear();
                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    if (coreMapping.containsKey(solver.assumptionsConflict().get(i))) {
                        assert !activeSoft.get(coreMapping.get(solver.assumptionsConflict().get(i)));
                        activeSoft.set(coreMapping.get(solver.assumptionsConflict().get(i)), true);
                        joinObjFunction.push(softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i)))
                                .relaxationVars().get(0));
                    }
                }
                currentObjFunction.clear();
                assumptions.clear();
                for (int i = 0; i < softClauses.size(); i++) {
                    if (activeSoft.get(i)) {
                        currentObjFunction.push(softClauses.get(i).relaxationVars().get(0));
                    } else {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                if (verbosity != Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", currentObjFunction.size(), objFunction.size());
                }
                if (!encoder.hasCardEncoding()) {
                    if (lbCost != currentObjFunction.size()) {
                        encoder.buildCardinality(solver, currentObjFunction, lbCost);
                        joinObjFunction.clear();
                        encoder.incUpdateCardinality(solver, joinObjFunction, currentObjFunction, lbCost,
                                encodingAssumptions);
                    }
                } else {
                    encoder.incUpdateCardinality(solver, joinObjFunction, currentObjFunction, lbCost,
                            encodingAssumptions);
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

    protected void initRelaxation(final LngIntVector objFunction) {
        for (int i = 0; i < softClauses.size(); i++) {
            final int l = newLiteral(false);
            softClauses.get(i).relaxationVars().push(l);
            softClauses.get(i).setAssumptionVar(l);
            objFunction.push(l);
        }
    }
}
