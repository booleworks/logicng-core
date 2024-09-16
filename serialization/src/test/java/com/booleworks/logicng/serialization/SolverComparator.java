// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static com.booleworks.logicng.serialization.CollectionComperator.assertBoolVecEquals;
import static com.booleworks.logicng.serialization.CollectionComperator.assertIntVecEquals;
import static com.booleworks.logicng.serialization.ReflectionHelper.getField;
import static com.booleworks.logicng.serialization.SolverDatastructureComparator.assertClausesEquals;
import static com.booleworks.logicng.serialization.SolverDatastructureComparator.assertHeapEquals;
import static com.booleworks.logicng.serialization.SolverDatastructureComparator.assertVariablesEquals;
import static com.booleworks.logicng.serialization.SolverDatastructureComparator.assertWatchListsEquals;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver.ProofInformation;

public class SolverComparator {

    public static void compareSolverStates(final SATSolver solver1, final SATSolver solver2) {
        final LNGCoreSolver s1 = solver1.getUnderlyingSolver();
        final LNGCoreSolver s2 = solver2.getUnderlyingSolver();

        assertFieldEqual(s1, s2, "config");
        assertFieldEqual(s1, s2, "llConfig");
        assertFieldEqual(s1, s2, "inSatCall");

        assertFieldEqual(s1, s2, "name2idx");
        assertFieldEqual(s1, s2, "idx2name");

        assertIntVecEquals(getField(s1, "validStates"), getField(s2, "validStates"));
        assertFieldEqual(s1, s2, "nextStateId");

        assertFieldEqual(s1, s2, "ok");
        assertFieldEqual(s1, s2, "qhead");
        assertIntVecEquals(getField(s1, "unitClauses"), getField(s2, "unitClauses"));
        assertClausesEquals(getField(s1, "clauses"), getField(s2, "clauses"));
        assertClausesEquals(getField(s1, "learnts"), getField(s2, "learnts"));
        assertWatchListsEquals(getField(s1, "watches"), getField(s2, "watches"));
        assertVariablesEquals(getField(s1, "vars"), getField(s2, "vars"));
        assertHeapEquals(getField(s1, "orderHeap"), getField(s2, "orderHeap"));
        assertIntVecEquals(getField(s1, "trail"), getField(s2, "trail"));
        assertIntVecEquals(getField(s1, "trailLim"), getField(s2, "trailLim"));
        assertBoolVecEquals(getField(s1, "model"), getField(s2, "model"));
        assertIntVecEquals(getField(s1, "assumptionsConflict"), getField(s2, "assumptionsConflict"));
        assertIntVecEquals(getField(s1, "assumptions"), getField(s2, "assumptions"));
        assertBoolVecEquals(getField(s1, "seen"), getField(s2, "seen"));
        assertFieldEqual(s1, s2, "analyzeBtLevel");
        assertFieldEqual(s1, s2, "claInc");
        assertFieldEqual(s1, s2, "varInc");
        assertFieldEqual(s1, s2, "varDecay");
        assertFieldEqual(s1, s2, "clausesLiterals");
        assertFieldEqual(s1, s2, "learntsLiterals");

        assertProofEqual(s1, s2);

        assertFieldEqual(s1, s2, "backboneCandidates");
        assertIntVecEquals(getField(s1, "backboneAssumptions"), getField(s2, "backboneAssumptions"));
        assertFieldEqual(s1, s2, "backboneMap");
        assertFieldEqual(s1, s2, "computingBackbone");

        assertIntVecEquals(getField(s1, "selectionOrder"), getField(s2, "selectionOrder"));
        assertFieldEqual(s1, s2, "selectionOrderIdx");

        assertWatchListsEquals(getField(s1, "watchesBin"), getField(s2, "watchesBin"));
        assertIntVecEquals(getField(s1, "permDiff"), getField(s2, "permDiff"));
        assertIntVecEquals(getField(s1, "lastDecisionLevel"), getField(s2, "lastDecisionLevel"));
        assertFieldEqual(s1, s2, "myflag");
        assertFieldEqual(s1, s2, "analyzeLBD");
        assertFieldEqual(s1, s2, "nbClausesBeforeReduce");
        assertFieldEqual(s1, s2, "conflicts");
        assertFieldEqual(s1, s2, "conflictsRestarts");
        assertFieldEqual(s1, s2, "sumLBD");
        assertFieldEqual(s1, s2, "curRestart");
    }

    private static void assertFieldEqual(final LNGCoreSolver s1, final LNGCoreSolver s2, final String field) {
        final Object f1 = getField(s1, field);
        final Object f2 = getField(s2, field);
        assertThat(f1).isEqualTo(f2);
    }

    private static void assertProofEqual(final LNGCoreSolver s1, final LNGCoreSolver s2) {
        final LNGVector<ProofInformation> pg1 = getField(s1, "pgOriginalClauses");
        final LNGVector<ProofInformation> pg2 = getField(s2, "pgOriginalClauses");
        if (pg1 == null) {
            assertThat(pg2).isNull();
        }
        if (pg2 == null) {
            assertThat(pg1).isNull();
        }
        if (pg1 == null) {
            return;
        }
        assertThat(pg1.size()).isEqualTo(pg2.size());
        for (int i = 0; i < pg1.size(); i++) {
            final ProofInformation pi1 = pg1.get(i);
            final ProofInformation pi2 = pg2.get(i);
            assertIntVecEquals(pi1.getClause(), pi2.getClause());
            assertThat(pi1.getProposition()).isEqualTo(pi2.getProposition());
        }

        final LNGVector<LNGIntVector> proof1 = getField(s1, "pgProof");
        final LNGVector<LNGIntVector> proof2 = getField(s2, "pgProof");
        if (proof1 == null) {
            assertThat(proof2).isNull();
        }
        if (proof2 == null) {
            assertThat(proof1).isNull();
        }
        if (proof1 == null) {
            return;
        }
        assertThat(proof1.size()).isEqualTo(proof2.size());
        for (int i = 0; i < proof1.size(); i++) {
            assertIntVecEquals(proof1.get(i), proof2.get(i));
        }
    }
}
