// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.algorithms;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * OLL Solver.
 * <p>
 * Based on "Unsatisfiability-based optimization in clasp*" by Andres, Kaufmann,
 * Matheis, and Schaub.
 * @version 3.0.0
 * @since 2.4.0
 */
public class Oll extends MaxSat {
    private Encoder encoder;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public Oll(final FormulaFactory f) {
        this(f, MaxSatConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public Oll(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
        verbosity = config.verbosity;
        if (config.cardinalityEncoding != MaxSatConfig.CardinalityEncoding.TOTALIZER) {
            throw new IllegalStateException("Error: Currently OLL only supports the totalizer encoding.");
        }
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPbEncoding(config.pbEncoding);
        if (problemType == ProblemType.WEIGHTED) {
            return weighted(handler);
        } else {
            return unweighted(handler);
        }
    }

    private LngCoreSolver rebuildSolver() {
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

    private LngResult<MaxSatResult> unweighted(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        final SortedMap<Integer, IntTriple> boundMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        final LngCoreSolver solver = rebuildSolver();

        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector joinObjFunction = new LngIntVector();
        final LngIntVector encodingAssumptions = new LngIntVector();
        encoder.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);

        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }

        final LinkedHashSet<Integer> cardinalityAssumptions = new LinkedHashSet<>();
        final LngVector<Encoder> softCardinality = new LngVector<>();

        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final LngBooleanVector model = solver.model();
                final int newCost = computeCostModel(model, Integer.MAX_VALUE);
                saveModel(model);

                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    if (newCost == 0) {
                        return optimum();
                    }
                    for (int i = 0; i < softClauses.size(); i++) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                } else {
                    assert lbCost == newCost;
                    return optimum();
                }
            } else {
                lbCost++;
                nbCores++;
                if (nbSatisfiable == 0) {
                    return unsat();
                }
                if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    return optimum();
                }
                sumSizeCores += solver.assumptionsConflict().size();
                final LngIntVector softRelax = new LngIntVector();
                final LngIntVector cardinalityRelax = new LngIntVector();

                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    final int p = solver.assumptionsConflict().get(i);
                    if (coreMapping.containsKey(p)) {
                        assert !activeSoft.get(coreMapping.get(p));
                        activeSoft.set(coreMapping.get(solver.assumptionsConflict().get(i)), true);
                        assert p == softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i)))
                                .relaxationVars().get(0);
                        softRelax.push(p);
                    }

                    if (boundMapping.containsKey(p)) {
                        assert cardinalityAssumptions.contains(p);
                        cardinalityAssumptions.remove(p);
                        cardinalityRelax.push(p);

                        // this is a soft cardinality -- bound must be increased
                        final IntTriple softId = boundMapping.get(solver.assumptionsConflict().get(i));
                        // // increase the bound
                        assert softId.id < softCardinality.size();
                        assert softCardinality.get(softId.id).hasCardEncoding();

                        joinObjFunction.clear();
                        encodingAssumptions.clear();
                        softCardinality.get(softId.id).incUpdateCardinality(solver, joinObjFunction,
                                softCardinality.get(softId.id).lits(),
                                softId.bound + 1, encodingAssumptions);

                        // if the bound is the same as the number of literals
                        // then no restriction is applied
                        if (softId.bound + 1 < softCardinality.get(softId.id).outputs().size()) {
                            assert softCardinality.get(softId.id).outputs().size() > softId.bound + 1;
                            final int out = softCardinality.get(softId.id).outputs().get(softId.bound + 1);
                            boundMapping.put(out, new IntTriple(softId.id, softId.bound + 1, 1));
                            cardinalityAssumptions.add(out);
                        }
                    }
                }

                assert softRelax.size() + cardinalityRelax.size() > 0;
                if (softRelax.size() == 1 && cardinalityRelax.size() == 0) {
                    solver.addClause(softRelax.get(0), null);
                }

                if (softRelax.size() + cardinalityRelax.size() > 1) {
                    final LngIntVector relaxHarden = new LngIntVector(softRelax);
                    for (int i = 0; i < cardinalityRelax.size(); i++) {
                        relaxHarden.push(cardinalityRelax.get(i));
                    }
                    final Encoder e = new Encoder(MaxSatConfig.CardinalityEncoding.TOTALIZER);
                    e.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
                    e.buildCardinality(solver, relaxHarden, 1);
                    softCardinality.push(e);
                    assert e.outputs().size() > 1;

                    final int out = e.outputs().get(1);
                    boundMapping.put(out, new IntTriple(softCardinality.size() - 1, 1, 1));
                    cardinalityAssumptions.add(out);
                }

                // reset the assumptions
                assumptions.clear();
                for (int i = 0; i < softClauses.size(); i++) {
                    if (!activeSoft.get(i)) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                for (final Integer it : cardinalityAssumptions) {
                    assumptions.push(LngCoreSolver.not(it));
                }
            }
        }
    }

    private LngResult<MaxSatResult> weighted(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        final SortedMap<Integer, IntTriple> boundMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        final LngCoreSolver solver = rebuildSolver();

        final LngIntVector assumptions = new LngIntVector();
        final LngIntVector joinObjFunction = new LngIntVector();
        final LngIntVector encodingAssumptions = new LngIntVector();
        encoder.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);

        final LngBooleanVector activeSoft = new LngBooleanVector(softClauses.size(), false);
        for (int i = 0; i < softClauses.size(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }

        final LinkedHashSet<Integer> cardinalityAssumptions = new LinkedHashSet<>();
        final LngVector<Encoder> softCardinality = new LngVector<>();
        int minWeight = currentWeight;

        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final LngBooleanVector model = solver.model();
                final int newCost = computeCostModel(model, Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(model);
                    ubCost = newCost;
                }
                if (nbSatisfiable == 1) {
                    minWeight = findNextWeightDiversity(minWeight, cardinalityAssumptions, boundMapping);
                    for (int i = 0; i < softClauses.size(); i++) {
                        if (softClauses.get(i).weight() >= minWeight) {
                            assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                        }
                    }
                } else {
                    // compute min weight in soft
                    int notConsidered = 0;
                    for (int i = 0; i < softClauses.size(); i++) {
                        if (softClauses.get(i).weight() < minWeight) {
                            notConsidered++;
                        }
                    }
                    for (final Integer it : cardinalityAssumptions) {
                        final IntTriple softId = boundMapping.get(it);
                        assert softId != null;
                        if (softId.weight < minWeight) {
                            notConsidered++;
                        }
                    }
                    if (notConsidered != 0) {
                        minWeight = findNextWeightDiversity(minWeight, cardinalityAssumptions, boundMapping);
                        assumptions.clear();
                        for (int i = 0; i < softClauses.size(); i++) {
                            if (!activeSoft.get(i) && softClauses.get(i).weight() >= minWeight) {
                                assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                            }
                        }
                        for (final Integer it : cardinalityAssumptions) {
                            final IntTriple softId = boundMapping.get(it);
                            assert softId != null;
                            if (softId.weight >= minWeight) {
                                assumptions.push(LngCoreSolver.not(it));
                            }
                        }
                    } else {
                        assert lbCost == newCost;
                        return optimum();
                    }
                }
            } else {
                // reduce the weighted to the unweighted case
                int minCore = Integer.MAX_VALUE;
                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    final int p = solver.assumptionsConflict().get(i);
                    if (coreMapping.containsKey(p)) {
                        assert !activeSoft.get(coreMapping.get(p));
                        if (softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i))).weight() < minCore) {
                            minCore = softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i))).weight();
                        }
                    }
                    if (boundMapping.containsKey(p)) {
                        final IntTriple softId = boundMapping.get(solver.assumptionsConflict().get(i));
                        if (softId.weight < minCore) {
                            minCore = softId.weight;
                        }
                    }
                }
                lbCost += minCore;
                nbCores++;
                if (nbSatisfiable == 0) {
                    return unsat();
                }
                if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    return optimum();
                }
                sumSizeCores += solver.assumptionsConflict().size();
                final LngIntVector softRelax = new LngIntVector();
                final LngIntVector cardinalityRelax = new LngIntVector();

                for (int i = 0; i < solver.assumptionsConflict().size(); i++) {
                    final int p = solver.assumptionsConflict().get(i);
                    if (coreMapping.containsKey(p)) {
                        if (softClauses.get(coreMapping.get(p)).weight() > minCore) {
                            assert !activeSoft.get(coreMapping.get(p));
                            // Split the clause
                            final int indexSoft = coreMapping.get(p);
                            assert softClauses.get(indexSoft).weight() - minCore > 0;

                            // Update the weight of the soft clause.
                            softClauses.get(indexSoft).setWeight(softClauses.get(indexSoft).weight() - minCore);
                            final LngIntVector clause = new LngIntVector(softClauses.get(indexSoft).clause());
                            final LngIntVector vars = new LngIntVector();

                            // Since cardinality constraints are added the
                            // variables are not in sync...
                            while (nVars() < solver.nVars()) {
                                newLiteral(false);
                            }
                            final int l = newLiteral(false);
                            vars.push(l);

                            // Add a new soft clause with the weight of the
                            // core.
                            addSoftClause(minCore, clause, vars);
                            activeSoft.push(true);

                            // Add information to the SAT solver
                            newSatVariable(solver);
                            clause.push(l);
                            solver.addClause(clause, null);
                            assert clause.size() - 1 == softClauses.get(indexSoft).clause().size();
                            assert softClauses.get(softClauses.size() - 1).relaxationVars().size() == 1;

                            // Create a new assumption literal.
                            softClauses.get(softClauses.size() - 1).setAssumptionVar(l);
                            assert softClauses.get(softClauses.size() - 1).assumptionVar() ==
                                    softClauses.get(softClauses.size() - 1).relaxationVars().get(0);
                            // Map the new soft clause to its assumption
                            // literal.
                            coreMapping.put(l, softClauses.size() - 1);
                            softRelax.push(l);
                            assert softClauses.get(coreMapping.get(l)).weight() == minCore;
                            assert activeSoft.size() == softClauses.size();
                        } else {
                            assert softClauses.get(coreMapping.get(solver.assumptionsConflict().get(i))).weight() ==
                                    minCore;
                            softRelax.push(p);
                            assert !activeSoft.get(coreMapping.get(p));
                            activeSoft.set(coreMapping.get(p), true);
                        }
                    }
                    if (boundMapping.containsKey(p)) {
                        assert cardinalityAssumptions.contains(p);
                        // this is a soft cardinality -- bound must be increased
                        final IntTriple softId = boundMapping.get(solver.assumptionsConflict().get(i));

                        // increase the bound
                        assert softId.id < softCardinality.size();
                        assert softCardinality.get(softId.id).hasCardEncoding();
                        if (softId.weight == minCore) {
                            cardinalityAssumptions.remove(p);
                            cardinalityRelax.push(p);
                            joinObjFunction.clear();
                            encodingAssumptions.clear();
                            softCardinality.get(softId.id).incUpdateCardinality(solver, joinObjFunction,
                                    softCardinality.get(softId.id).lits(), softId.bound + 1, encodingAssumptions);

                            // if the bound is the same as the number of
                            // literals then no restriction is applied
                            if (softId.bound + 1 < softCardinality.get(softId.id).outputs().size()) {
                                assert softCardinality.get(softId.id).outputs().size() > softId.bound + 1;
                                final int out = softCardinality.get(softId.id).outputs().get(softId.bound + 1);
                                boundMapping.put(out, new IntTriple(softId.id, softId.bound + 1, minCore));
                                cardinalityAssumptions.add(out);
                            }
                        } else {
                            // Duplicate cardinality constraint
                            final Encoder e = new Encoder(MaxSatConfig.CardinalityEncoding.TOTALIZER);
                            e.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
                            e.buildCardinality(solver, softCardinality.get(softId.id).lits(), softId.bound);
                            assert e.outputs().size() > softId.bound;
                            final int out = e.outputs().get(softId.bound);
                            softCardinality.push(e);
                            boundMapping.put(out, new IntTriple(softCardinality.size() - 1, softId.bound, minCore));
                            cardinalityRelax.push(out);

                            // Update value of the previous cardinality
                            // constraint
                            assert softId.weight - minCore > 0;
                            boundMapping.put(p, new IntTriple(softId.id, softId.bound, softId.weight - minCore));

                            // Update bound as usual...
                            final IntTriple softCoreId = boundMapping.get(out);
                            joinObjFunction.clear();
                            encodingAssumptions.clear();
                            softCardinality.get(softCoreId.id).incUpdateCardinality(solver, joinObjFunction,
                                    softCardinality.get(softCoreId.id).lits(), softCoreId.bound + 1,
                                    encodingAssumptions);

                            // if the bound is the same as the number of
                            // literals then no restriction is applied
                            if (softCoreId.bound + 1 < softCardinality.get(softCoreId.id).outputs().size()) {
                                assert softCardinality.get(softCoreId.id).outputs().size() > softCoreId.bound + 1;
                                final int out2 = softCardinality.get(softCoreId.id).outputs().get(softCoreId.bound + 1);
                                boundMapping.put(out2, new IntTriple(softCoreId.id, softCoreId.bound + 1, minCore));
                                cardinalityAssumptions.add(out2);
                            }
                        }
                    }
                }
                assert softRelax.size() + cardinalityRelax.size() > 0;
                if (softRelax.size() == 1 && cardinalityRelax.size() == 0) {
                    solver.addClause(softRelax.get(0), null);
                }
                if (softRelax.size() + cardinalityRelax.size() > 1) {
                    final LngIntVector relaxHarden = new LngIntVector(softRelax);
                    for (int i = 0; i < cardinalityRelax.size(); i++) {
                        relaxHarden.push(cardinalityRelax.get(i));
                    }
                    final Encoder e = new Encoder(MaxSatConfig.CardinalityEncoding.TOTALIZER);
                    e.setIncremental(MaxSatConfig.IncrementalStrategy.ITERATIVE);
                    e.buildCardinality(solver, relaxHarden, 1);
                    softCardinality.push(e);
                    assert e.outputs().size() > 1;
                    final int out = e.outputs().get(1);
                    boundMapping.put(out, new IntTriple(softCardinality.size() - 1, 1, minCore));
                    cardinalityAssumptions.add(out);
                }
                assumptions.clear();
                for (int i = 0; i < softClauses.size(); i++) {
                    if (!activeSoft.get(i) && softClauses.get(i).weight() >= minWeight) {
                        assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                for (final Integer it : cardinalityAssumptions) {
                    final IntTriple softId = boundMapping.get(it);
                    assert softId != null;
                    if (softId.weight >= minWeight) {
                        assumptions.push(LngCoreSolver.not(it));
                    }
                }
            }
        }
    }

    private void initRelaxation() {
        for (int i = 0; i < softClauses.size(); i++) {
            final int l = newLiteral(false);
            softClauses.get(i).relaxationVars().push(l);
            softClauses.get(i).setAssumptionVar(l);
        }
    }

    private int findNextWeightDiversity(final int weight, final Set<Integer> cardinalityAssumptions, final SortedMap<Integer, IntTriple> boundMapping) {
        assert (nbSatisfiable > 0);
        int nextWeight = weight;
        int nbClauses;
        final LinkedHashSet<Integer> nbWeights = new LinkedHashSet<>();
        final double alpha = 1.25;
        boolean findNext = false;
        while (true) {
            if (nbSatisfiable > 1 || findNext) {
                nextWeight = findNextWeight(nextWeight, cardinalityAssumptions, boundMapping);
            }
            nbClauses = 0;
            nbWeights.clear();
            for (int i = 0; i < softClauses.size(); i++) {
                if (softClauses.get(i).weight() >= nextWeight) {
                    nbClauses++;
                    nbWeights.add(softClauses.get(i).weight());
                }
            }
            for (final Integer it : cardinalityAssumptions) {
                final IntTriple softId = boundMapping.get(it);
                assert softId != null;
                if (softId.weight >= nextWeight) {
                    nbClauses++;
                    nbWeights.add(softId.weight);
                }
            }
            if ((float) nbClauses / nbWeights.size() > alpha || nbClauses == softClauses.size() + cardinalityAssumptions.size()) {
                break;
            }
            if (nbSatisfiable == 1 && !findNext) {
                findNext = true;
            }
        }
        return nextWeight;
    }

    int findNextWeight(final int weight, final Set<Integer> cardinalityAssumptions, final SortedMap<Integer, IntTriple> boundMapping) {
        int nextWeight = 1;
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() > nextWeight && softClauses.get(i).weight() < weight) {
                nextWeight = softClauses.get(i).weight();
            }
        }
        for (final Integer it : cardinalityAssumptions) {
            final IntTriple softId = boundMapping.get(it);
            assert softId != null;
            if (softId.weight > nextWeight && softId.weight < weight) {
                nextWeight = softId.weight;
            }
        }
        return nextWeight;
    }

    private static class IntTriple {
        private final int id;
        private final int bound;
        private final int weight;

        public IntTriple(final int id, final int bound, final int weight) {
            this.id = id;
            this.bound = bound;
            this.weight = weight;
        }
    }
}
