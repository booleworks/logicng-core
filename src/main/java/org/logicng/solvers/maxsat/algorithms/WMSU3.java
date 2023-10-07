// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Open-WBO -- Copyright (c) 2013-2015, Ruben Martins, Vasco Manquinho, Ines Lynce
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.logicng.solvers.maxsat.algorithms;

import static org.logicng.datastructures.Tristate.TRUE;
import static org.logicng.handlers.Handler.aborted;
import static org.logicng.solvers.maxsat.algorithms.MaxSATConfig.CardinalityEncoding;
import static org.logicng.solvers.maxsat.algorithms.MaxSATConfig.IncrementalStrategy;
import static org.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity;
import static org.logicng.solvers.sat.MiniSatStyleSolver.not;

import org.logicng.collections.LNGBooleanVector;
import org.logicng.collections.LNGIntVector;
import org.logicng.collections.LNGVector;
import org.logicng.datastructures.Tristate;
import org.logicng.handlers.SATHandler;
import org.logicng.solvers.maxsat.encodings.Encoder;
import org.logicng.solvers.sat.MiniSatStyleSolver;

import java.io.PrintStream;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The weighted MSU3 algorithm.
 * @version 2.1.0
 * @since 1.0
 */
public class WMSU3 extends MaxSAT {

    final boolean bmoStrategy;
    final protected Encoder encoder;
    final protected IncrementalStrategy incrementalStrategy;
    final protected LNGIntVector assumptions;
    final protected LNGIntVector objFunction;
    final protected LNGIntVector coeffs;
    final protected SortedMap<Integer, Integer> coreMapping;
    final protected LNGBooleanVector activeSoft;
    final protected PrintStream output;
    boolean isBmo;
    protected MiniSatStyleSolver solver;

    /**
     * Constructs a new solver with default values.
     */
    public WMSU3() {
        this(MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param config the configuration
     */
    public WMSU3(final MaxSATConfig config) {
        super(config);
        solver = null;
        verbosity = config.verbosity;
        incrementalStrategy = config.incrementalStrategy;
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPBEncoding(config.pbEncoding);
        bmoStrategy = config.bmo;
        isBmo = false;
        assumptions = new LNGIntVector();
        objFunction = new LNGIntVector();
        coeffs = new LNGIntVector();
        coreMapping = new TreeMap<>();
        activeSoft = new LNGBooleanVector();
        output = config.output;
    }

    protected static boolean subsetSum(final LNGIntVector set, final int sum) {
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
    public MaxSATResult search() {
        if (problemType == ProblemType.UNWEIGHTED) {
            throw new IllegalStateException("Error: Currently algorithm WMSU3 does not support unweighted MaxSAT instances.");
        }
        if (bmoStrategy) {
            isBmo = isBMO(true);
        }
        if (!isBmo) {
            currentWeight = 1;
        }
        switch (incrementalStrategy) {
            case NONE:
                return none();
            case ITERATIVE:
                if (isBmo) {
                    if (encoder.cardEncoding() != CardinalityEncoding.TOTALIZER) {
                        throw new IllegalStateException("Error: Currently iterative encoding in WMSU3 only supports the Totalizer encoding.");
                    }
                    return iterativeBmo();
                } else {
                    return iterative();
                }
            default:
                throw new IllegalArgumentException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    protected MaxSATResult iterative() {
        nbInitialVariables = nVars();
        Tristate res;
        initRelaxation();
        solver = rebuildSolver();
        encoder.setIncremental(IncrementalStrategy.ITERATIVE);
        activeSoft.growTo(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        assumptions.clear();
        final LNGIntVector fullObjFunction = new LNGIntVector();
        final LNGIntVector fullCoeffsFunction = new LNGIntVector();
        while (true) {
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(solver.model());
                    if (verbosity != Verbosity.NONE) {
                        output.println("o " + newCost);
                    }
                    ubCost = newCost;
                }
                if (ubCost == 0 || lbCost == ubCost || (currentWeight == 1 && nbSatisfiable > 1)) {
                    assert lbCost == ubCost;
                    assert nbSatisfiable > 0;
                    return MaxSATResult.OPTIMUM;
                } else if (!foundUpperBound(ubCost, null)) {
                    return MaxSATResult.UNDEF;
                }
                for (int i = 0; i < nSoft(); i++) {
                    if (softClauses.get(i).weight() >= currentWeight && !activeSoft.get(i)) {
                        assumptions.push(not(softClauses.get(i).assumptionVar()));
                    }
                }
            } else {
                nbCores++;
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
                objFunction.clear();
                coeffs.clear();
                assumptions.clear();
                for (int i = 0; i < solver.conflict().size(); i++) {
                    if (!coreMapping.containsKey(solver.conflict().get(i))) {
                        continue;
                    }
                    final int indexSoft = coreMapping.get(solver.conflict().get(i));
                    if (!activeSoft.get(indexSoft)) {
                        activeSoft.set(indexSoft, true);
                        objFunction.push(softClauses.get(indexSoft).relaxationVars().get(0));
                        coeffs.push(softClauses.get(indexSoft).weight());
                    }
                }
                for (int i = 0; i < nSoft(); i++) {
                    if (!activeSoft.get(i) && softClauses.get(i).weight() >= currentWeight) {
                        assumptions.push(not(softClauses.get(i).assumptionVar()));
                    }
                }
                for (int i = 0; i < coeffs.size(); i++) {
                    fullCoeffsFunction.push(coeffs.get(i));
                    fullObjFunction.push(objFunction.get(i));
                }
                if (verbosity != Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", fullCoeffsFunction.size(), nSoft());
                }
                lbCost++;
                while (!subsetSum(fullCoeffsFunction, lbCost)) {
                    lbCost++;
                }
                if (verbosity != Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                if (!encoder.hasPBEncoding()) {
                    encoder.incEncodePB(solver, objFunction, coeffs, lbCost, assumptions, nSoft());
                } else {
                    encoder.incUpdatePB(solver, objFunction, coeffs, lbCost);
                    encoder.incUpdatePBAssumptions(assumptions);
                }
            }
        }
    }

    protected MaxSATResult none() {
        nbInitialVariables = nVars();
        Tristate res;
        initRelaxation();
        solver = rebuildSolver();
        encoder.setIncremental(IncrementalStrategy.NONE);
        activeSoft.growTo(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        assumptions.clear();
        while (true) {
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(solver.model());
                    if (verbosity != Verbosity.NONE) {
                        output.println("o " + newCost);
                    }
                    ubCost = newCost;
                }
                if (ubCost == 0 || lbCost == ubCost || (currentWeight == 1 && nbSatisfiable > 1)) {
                    assert nbSatisfiable > 0;
                    return MaxSATResult.OPTIMUM;
                } else if (!foundUpperBound(ubCost, null)) {
                    return MaxSATResult.UNDEF;
                }
                for (int i = 0; i < nSoft(); i++) {
                    if (softClauses.get(i).weight() >= currentWeight && !activeSoft.get(i)) {
                        assumptions.push(not(softClauses.get(i).assumptionVar()));
                    }
                }
            } else {
                nbCores++;
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
                    final int indexSoft = coreMapping.get(solver.conflict().get(i));
                    assert !activeSoft.get(indexSoft);
                    activeSoft.set(indexSoft, true);
                }
                objFunction.clear();
                coeffs.clear();
                assumptions.clear();
                for (int i = 0; i < nSoft(); i++) {
                    if (activeSoft.get(i)) {
                        objFunction.push(softClauses.get(i).relaxationVars().get(0));
                        coeffs.push(softClauses.get(i).weight());
                    } else if (softClauses.get(i).weight() >= currentWeight) {
                        assumptions.push(not(softClauses.get(i).assumptionVar()));
                    }
                }
                if (verbosity != Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", objFunction.size(), nSoft());
                }
                solver = rebuildSolver();
                lbCost++;
                while (!subsetSum(coeffs, lbCost)) {
                    lbCost++;
                }
                if (verbosity != Verbosity.NONE) {
                    output.println("c LB : " + lbCost);
                }
                encoder.encodePB(solver, objFunction, coeffs, lbCost);
            }
        }
    }

    protected MaxSATResult iterativeBmo() {
        assert isBmo;
        nbInitialVariables = nVars();
        Tristate res;
        initRelaxation();
        solver = rebuildSolver();
        encoder.setIncremental(IncrementalStrategy.ITERATIVE);
        final LNGIntVector joinObjFunction = new LNGIntVector();
        final LNGIntVector encodingAssumptions = new LNGIntVector();
        final LNGIntVector joinCoeffs = new LNGIntVector();
        activeSoft.growTo(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }
        int minWeight = 0;
        int posWeight = 0;
        int localCost = 0;
        final LNGVector<LNGIntVector> functions = new LNGVector<>();
        final LNGIntVector weights = new LNGIntVector();
        final LNGVector<Encoder> bmoEncodings = new LNGVector<>();
        final LNGBooleanVector firstEncoding = new LNGBooleanVector();
        functions.push(new LNGIntVector());
        weights.push(0);
        assert objFunction.size() == 0;
        Encoder e = new Encoder(CardinalityEncoding.TOTALIZER);
        e.setIncremental(IncrementalStrategy.ITERATIVE);
        bmoEncodings.push(e);
        firstEncoding.push(true);
        while (true) {
            final SATHandler satHandler = satHandler();
            res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == TRUE) {
                nbSatisfiable++;
                final int newCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(solver.model());
                    if (verbosity != Verbosity.NONE) {
                        output.println("o " + newCost);
                    }
                    ubCost = newCost;
                }
                if (nbSatisfiable == 1) {
                    if (ubCost == 0) {
                        return MaxSATResult.OPTIMUM;
                    } else if (!foundUpperBound(ubCost, null)) {
                        return MaxSATResult.UNDEF;
                    }
                    assert orderWeights.size() > 0;
                    assert orderWeights.get(0) > 1;
                    minWeight = orderWeights.get(orderWeights.size() - 1);
                    currentWeight = orderWeights.get(0);
                    for (int i = 0; i < nSoft(); i++) {
                        if (softClauses.get(i).weight() >= currentWeight) {
                            assumptions.push(not(softClauses.get(i).assumptionVar()));
                        }
                    }
                } else {
                    if (currentWeight == 1 || currentWeight == minWeight) {
                        return MaxSATResult.OPTIMUM;
                    } else {
                        if (!foundUpperBound(ubCost, null)) {
                            return MaxSATResult.UNDEF;
                        }
                        assumptions.clear();
                        final int previousWeight = currentWeight;
                        posWeight++;
                        assert posWeight < orderWeights.size();
                        currentWeight = orderWeights.get(posWeight);
                        if (objFunction.size() > 0) {
                            functions.set(functions.size() - 1, new LNGIntVector(objFunction));
                        }
                        functions.push(new LNGIntVector());
                        weights.push(0);
                        localCost = 0;
                        e = new Encoder(CardinalityEncoding.TOTALIZER);
                        e.setIncremental(IncrementalStrategy.ITERATIVE);
                        bmoEncodings.push(e);
                        firstEncoding.push(true);
                        for (int i = 0; i < encodingAssumptions.size(); i++) {
                            solver.addClause(encodingAssumptions.get(i), null);
                        }
                        encodingAssumptions.clear();
                        for (int i = 0; i < nSoft(); i++) {
                            if (!activeSoft.get(i) && previousWeight == softClauses.get(i).weight()) {
                                solver.addClause(not(softClauses.get(i).assumptionVar()), null);
                            }
                            if (currentWeight == softClauses.get(i).weight()) {
                                assumptions.push(not(softClauses.get(i).assumptionVar()));
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
                joinObjFunction.clear();
                joinCoeffs.clear();
                for (int i = 0; i < solver.conflict().size(); i++) {
                    if (coreMapping.containsKey(solver.conflict().get(i))) {
                        if (activeSoft.get(coreMapping.get(solver.conflict().get(i)))) {
                            continue;
                        }
                        assert softClauses.get(coreMapping.get(solver.conflict().get(i))).weight() == currentWeight;
                        activeSoft.set(coreMapping.get(solver.conflict().get(i)), true);
                        joinObjFunction.push(softClauses.get(coreMapping.get(solver.conflict().get(i))).relaxationVars().get(0));
                        joinCoeffs.push(softClauses.get(coreMapping.get(solver.conflict().get(i))).weight());
                    }
                }
                objFunction.clear();
                coeffs.clear();
                assumptions.clear();
                for (int i = 0; i < nSoft(); i++) {
                    if (activeSoft.get(i)) {
                        assert softClauses.get(i).weight() == currentWeight;
                        objFunction.push(softClauses.get(i).relaxationVars().get(0));
                        coeffs.push(softClauses.get(i).weight());
                    } else if (currentWeight == softClauses.get(i).weight()) {
                        assumptions.push(not(softClauses.get(i).assumptionVar()));
                    }
                }
                if (verbosity != Verbosity.NONE) {
                    output.printf("c Relaxed soft clauses %d / %d%n", objFunction.size(), nSoft());
                }
                assert posWeight < functions.size();
                functions.set(posWeight, new LNGIntVector(objFunction));
                weights.set(posWeight, localCost);
                if (firstEncoding.get(posWeight)) {
                    if (weights.get(posWeight) != objFunction.size()) {
                        bmoEncodings.get(posWeight).buildCardinality(solver, objFunction, weights.get(posWeight));
                        joinObjFunction.clear();
                        bmoEncodings.get(posWeight).incUpdateCardinality(solver, joinObjFunction, objFunction, weights.get(posWeight), encodingAssumptions);
                        firstEncoding.set(posWeight, false);
                    }
                } else {
                    bmoEncodings.get(posWeight).incUpdateCardinality(solver, joinObjFunction, objFunction, weights.get(posWeight), encodingAssumptions);
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
        }
    }
}
