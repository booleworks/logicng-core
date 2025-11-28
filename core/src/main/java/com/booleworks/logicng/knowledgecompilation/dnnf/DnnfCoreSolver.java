// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * A variation of the LNG core solver used during the DNNF compilation process.
 * @version 2.0.0
 * @since 2.0.0
 */
public class DnnfCoreSolver extends LngCoreSolver implements DnnfSatSolver {

    protected boolean newlyImpliedDirty = false;
    protected int assertionLevel = -1;
    protected LngIntVector lastLearnt = null;
    protected final FormulaFactory f;
    protected final Tristate[] assignment;
    protected final List<Literal> impliedOperands;

    /**
     * Constructs a new DNNF core solver with the given number of variables.
     * @param f                 the formula factory
     * @param numberOfVariables the number of variables
     */
    public DnnfCoreSolver(final FormulaFactory f, final int numberOfVariables) {
        super(f, SatSolverConfig.builder().build());
        this.f = f;
        assignment = new Tristate[2 * numberOfVariables];
        Arrays.fill(assignment, Tristate.UNDEF);
        impliedOperands = new ArrayList<>();
    }

    @Override
    public boolean start() {
        newlyImpliedDirty = true;
        return propagate() == null;
    }

    @Override
    public Tristate valueOf(final int lit) {
        return assignment[lit];
    }

    @Override
    public int variableIndex(final Literal lit) {
        return idxForName(lit.getName());
    }

    @Override
    public Literal litForIdx(final int var) {
        return f.literal(idx2name.get(var), true);
    }

    /**
     * Returns the variable index for a given literal.
     * @param lit the literal
     * @return the variable index of the literal
     */
    public static int var(final int lit) {
        return LngCoreSolver.var(lit);
    }

    /**
     * Returns the phase of the given solver literal.
     * @param lit the solver literal
     * @return {@code true} if the literal has a positive phase, {@code false}
     * if the literal has a negative phase (literal is negated)
     */
    public static boolean phase(final int lit) {
        return !sign(lit);
    }

    @Override
    public FormulaFactory getFactory() {
        return f;
    }

    @Override
    public void add(final Formula formula) {
        final Formula cnf = formula.cnf(f);
        switch (cnf.getType()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                addClause(generateClauseVector(cnf.literals(f), this), null);
                break;
            case AND:
                for (final Formula op : cnf) {
                    addClause(generateClauseVector(op.literals(f), this), null);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + cnf);
        }
    }

    @Override
    public boolean decide(final int var, final boolean phase) {
        newlyImpliedDirty = true;
        final int lit = mkLit(var, !phase);
        trailLim.push(trail.size());
        uncheckedEnqueue(lit, null);
        return propagateAfterDecide();
    }

    @Override
    public void undoDecide(final int var) {
        newlyImpliedDirty = false;
        cancelUntil(vars.get(var).level() - 1);
    }

    @Override
    public boolean atAssertionLevel() {
        return decisionLevel() == assertionLevel;
    }

    @Override
    public boolean assertCdLiteral() {
        newlyImpliedDirty = true;
        if (!atAssertionLevel()) {
            throw new IllegalStateException("assertCdLiteral called although not at assertion level!");
        }

        if (lastLearnt.size() == 1) {
            uncheckedEnqueue(lastLearnt.get(0), null);
            unitClauses.push(lastLearnt.get(0));
        } else {
            final LngClause cr = new LngClause(lastLearnt, nextStateId);
            cr.setLbd(analyzeLbd);
            cr.setOneWatched(false);
            learnts.push(cr);
            attachClause(cr);
            claBumpActivity(cr);
            uncheckedEnqueue(lastLearnt.get(0), cr);
        }
        varDecayActivity();
        claDecayActivity();
        return propagateAfterDecide();
    }

    @Override
    public Formula newlyImplied(final BitSet knownVariables) {
        impliedOperands.clear();
        if (newlyImpliedDirty) {
            final int limit = trailLim.isEmpty() ? -1 : trailLim.back();
            for (int i = trail.size() - 1; i > limit; i--) {
                final int lit = trail.get(i);
                if (knownVariables.get(var(lit))) {
                    impliedOperands.add(intToLiteral(lit));
                }
            }
        }
        newlyImpliedDirty = false;
        return f.and(impliedOperands);
    }

    protected Literal intToLiteral(final int lit) {
        final String name = nameForIdx(var(lit));
        return f.literal(name, !sign(lit));
    }

    protected boolean propagateAfterDecide() {
        final LngClause conflict = propagate();
        if (conflict != null) {
            handleConflict(conflict);
            return false;
        }
        return true;
    }

    @Override
    protected void uncheckedEnqueue(final int lit, final LngClause reason) {
        assignment[lit] = Tristate.TRUE;
        assignment[lit ^ 1] = Tristate.FALSE;
        super.uncheckedEnqueue(lit, reason);
    }

    @Override
    protected void cancelUntil(final int level) {
        if (decisionLevel() > level) {
            for (int c = trail.size() - 1; c >= trailLim.get(level); c--) {
                final int l = trail.get(c);
                assignment[l] = Tristate.UNDEF;
                assignment[l ^ 1] = Tristate.UNDEF;
            }
            super.cancelUntil(level);
        }
    }

    protected void handleConflict(final LngClause conflict) {
        if (decisionLevel() > 0) {
            lastLearnt = new LngIntVector();
            analyze(conflict, lastLearnt);
            assertionLevel = analyzeBtLevel;
        } else {
            // solver unsat
            cancelUntil(0);
            lastLearnt = null;
            assertionLevel = -1;
        }
    }
}
