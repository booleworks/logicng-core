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

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.solvers.maxsat.InternalMaxSATResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.io.PrintStream;

/**
 * Linear search solver.
 * @version 2.1.0
 * @since 1.0
 */
public class LinearUS extends MaxSAT {

    protected final Encoder encoder;
    protected final MaxSATConfig.IncrementalStrategy incrementalStrategy;
    protected final LNGIntVector objFunction;
    protected final PrintStream output;
    protected LNGCoreSolver solver;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public LinearUS(final FormulaFactory f) {
        this(f, MaxSATConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public LinearUS(final FormulaFactory f, final MaxSATConfig config) {
        super(f, config);
        solver = null;
        verbosity = config.verbosity;
        incrementalStrategy = config.incrementalStrategy;
        encoder = new Encoder(config.cardinalityEncoding);
        objFunction = new LNGIntVector();
        output = config.output;
    }

    @Override
    protected LNGResult<InternalMaxSATResult> internalSearch(final ComputationHandler handler) {
        if (problemType == ProblemType.WEIGHTED) {
            throw new IllegalStateException("Error: Currently LinearUS does not support weighted MaxSAT instances.");
        }
        switch (incrementalStrategy) {
            case NONE:
                return none(handler);
            case ITERATIVE:
                if (encoder.cardEncoding() != MaxSATConfig.CardinalityEncoding.TOTALIZER) {
                    throw new IllegalStateException(
                            "Error: Currently iterative encoding in LinearUS only supports the Totalizer encoding.");
                }
                return iterative(handler);
            default:
                throw new IllegalArgumentException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    protected LNGResult<InternalMaxSATResult> none(final ComputationHandler handler) {
        nbInitialVariables = nVars();
        initRelaxation();
        solver = rebuildSolver();
        final LNGIntVector assumptions = new LNGIntVector();
        encoder.setIncremental(MaxSATConfig.IncrementalStrategy.NONE);
        while (true) {
            final LNGResult<Boolean> res = searchSATSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LNGResult.aborted(res.getAbortionEvent());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    final LNGEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LNGResult.aborted(upperBoundEvent);
                    }
                    if (encoder.cardEncoding() == MaxSATConfig.CardinalityEncoding.MTOTALIZER) {
                        encoder.setModulo((int) Math.ceil(Math.sqrt(ubCost + 1.0)));
                    }
                    encoder.encodeCardinality(solver, objFunction, 0);
                } else {
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
            } else {
                lbCost++;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                } else if (lbCost == ubCost) {
                    if (nbSatisfiable > 0) {
                        if (verbosity != MaxSATConfig.Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                    } else {
                        return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                    }
                } else {
                    final LNGEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LNGResult.aborted(lowerBoundEvent);
                    }
                }
                solver = rebuildSolver();
                encoder.encodeCardinality(solver, objFunction, lbCost);
            }
        }
    }

    protected LNGResult<InternalMaxSATResult> iterative(final ComputationHandler handler) {
        assert encoder.cardEncoding() == MaxSATConfig.CardinalityEncoding.TOTALIZER;
        nbInitialVariables = nVars();
        initRelaxation();
        solver = rebuildSolver();
        final LNGIntVector assumptions = new LNGIntVector();
        encoder.setIncremental(MaxSATConfig.IncrementalStrategy.ITERATIVE);
        while (true) {
            final LNGResult<Boolean> res = searchSATSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LNGResult.aborted(res.getAbortionEvent());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    final LNGEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LNGResult.aborted(upperBoundEvent);
                    }
                    for (int i = 0; i < objFunction.size(); i++) {
                        assumptions.push(LNGCoreSolver.not(objFunction.get(i)));
                    }
                } else {
                    assert lbCost == ubCost;
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
            } else {
                nbCores++;
                lbCost++;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                }
                if (lbCost == ubCost) {
                    if (nbSatisfiable > 0) {
                        if (verbosity != MaxSATConfig.Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                    } else {
                        return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                    }
                }
                final LNGEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LNGResult.aborted(lowerBoundEvent);
                }
                if (!encoder.hasCardEncoding()) {
                    encoder.buildCardinality(solver, objFunction, lbCost);
                }
                final LNGIntVector join = new LNGIntVector();
                encoder.incUpdateCardinality(solver, join, objFunction, lbCost, assumptions);
            }
        }
    }

    protected LNGCoreSolver rebuildSolver() {
        final LNGCoreSolver s = newSATSolver();
        for (int i = 0; i < nVars(); i++) {
            newSATVariable(s);
        }
        for (int i = 0; i < nHard(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        LNGIntVector clause;
        for (int i = 0; i < nSoft(); i++) {
            clause = new LNGIntVector(softClauses.get(i).clause());
            for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                clause.push(softClauses.get(i).relaxationVars().get(j));
            }
            s.addClause(clause, null);
        }
        return s;
    }

    protected void initRelaxation() {
        for (int i = 0; i < nbSoft; i++) {
            final int l = newLiteral(false);
            softClauses.get(i).relaxationVars().push(l);
            softClauses.get(i).setAssumptionVar(l);
            objFunction.push(l);
        }
    }
}
