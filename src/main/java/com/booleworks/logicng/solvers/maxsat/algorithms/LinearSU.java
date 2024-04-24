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

import static com.booleworks.logicng.handlers.Handler.aborted;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.CardinalityEncoding;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.solvers.datastructures.LNGSoftClause;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.io.PrintStream;

/**
 * Linear search solver with Boolean Multilevel Optimization (BMO)
 * @version 2.1.0
 * @since 1.0
 */
public class LinearSU extends MaxSAT {

    protected final Encoder encoder;
    protected final boolean bmoMode; // Enables BMO mode.
    protected final LNGIntVector objFunction; // Literals to be used in the
                                              // constraint that excludes
                                              // models.
    protected final LNGIntVector coeffs; // Coefficients of the literals that
                                         // are used in the constraint that
                                         // excludes models.
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
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPBEncoding(config.pbEncoding);
        verbosity = config.verbosity;
        bmoMode = config.bmo;
        isBmo = false;
        objFunction = new LNGIntVector();
        coeffs = new LNGIntVector();
        output = config.output;
    }

    @Override
    public MaxSATResult search() {
        nbInitialVariables = nVars();
        if (currentWeight == 1) {
            problemType = ProblemType.UNWEIGHTED;
        } else {
            isBmo = isBMO(true);
        }
        if (problemType == ProblemType.WEIGHTED) {
            if (bmoMode && isBmo) {
                return bmoSearch();
            } else {
                return normalSearch();
            }
        } else {
            return normalSearch();
        }
    }

    protected MaxSATResult bmoSearch() {
        assert orderWeights.size() > 0;
        Tristate res;
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
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            }
            if (res == Tristate.TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), currentWeight);
                if (currentWeight == minWeight) {
                    saveModel(solver.model());
                    if (verbosity != Verbosity.NONE) {
                        output.println("o " + (newCost + lbCost));
                    }
                    ubCost = newCost + lbCost;
                    if (newCost > 0 && !foundUpperBound(ubCost, null)) {
                        return MaxSATResult.UNDEF;
                    }
                } else if (verbosity != Verbosity.NONE) {
                    output.printf("c BMO-UB : %d (Function %d/%d)%n", newCost, posWeight + 1, orderWeights.size());
                }
                if (newCost == 0 && currentWeight == minWeight) {
                    return MaxSATResult.OPTIMUM;
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
                        return MaxSATResult.UNSATISFIABLE;
                    } else {
                        return MaxSATResult.OPTIMUM;
                    }
                } else {
                    functions.push(new LNGIntVector(objFunction));
                    weights.push(localCost / currentWeight);
                    lbCost += localCost;
                    posWeight++;
                    currentWeight = orderWeights.get(posWeight);
                    localCost = 0;
                    if (!foundLowerBound(lbCost, null)) {
                        return MaxSATResult.UNDEF;
                    }
                    solver = rebuildBMO(functions, weights, currentWeight);
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB : " + lbCost);
                    }
                }
            }
        }
    }

    protected MaxSATResult normalSearch() {
        Tristate res;
        initRelaxation();
        solver = rebuildSolver(1);
        while (true) {
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == Tristate.TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                if (newCost == 0) {
                    ubCost = newCost;
                    return MaxSATResult.OPTIMUM;
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
                    if (!foundUpperBound(ubCost, null)) {
                        return MaxSATResult.UNDEF;
                    }
                }
            } else {
                nbCores++;
                if (model.size() == 0) {
                    assert nbSatisfiable == 0;
                    return MaxSATResult.UNSATISFIABLE;
                } else {
                    return MaxSATResult.OPTIMUM;
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
        for (int i = 0; i < nHard(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        for (int i = 0; i < nSoft(); i++) {
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
        for (int i = 0; i < nSoft(); i++) {
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
