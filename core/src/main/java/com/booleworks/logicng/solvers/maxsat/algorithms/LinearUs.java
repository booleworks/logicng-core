// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.algorithms;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Linear search solver.
 * @version 3.0.0
 * @since 1.0
 */
public class LinearUs extends MaxSat {

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public LinearUs(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(config.cardinalityEncoding);
        if (problemType == ProblemType.WEIGHTED) {
            throw new IllegalStateException("Error: Currently LinearUS does not support weighted MaxSAT instances.");
        }
        switch (incrementalStrategy) {
            case NONE:
                return none(handler);
            case ITERATIVE:
                if (encoder.cardEncoding() != MaxSatConfig.CardinalityEncoding.TOTALIZER) {
                    throw new IllegalStateException(
                            "Error: Currently iterative encoding in LinearUS only supports the Totalizer encoding.");
                }
                return iterative(handler);
            default:
                throw new IllegalArgumentException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    protected LngResult<MaxSatResult> none(final ComputationHandler handler) {
        nbInitialVariables = nVars();
        final LngIntVector objFunction = new LngIntVector();
        initRelaxation(objFunction);
        LngCoreSolver solver = rebuildSolver();
        final LngIntVector assumptions = new LngIntVector();
        encoder.setIncremental(IncrementalStrategy.NONE);
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LngResult.canceled(upperBoundEvent);
                    }
                    if (encoder.cardEncoding() == MaxSatConfig.CardinalityEncoding.MTOTALIZER) {
                        encoder.setModulo((int) Math.ceil(Math.sqrt(ubCost + 1.0)));
                    }
                    encoder.encodeCardinality(solver, objFunction, 0);
                } else {
                    return optimum();
                }
            } else {
                lbCost++;
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return unsat();
                } else if (lbCost == ubCost) {
                    if (nbSatisfiable > 0) {
                        if (verbosity != MaxSatConfig.Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return optimum();
                    } else {
                        return unsat();
                    }
                } else {
                    final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LngResult.canceled(lowerBoundEvent);
                    }
                }
                solver = rebuildSolver();
                encoder.encodeCardinality(solver, objFunction, lbCost);
            }
        }
    }

    protected LngResult<MaxSatResult> iterative(final ComputationHandler handler) {
        assert encoder.cardEncoding() == MaxSatConfig.CardinalityEncoding.TOTALIZER;
        final LngIntVector objFunction = new LngIntVector();
        nbInitialVariables = nVars();
        initRelaxation(objFunction);
        final LngCoreSolver solver = rebuildSolver();
        final LngIntVector assumptions = new LngIntVector();
        encoder.setIncremental(IncrementalStrategy.ITERATIVE);
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
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
                    assert lbCost == ubCost;
                    return optimum();
                }
            } else {
                nbCores++;
                lbCost++;
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return unsat();
                }
                if (lbCost == ubCost) {
                    if (nbSatisfiable > 0) {
                        if (verbosity != MaxSatConfig.Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return optimum();
                    } else {
                        return unsat();
                    }
                }
                final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LngResult.canceled(lowerBoundEvent);
                }
                if (!encoder.hasCardEncoding()) {
                    encoder.buildCardinality(solver, objFunction, lbCost);
                }
                final LngIntVector join = new LngIntVector();
                encoder.incUpdateCardinality(solver, join, objFunction, lbCost, assumptions);
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
