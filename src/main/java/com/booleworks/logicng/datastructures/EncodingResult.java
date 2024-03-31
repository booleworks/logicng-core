// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of an encoding.
 * <p>
 * Encodings (normal forms, cardinality constraints, pseudo-Boolean constraint) are often used only when adding
 * formulas to the SAT solver.  Therefore, it is not necessary to generate all the formulas required for the encoding
 * in the formula factory and therefore polluting the factory and the heap.  This class can be used to connect an
 * encoding directly with a SAT solver and therefore introducing the variables only on the solver - not in the factory.
 * When working with many encodings, this can be a large performance gain.
 * @version 2.0.0
 * @since 1.1
 */
public final class EncodingResult {
    private final FormulaFactory f;
    private final Proposition proposition;
    private final SATSolver miniSat;
    private final List<Formula> result;

    /**
     * Constructs a new CC encoding algorithm.
     * @param f           the formula factory
     * @param miniSat     the MiniSat instance
     * @param proposition the original proposition of the cardinality constraint
     */
    private EncodingResult(final FormulaFactory f, final SATSolver miniSat, final Proposition proposition) {
        this.f = f;
        this.proposition = proposition;
        this.miniSat = miniSat;
        result = new ArrayList<>();
    }

    /**
     * Constructs a new result which stores the result in a formula.
     * @param f the formula factory
     * @return the result
     */
    public static EncodingResult resultForFormula(final FormulaFactory f) {
        return new EncodingResult(f, null, null);
    }

    /**
     * Constructs a new result which adds the result directly to a given MiniSat solver.
     * @param f           the formula factory
     * @param miniSat     the solver
     * @param proposition the original proposition of the cardinality constraint
     * @return the result
     */
    public static EncodingResult resultForMiniSat(final FormulaFactory f, final SATSolver miniSat, final Proposition proposition) {
        return new EncodingResult(f, miniSat, proposition);
    }

    /**
     * Adds a clause to the result
     * @param literals the literals of the clause
     */
    public void addClause(final Literal... literals) {
        if (miniSat == null) {
            result.add(f.clause(literals));
        } else {
            final LNGIntVector clauseVec = new LNGIntVector(literals.length);
            for (final Literal literal : literals) {
                addLiteral(clauseVec, literal);
            }
            miniSat.underlyingSolver().addClause(clauseVec, proposition);
        }
    }

    /**
     * Adds a clause to the result
     * @param literals the literals of the clause
     */
    public void addClause(final LNGVector<Literal> literals) {
        if (miniSat == null) {
            result.add(vec2clause(literals));
        } else {
            final LNGIntVector clauseVec = new LNGIntVector(literals.size());
            for (final Literal l : literals) {
                addLiteral(clauseVec, l);
            }
            miniSat.underlyingSolver().addClause(clauseVec, proposition);
        }
    }

    private void addLiteral(final LNGIntVector clauseVec, final Literal lit) {
        int index = miniSat.underlyingSolver().idxForName(lit.name());
        if (index == -1) {
            index = miniSat.underlyingSolver().newVar(!miniSat.config().initialPhase(), true);
            miniSat.underlyingSolver().addName(lit.name(), index);
        }
        final int litNum;
        if (lit instanceof EncodingAuxiliaryVariable) {
            litNum = !((EncodingAuxiliaryVariable) lit).negated ? index * 2 : (index * 2) ^ 1;
        } else {
            litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
        }
        clauseVec.push(litNum);
    }

    /**
     * Returns a clause for a vector of literals.
     * @param literals the literals
     * @return the clause
     */
    private Formula vec2clause(final LNGVector<Literal> literals) {
        final List<Literal> lits = new ArrayList<>(literals.size());
        for (final Literal l : literals) {
            lits.add(l);
        }
        return f.clause(lits);
    }

    /**
     * Returns a new auxiliary variable.
     * @return a new auxiliary variable
     */
    public Variable newVariable() {
        if (miniSat == null) {
            return f.newCCVariable();
        } else {
            final int index = miniSat.underlyingSolver().newVar(!miniSat.config().initialPhase(), true);
            final String name = FormulaFactory.CC_PREFIX + "MINISAT_" + index;
            miniSat.underlyingSolver().addName(name, index);
            return new EncodingAuxiliaryVariable(name, false);
        }
    }

    /**
     * Returns the result of this algorithm.
     * @return the result of this algorithm
     */
    public List<Formula> result() {
        return result;
    }

    public FormulaFactory factory() {
        return f;
    }
}
