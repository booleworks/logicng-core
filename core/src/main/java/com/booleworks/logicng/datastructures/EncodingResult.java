// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.InternalAuxVarType;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of an encoding.
 * <p>
 * Encodings (normal forms, cardinality constraints, pseudo-Boolean constraint)
 * are often used only when adding formulas to the SAT solver. Therefore, it is
 * not necessary to generate all the formulas required for the encoding in the
 * formula factory and therefore polluting the factory and the heap. This class
 * can be used to connect an encoding directly with a SAT solver and therefore
 * introducing the variables only on the solver - not in the factory. When
 * working with many encodings, this can be a large performance gain.
 * @version 3.0.0
 * @since 1.1
 */
public final class EncodingResult {
    private final FormulaFactory f;
    private final Proposition proposition;
    private final LngCoreSolver solver;
    private final List<Formula> result;

    /**
     * Constructs a new CC encoding algorithm.
     * @param f           the formula factory
     * @param solver      the solver instance
     * @param proposition the original proposition of the cardinality constraint
     */
    private EncodingResult(final FormulaFactory f, final LngCoreSolver solver, final Proposition proposition) {
        this.f = f;
        this.proposition = proposition;
        this.solver = solver;
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
     * Constructs a new result which adds the result directly to a given solver.
     * @param f           the formula factory
     * @param solver      the solver
     * @param proposition the original proposition of the cardinality constraint
     * @return the result
     */
    public static EncodingResult resultForSatSolver(final FormulaFactory f, final LngCoreSolver solver,
                                                    final Proposition proposition) {
        return new EncodingResult(f, solver, proposition);
    }

    /**
     * Adds a clause to the result
     * @param literals the literals of the clause
     */
    public void addClause(final Literal... literals) {
        if (solver == null) {
            result.add(f.clause(literals));
        } else {
            final LngIntVector clauseVec = new LngIntVector(literals.length);
            for (final Literal literal : literals) {
                addLiteral(clauseVec, literal);
            }
            solver.addClause(clauseVec, proposition);
        }
    }

    /**
     * Adds a clause to the result
     * @param literals the literals of the clause
     */
    public void addClause(final LngVector<Literal> literals) {
        if (solver == null) {
            result.add(vec2clause(literals));
        } else {
            final LngIntVector clauseVec = new LngIntVector(literals.size());
            for (final Literal l : literals) {
                addLiteral(clauseVec, l);
            }
            solver.addClause(clauseVec, proposition);
        }
    }

    private void addLiteral(final LngIntVector clauseVec, final Literal lit) {
        int index = solver.idxForName(lit.getName());
        if (index == -1) {
            index = solver.newVar(!solver.getConfig().getInitialPhase(), true);
            solver.addName(lit.getName(), index);
        }
        final int litNum;
        if (lit instanceof EncodingAuxiliaryVariable) {
            litNum = !((EncodingAuxiliaryVariable) lit).negated ? index * 2 : (index * 2) ^ 1;
        } else {
            litNum = lit.getPhase() ? index * 2 : (index * 2) ^ 1;
        }
        clauseVec.push(litNum);
    }

    /**
     * Returns a clause for a vector of literals.
     * @param literals the literals
     * @return the clause
     */
    private Formula vec2clause(final LngVector<Literal> literals) {
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
    public Variable newVariable(final String auxType) {
        if (solver == null) {
            return f.newAuxVariable(auxType);
        } else {
            final int index = solver.newVar(!solver.getConfig().getInitialPhase(), true);
            final String name = "@AUX_" + auxType + "_SAT_SOLVER_" + index;
            solver.addName(name, index);
            return new EncodingAuxiliaryVariable(name, false);
        }
    }

    public Variable newVariable(final InternalAuxVarType auxType) {
        return newVariable(auxType.getPrefix());
    }

    /**
     * Returns a new auxiliary variable for cardinality constraint encodings.
     * @return a new auxiliary variable
     */
    public Variable newCcVariable() {
        return newVariable(InternalAuxVarType.CC);
    }

    /**
     * Returns a new auxiliary variable for pseudo-boolean constraint encodings.
     * @return a new auxiliary variable
     */
    public Variable newPbcVariable() {
        return newVariable(InternalAuxVarType.PBC);
    }

    /**
     * Returns a new auxiliary variable for cnf encodings.
     * @return a new auxiliary variable
     */
    public Variable newCnfVariable() {
        return newVariable(InternalAuxVarType.CNF);
    }

    /**
     * Returns the result of this algorithm.
     * @return the result of this algorithm
     */
    public List<Formula> getResult() {
        return result;
    }

    public FormulaFactory getFactory() {
        return f;
    }
}
