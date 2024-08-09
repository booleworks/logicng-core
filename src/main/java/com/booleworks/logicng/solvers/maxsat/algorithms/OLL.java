// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.algorithms;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.solvers.maxsat.InternalMaxSATResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * OLL Solver.
 * <p>
 * Based on "Unsatisfiability-based optimization in clasp*" by Andres, Kaufmann,
 * Matheis, and Schaub.
 * @version 2.4.0
 * @since 2.4.0
 */
public class OLL extends MaxSAT {
    private Encoder encoder;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public OLL(final FormulaFactory f) {
        this(f, MaxSATConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public OLL(final FormulaFactory f, final MaxSATConfig config) {
        super(f, config);
        verbosity = config.verbosity;
        if (config.cardinalityEncoding != MaxSATConfig.CardinalityEncoding.TOTALIZER) {
            throw new IllegalStateException("Error: Currently OLL only supports the totalizer encoding.");
        }
    }

    @Override
    protected LNGResult<InternalMaxSATResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(config.cardinalityEncoding);
        encoder.setPBEncoding(config.pbEncoding);
        if (problemType == ProblemType.WEIGHTED) {
            return weighted(handler);
        } else {
            return unweighted(handler);
        }
    }

    private LNGCoreSolver rebuildSolver() {
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

    private LNGResult<InternalMaxSATResult> unweighted(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        final SortedMap<Integer, IntTriple> boundMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        final LNGCoreSolver solver = rebuildSolver();

        final LNGIntVector assumptions = new LNGIntVector();
        final LNGIntVector joinObjFunction = new LNGIntVector();
        final LNGIntVector encodingAssumptions = new LNGIntVector();
        encoder.setIncremental(MaxSATConfig.IncrementalStrategy.ITERATIVE);

        final LNGBooleanVector activeSoft = new LNGBooleanVector(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }

        final LinkedHashSet<Integer> cardinalityAssumptions = new LinkedHashSet<>();
        final LNGVector<Encoder> softCardinality = new LNGVector<>();

        while (true) {
            final LNGResult<Boolean> res = searchSATSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LNGResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final LNGBooleanVector model = solver.model();
                final int newCost = computeCostModel(model, Integer.MAX_VALUE);
                saveModel(model);

                ubCost = newCost;
                if (nbSatisfiable == 1) {
                    if (newCost == 0) {
                        return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                    }
                    for (int i = 0; i < nSoft(); i++) {
                        assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                } else {
                    assert lbCost == newCost;
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
            } else {
                lbCost++;
                nbCores++;
                if (nbSatisfiable == 0) {
                    return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                }
                if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
                sumSizeCores += solver.assumptionsConflict().size();
                final LNGIntVector softRelax = new LNGIntVector();
                final LNGIntVector cardinalityRelax = new LNGIntVector();

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
                    final LNGIntVector relaxHarden = new LNGIntVector(softRelax);
                    for (int i = 0; i < cardinalityRelax.size(); i++) {
                        relaxHarden.push(cardinalityRelax.get(i));
                    }
                    final Encoder e = new Encoder(MaxSATConfig.CardinalityEncoding.TOTALIZER);
                    e.setIncremental(MaxSATConfig.IncrementalStrategy.ITERATIVE);
                    e.buildCardinality(solver, relaxHarden, 1);
                    softCardinality.push(e);
                    assert e.outputs().size() > 1;

                    final int out = e.outputs().get(1);
                    boundMapping.put(out, new IntTriple(softCardinality.size() - 1, 1, 1));
                    cardinalityAssumptions.add(out);
                }

                // reset the assumptions
                assumptions.clear();
                for (int i = 0; i < nSoft(); i++) {
                    if (!activeSoft.get(i)) {
                        assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                for (final Integer it : cardinalityAssumptions) {
                    assumptions.push(LNGCoreSolver.not(it));
                }
            }
        }
    }

    private LNGResult<InternalMaxSATResult> weighted(final ComputationHandler handler) {
        final SortedMap<Integer, Integer> coreMapping = new TreeMap<>();
        final SortedMap<Integer, IntTriple> boundMapping = new TreeMap<>();
        nbInitialVariables = nVars();
        initRelaxation();
        final LNGCoreSolver solver = rebuildSolver();

        final LNGIntVector assumptions = new LNGIntVector();
        final LNGIntVector joinObjFunction = new LNGIntVector();
        final LNGIntVector encodingAssumptions = new LNGIntVector();
        encoder.setIncremental(MaxSATConfig.IncrementalStrategy.ITERATIVE);

        final LNGBooleanVector activeSoft = new LNGBooleanVector(nSoft(), false);
        for (int i = 0; i < nSoft(); i++) {
            coreMapping.put(softClauses.get(i).assumptionVar(), i);
        }

        final LinkedHashSet<Integer> cardinalityAssumptions = new LinkedHashSet<>();
        final LNGVector<Encoder> softCardinality = new LNGVector<>();
        int minWeight = currentWeight;

        while (true) {
            final LNGResult<Boolean> res = searchSATSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LNGResult.canceled(res.getCancelCause());
            } else if (res.getResult()) {
                nbSatisfiable++;
                final LNGBooleanVector model = solver.model();
                final int newCost = computeCostModel(model, Integer.MAX_VALUE);
                if (newCost < ubCost || nbSatisfiable == 1) {
                    saveModel(model);
                    ubCost = newCost;
                }
                if (nbSatisfiable == 1) {
                    minWeight = findNextWeightDiversity(minWeight, cardinalityAssumptions, boundMapping);
                    for (int i = 0; i < nSoft(); i++) {
                        if (softClauses.get(i).weight() >= minWeight) {
                            assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                        }
                    }
                } else {
                    // compute min weight in soft
                    int notConsidered = 0;
                    for (int i = 0; i < nSoft(); i++) {
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
                        for (int i = 0; i < nSoft(); i++) {
                            if (!activeSoft.get(i) && softClauses.get(i).weight() >= minWeight) {
                                assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                            }
                        }
                        for (final Integer it : cardinalityAssumptions) {
                            final IntTriple softId = boundMapping.get(it);
                            assert softId != null;
                            if (softId.weight >= minWeight) {
                                assumptions.push(LNGCoreSolver.not(it));
                            }
                        }
                    } else {
                        assert lbCost == newCost;
                        return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
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
                    return LNGResult.of(InternalMaxSATResult.unsatisfiable());
                }
                if (lbCost == ubCost) {
                    assert nbSatisfiable > 0;
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
                sumSizeCores += solver.assumptionsConflict().size();
                final LNGIntVector softRelax = new LNGIntVector();
                final LNGIntVector cardinalityRelax = new LNGIntVector();

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
                            final LNGIntVector clause = new LNGIntVector(softClauses.get(indexSoft).clause());
                            final LNGIntVector vars = new LNGIntVector();

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
                            newSATVariable(solver);
                            clause.push(l);
                            solver.addClause(clause, null);
                            assert clause.size() - 1 == softClauses.get(indexSoft).clause().size();
                            assert softClauses.get(nSoft() - 1).relaxationVars().size() == 1;

                            // Create a new assumption literal.
                            softClauses.get(nSoft() - 1).setAssumptionVar(l);
                            assert softClauses.get(nSoft() - 1).assumptionVar() ==
                                    softClauses.get(nSoft() - 1).relaxationVars().get(0);
                            // Map the new soft clause to its assumption
                            // literal.
                            coreMapping.put(l, nSoft() - 1);
                            softRelax.push(l);
                            assert softClauses.get(coreMapping.get(l)).weight() == minCore;
                            assert activeSoft.size() == nSoft();
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
                            final Encoder e = new Encoder(MaxSATConfig.CardinalityEncoding.TOTALIZER);
                            e.setIncremental(MaxSATConfig.IncrementalStrategy.ITERATIVE);
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
                    final LNGIntVector relaxHarden = new LNGIntVector(softRelax);
                    for (int i = 0; i < cardinalityRelax.size(); i++) {
                        relaxHarden.push(cardinalityRelax.get(i));
                    }
                    final Encoder e = new Encoder(MaxSATConfig.CardinalityEncoding.TOTALIZER);
                    e.setIncremental(MaxSATConfig.IncrementalStrategy.ITERATIVE);
                    e.buildCardinality(solver, relaxHarden, 1);
                    softCardinality.push(e);
                    assert e.outputs().size() > 1;
                    final int out = e.outputs().get(1);
                    boundMapping.put(out, new IntTriple(softCardinality.size() - 1, 1, minCore));
                    cardinalityAssumptions.add(out);
                }
                assumptions.clear();
                for (int i = 0; i < nSoft(); i++) {
                    if (!activeSoft.get(i) && softClauses.get(i).weight() >= minWeight) {
                        assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                    }
                }
                for (final Integer it : cardinalityAssumptions) {
                    final IntTriple softId = boundMapping.get(it);
                    assert softId != null;
                    if (softId.weight >= minWeight) {
                        assumptions.push(LNGCoreSolver.not(it));
                    }
                }
            }
        }
    }

    private void initRelaxation() {
        for (int i = 0; i < nbSoft; i++) {
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
            for (int i = 0; i < nSoft(); i++) {
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
            if ((float) nbClauses / nbWeights.size() > alpha || nbClauses == nSoft() + cardinalityAssumptions.size()) {
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
        for (int i = 0; i < nSoft(); i++) {
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
