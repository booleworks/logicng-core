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

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Verbosity;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.datastructures.LngSoftClause;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Linear search solver with Boolean Multilevel Optimization (BMO)
 * @version 3.0.0
 * @since 1.0
 */
public class LinearSu extends MaxSat {

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public LinearSu(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPbEncoding(config.pbEncoding);
        nbInitialVariables = nVars();
        if (problemType == ProblemType.WEIGHTED) {
            if (config.bmo && isBmo(true)) {
                return bmoSearch(handler);
            } else {
                return normalSearch(handler);
            }
        } else {
            return normalSearch(handler);
        }
    }

    protected LngResult<MaxSatResult> bmoSearch(final ComputationHandler handler) {
        assert !orderWeights.isEmpty();
        final LngIntVector objFunction = new LngIntVector();
        final LngIntVector coeffs = new LngIntVector();
        initRelaxation(objFunction, coeffs);
        int currentWeight = orderWeights.get(0);
        final int minWeight = orderWeights.get(orderWeights.size() - 1);
        int posWeight = 0;
        final LngVector<LngIntVector> functions = new LngVector<>();
        final LngIntVector weights = new LngIntVector();
        LngCoreSolver solver = rebuildBmo(functions, weights, currentWeight, objFunction, coeffs);
        int localCost = 0;
        ubCost = 0;
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
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
                        final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LngResult.canceled(upperBoundEvent);
                        }
                    }
                } else if (verbosity != Verbosity.NONE) {
                    output.printf("c BMO-UB : %d (Function %d/%d)%n", newCost, posWeight + 1, orderWeights.size());
                }
                if (newCost == 0 && currentWeight == minWeight) {
                    return optimum();
                } else {
                    if (newCost == 0) {
                        functions.push(new LngIntVector(objFunction));
                        localCost = newCost;
                        weights.push(0);
                        posWeight++;
                        currentWeight = orderWeights.get(posWeight);
                        solver = rebuildBmo(functions, weights, currentWeight, objFunction, coeffs);
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
                    if (nbSatisfiable == 0) {
                        return unsat();
                    } else {
                        return optimum();
                    }
                } else {
                    functions.push(new LngIntVector(objFunction));
                    weights.push(localCost / currentWeight);
                    lbCost += localCost;
                    posWeight++;
                    currentWeight = orderWeights.get(posWeight);
                    localCost = 0;
                    final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LngResult.canceled(lowerBoundEvent);
                    }
                    solver = rebuildBmo(functions, weights, currentWeight, objFunction, coeffs);
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB : " + lbCost);
                    }
                }
            }
        }
    }

    protected LngResult<MaxSatResult> normalSearch(final ComputationHandler handler) {
        final LngIntVector objFunction = new LngIntVector();
        final LngIntVector coeffs = new LngIntVector();
        initRelaxation(objFunction, coeffs);
        final LngCoreSolver solver = rebuildSolver(1);
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                if (newCost == 0) {
                    ubCost = newCost;
                    return optimum();
                } else {
                    if (problemType == ProblemType.WEIGHTED) {
                        if (!encoder.hasPbEncoding()) {
                            encoder.encodePb(solver, objFunction, coeffs, newCost - 1);
                        } else {
                            encoder.updatePb(solver, newCost - 1);
                        }
                    } else {
                        if (!encoder.hasCardEncoding()) {
                            encoder.encodeCardinality(solver, objFunction, newCost - 1);
                        } else {
                            encoder.updateCardinality(solver, newCost - 1);
                        }
                    }
                    ubCost = newCost;
                    final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                    if (upperBoundEvent != null) {
                        return LngResult.canceled(upperBoundEvent);
                    }
                }
            } else {
                nbCores++;
                if (nbSatisfiable == 0) {
                    return unsat();
                } else {
                    return optimum();
                }
            }
        }
    }

    /**
     * Rebuilds a SAT solver with the current MaxSAT formula.
     * @param minWeight the minimal weight
     * @return the rebuilt solver
     */
    protected LngCoreSolver rebuildSolver(final int minWeight) {
        final LngBooleanVector seen = new LngBooleanVector(nVars());
        seen.growTo(nVars(), false);
        final LngCoreSolver s = newSatSolver();
        for (int i = 0; i < nVars(); i++) {
            newSatVariable(s);
        }
        for (int i = 0; i < hardClauses.size(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() < minWeight) {
                continue;
            }
            final LngIntVector clause = new LngIntVector(softClauses.get(i).clause());
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
    protected LngCoreSolver rebuildBmo(
            final LngVector<LngIntVector> functions,
            final LngIntVector rhs,
            final int currentWeight,
            final LngIntVector objFunction,
            final LngIntVector coeffs) {
        assert functions.size() == rhs.size();
        final LngCoreSolver s = rebuildSolver(currentWeight);
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
    protected void initRelaxation(final LngIntVector objFunction, final LngIntVector coeffs) {
        for (final LngSoftClause softClause : softClauses) {
            final int l = newLiteral(false);
            softClause.relaxationVars().push(l);
            objFunction.push(l);
            coeffs.push(softClause.weight());
        }
    }
}
