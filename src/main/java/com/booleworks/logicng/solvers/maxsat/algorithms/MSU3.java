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
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.IncrementalStrategy;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;

import java.io.PrintStream;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Non-incremental MSU3 solver.
 * @version 2.1.0
 * @since 1.0
 */
public class MSU3 extends MaxSAT {

    protected final Encoder encoder;
    protected final IncrementalStrategy incrementalStrategy;
    protected final LNGIntVector objFunction;
    protected final SortedMap<Integer, Integer> coreMapping;
    protected final LNGBooleanVector activeSoft;
    protected final PrintStream output;
    protected MiniSatStyleSolver solver;

    /**
     * Constructs a new solver with default values.
     */
    public MSU3() {
        this(MaxSATConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param config the configuration
     */
    public MSU3(final MaxSATConfig config) {
        super(config);
        solver = null;
        verbosity = config.verbosity;
        incrementalStrategy = config.incrementalStrategy;
        encoder = new Encoder(config.cardinalityEncoding);
        objFunction = new LNGIntVector();
        coreMapping = new TreeMap<>();
        activeSoft = new LNGBooleanVector();
        output = config.output;
    }

    @Override
    public MaxSATResult search() {
        if (problemType == ProblemType.WEIGHTED) {
            throw new IllegalStateException(
                    "Error: Currently algorithm MSU3 does not support weighted MaxSAT instances.");
        }
        switch (incrementalStrategy) {
            case NONE:
                return none();
            case ITERATIVE:
                if (encoder.cardEncoding() != CardinalityEncoding.TOTALIZER) {
                    throw new IllegalStateException(
                            "Error: Currently iterative encoding in MSU3 only supports the Totalizer encoding.");
                }
                return iterative();
            default:
                throw new IllegalArgumentException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    protected MaxSATResult none() {
        nbInitialVariables = nVars();
        Tristate res;
        initRelaxation();
        solver = rebuildSolver();
        final LNGIntVector assumptions = new LNGIntVector();
        final LNGIntVector currentObjFunction = new LNGIntVector();
        encoder.setIncremental(IncrementalStrategy.NONE);
        activeSoft.growTo(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        while (true) {
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == Tristate.TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    if (!foundUpperBound(ubCost, null)) {
                        return MaxSATResult.UNDEF;
                    }
                    for (int i = 0; i < objFunction.size(); i++) {
                        assumptions.push(MiniSatStyleSolver.not(objFunction.get(i)));
                    }
                } else {
                    return MaxSATResult.OPTIMUM;
                }
            } else {
                lbCost++;
                nbCores++;
                if (verbosity != Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return MaxSATResult.UNSATISFIABLE;
                } else if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return MaxSATResult.OPTIMUM;
                } else if (!foundLowerBound(lbCost, null)) {
                    return MaxSATResult.UNDEF;
                }
                sumSizeCores += solver.conflict().size();
                for (int i = 0; i < solver.conflict().size(); i++) {
                    assert !activeSoft.get(coreMapping.get(solver.conflict().get(i)));
                    activeSoft.set(coreMapping.get(solver.conflict().get(i)), true);
                }
                currentObjFunction.clear();
                assumptions.clear();
                for (int i = 0; i < nSoft(); i++) {
                    if (activeSoft.get(i)) {
                        currentObjFunction.push(softClauses.get(i).relaxationVars().get(0));
                    } else {
                        assumptions.push(MiniSatStyleSolver.not(softClauses.get(i).assumptionVar()));
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

    protected MaxSATResult iterative() {
        if (encoder.cardEncoding() != CardinalityEncoding.TOTALIZER) {
            throw new IllegalStateException(
                    "Error: Currently algorithm MSU3 with iterative encoding only  supports the totalizer encoding.");
        }
        nbInitialVariables = nVars();
        Tristate res;
        initRelaxation();
        solver = rebuildSolver();
        final LNGIntVector assumptions = new LNGIntVector();
        final LNGIntVector joinObjFunction = new LNGIntVector();
        final LNGIntVector currentObjFunction = new LNGIntVector();
        final LNGIntVector encodingAssumptions = new LNGIntVector();
        encoder.setIncremental(IncrementalStrategy.ITERATIVE);
        activeSoft.growTo(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        while (true) {
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == Tristate.TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                saveModel(solver.model());
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + newCost);
                }
                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    if (!foundUpperBound(ubCost, null)) {
                        return MaxSATResult.UNDEF;
                    }
                    for (int i = 0; i < objFunction.size(); i++) {
                        assumptions.push(MiniSatStyleSolver.not(objFunction.get(i)));
                    }
                } else {
                    assert lbCost == newCost;
                    return MaxSATResult.OPTIMUM;
                }
            } else {
                lbCost++;
                nbCores++;
                if (verbosity != Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (nbSatisfiable == 0) {
                    return MaxSATResult.UNSATISFIABLE;
                }
                if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return MaxSATResult.OPTIMUM;
                }
                sumSizeCores += solver.conflict().size();
                if (solver.conflict().size() == 0) {
                    return MaxSATResult.UNSATISFIABLE;
                }
                if (!foundLowerBound(lbCost, null)) {
                    return MaxSATResult.UNDEF;
                }
                joinObjFunction.clear();
                for (int i = 0; i < solver.conflict().size(); i++) {
                    if (coreMapping.containsKey(solver.conflict().get(i))) {
                        assert !activeSoft.get(coreMapping.get(solver.conflict().get(i)));
                        activeSoft.set(coreMapping.get(solver.conflict().get(i)), true);
                        joinObjFunction.push(
                                softClauses.get(coreMapping.get(solver.conflict().get(i))).relaxationVars().get(0));
                    }
                }
                currentObjFunction.clear();
                assumptions.clear();
                for (int i = 0; i < nSoft(); i++) {
                    if (activeSoft.get(i)) {
                        currentObjFunction.push(softClauses.get(i).relaxationVars().get(0));
                    } else {
                        assumptions.push(MiniSatStyleSolver.not(softClauses.get(i).assumptionVar()));
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

    protected MiniSatStyleSolver rebuildSolver() {
        final MiniSatStyleSolver s = newSATSolver();
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
