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

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.CardinalityEncoding;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.solvers.datastructures.LNGSoftClause;
import com.booleworks.logicng.solvers.maxsat.InternalMaxSATResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.io.PrintStream;

/**
 * Linear search solver with Boolean Multilevel Optimization (BMO)
 * @version 2.1.0
 * @since 1.0
 */
public class LinearSU extends MaxSAT {

    protected Encoder encoder;
    protected final boolean bmoMode; // Enables BMO mode.
    // Literals to be used in the constraint that excludes models.
    protected final LNGIntVector objFunction;
    // Coefficients of the literals that are used in the constraint that
    // excludes models.
    protected final LNGIntVector coeffs;
    protected final PrintStream output;
    protected LNGCoreSolver solver;
    protected boolean isBmo; // Stores if the formula is BMO or not.

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public LinearSU(final FormulaFactory f) {
        this(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public LinearSU(final FormulaFactory f, final MaxSATConfig config) {
        super(f, config);
        solver = null;
        verbosity = config.verbosity;
        bmoMode = config.bmo;
        isBmo = false;
        objFunction = new LNGIntVector();
        coeffs = new LNGIntVector();
        output = config.output;
    }

    @Override
    protected LNGResult<InternalMaxSATResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPBEncoding(config.pbEncoding);
        nbInitialVariables = nVars();
        if (currentWeight == 1) {
            problemType = ProblemType.UNWEIGHTED;
        } else {
            isBmo = isBMO(true);
        }
        objFunction.clear();
        coeffs.clear();
        if (problemType == ProblemType.WEIGHTED) {
            if (bmoMode && isBmo) {
                return bmoSearch(handler);
            } else {
                return normalSearch(handler);
            }
        } else {
            return normalSearch(handler);
        }
    }

    protected LNGResult<InternalMaxSATResult> bmoSearch(final ComputationHandler handler) {
        assert orderWeights.size() > 0;
        initRelaxation();
        int currentWeight = orderWeights.get(0);
        final int minWeight = orderWeights.get(orderWeights.size() - 1);
        int posWeight = 0;
        final LNGVector<LNGIntVector> functions = new LNGVector<>();
        final LNGIntVector weights = new LNGIntVector();
        solver = rebuildBMO(functions, weights, currentWeight);
        int localCost = 0;
        ubCost = 0;
        while (true) {
            final LNGResult<Boolean> res = searchSATSolver(solver, handler);
            if (!res.isSuccess()) {
                return LNGResult.canceled(res.getCancelCause());
            }
            if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), currentWeight);
                if (currentWeight == minWeight) {
                    saveModel(solver.model());
                    if (verbosity != Verbosity.NONE) {
                        output.println("o " + (newCost + lbCost));
                    }
                    ubCost = newCost + lbCost;
                    if (newCost > 0) {
                        final LNGEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LNGResult.canceled(upperBoundEvent);
                        }
                    }
                } else if (verbosity != Verbosity.NONE) {
                    output.printf("c BMO-UB : %d (Function %d/%d)%n", newCost, posWeight + 1, orderWeights.size());
                }
                if (newCost == 0 && currentWeight == minWeight) {
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                } else {
                    if (newCost == 0) {
                        functions.push(new LNGIntVector(objFunction));
                        localCost = newCost;
                        weights.push(0);
                        posWeight++;
                        currentWeight = orderWeights.get(posWeight);
                        solver = rebuildBMO(functions, weights, currentWeight);
                        if (verbosity != Verbosity.NONE) {
                            output.println("c LB : " + lbCost);
                        }
                    } else {
                        if (localCost == 0) {
                            encoder.encodeCardinality(solver, objFunction, newCost / currentWeight - 1);
                        } else {
                            encoder.updateCardinality(solver, newCost / currentWeight - 1);
                        }
                        localCost = newCost;
                    }
                }
            } else {
                nbCores++;
                if (currentWeight == minWeight) {
                    if (model.size() == 0) {
                        assert nbSatisfiable == 0;
                        return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                    } else {
                        return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                    }
                } else {
                    functions.push(new LNGIntVector(objFunction));
                    weights.push(localCost / currentWeight);
                    lbCost += localCost;
                    posWeight++;
                    currentWeight = orderWeights.get(posWeight);
                    localCost = 0;
                    final LNGEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LNGResult.canceled(lowerBoundEvent);
                    }
                    solver = rebuildBMO(functions, weights, currentWeight);
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB : " + lbCost);
                    }
                }
            }
        }
    }

    protected LNGResult<InternalMaxSATResult> normalSearch(final ComputationHandler handler) {
        initRelaxation();
        solver = rebuildSolver(1);
        while (true) {
            final LNGResult<Boolean> res = searchSATSolver(solver, handler);
            if (!res.isSuccess()) {
                return LNGResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                if (newCost == 0) {
                    ubCost = newCost;
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                } else {
                    if (problemType == ProblemType.WEIGHTED) {
                        if (!encoder.hasPBEncoding()) {
                            encoder.encodePB(solver, objFunction, coeffs, newCost - 1);
                        } else {
                            encoder.updatePB(solver, newCost - 1);
                        }
                    } else {
                        if (!encoder.hasCardEncoding()) {
                            encoder.encodeCardinality(solver, objFunction, newCost - 1);
                        } else {
                            encoder.updateCardinality(solver, newCost - 1);
                        }
                    }
                    ubCost = newCost;
                    final LNGEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LNGResult.canceled(upperBoundEvent);
                    }
                }
            } else {
                nbCores++;
                if (model.size() == 0) {
                    assert nbSatisfiable == 0;
                    return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                } else {
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
            }
        }
    }

    /**
     * Rebuilds a SAT solver with the current MaxSAT formula.
     * @param minWeight the minimal weight
     * @return the rebuilt solver
     */
    protected LNGCoreSolver rebuildSolver(final int minWeight) {
        final LNGBooleanVector seen = new LNGBooleanVector(nVars());
        seen.growTo(nVars(), false);
        final LNGCoreSolver s = newSATSolver();
        for (int i = 0; i < nVars(); i++) {
            newSATVariable(s);
        }
        for (int i = 0; i < hardClauses.size(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() < minWeight) {
                continue;
            }
            final LNGIntVector clause = new LNGIntVector(softClauses.get(i).clause());
            for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                clause.push(softClauses.get(i).relaxationVars().get(j));
            }
            s.addClause(clause, null);
        }
        return s;
    }

    /**
     * Rebuilds a SAT solver with the current MaxSAT formula
     * @param functions     the functions
     * @param rhs           the right-hand side
     * @param currentWeight the current weight
     * @return the rebuilt solver
     */
    protected LNGCoreSolver rebuildBMO(final LNGVector<LNGIntVector> functions, final LNGIntVector rhs,
                                       final int currentWeight) {
        assert functions.size() == rhs.size();
        final LNGCoreSolver s = rebuildSolver(currentWeight);
        objFunction.clear();
        coeffs.clear();
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() == currentWeight) {
                objFunction.push(softClauses.get(i).relaxationVars().get(0));
                coeffs.push(softClauses.get(i).weight());
            }
        }
        for (int i = 0; i < functions.size(); i++) {
            encoder.encodeCardinality(s, functions.get(i), rhs.get(i));
        }
        return s;
    }

    /**
     * Initializes the relaxation variables by adding a fresh variable to the
     * 'relaxationVars' of each soft clause.
     */
    protected void initRelaxation() {
        for (final LNGSoftClause softClause : softClauses) {
            final int l = newLiteral(false);
            softClause.relaxationVars().push(l);
            objFunction.push(l);
            coeffs.push(softClause.weight());
        }
    }
}
