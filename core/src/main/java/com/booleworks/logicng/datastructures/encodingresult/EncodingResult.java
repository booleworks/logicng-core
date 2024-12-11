// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.InternalAuxVarType;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

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
public interface EncodingResult {

    /**
     * Adds a clause to the result
     * @param literals the literals of the clause
     */
    void addClause(final Literal... literals);

    /**
     * Adds a clause to the result
     * @param literals the literals of the clause
     */
    void addClause(final LngVector<Literal> literals);

    /**
     * Returns a new auxiliary variable.
     * @param auxType auxiliary type of the variable
     * @return a new auxiliary variable
     */
    Variable newVariable(final String auxType);

    /**
     * Returns a new auxiliary variable.
     * @param auxType auxiliary type of the variable
     * @return a new auxiliary variable
     */
    default Variable newVariable(final InternalAuxVarType auxType) {
        return newVariable(auxType.getPrefix());
    }

    /**
     * Returns a new auxiliary variable for cardinality constraint encodings.
     * @return a new auxiliary variable
     */
    default Variable newCcVariable() {
        return newVariable(InternalAuxVarType.CC);
    }

    /**
     * Returns a new auxiliary variable for pseudo-boolean constraint encodings.
     * @return a new auxiliary variable
     */
    default Variable newPbcVariable() {
        return newVariable(InternalAuxVarType.PBC);
    }

    /**
     * Returns a new auxiliary variable for cnf encodings.
     * @return a new auxiliary variable
     */
    default Variable newCnfVariable() {
        return newVariable(InternalAuxVarType.CNF);
    }

    /**
     * Returns the {@link FormulaFactory}.
     * @return the {@link FormulaFactory}
     */
    FormulaFactory getFactory();

    /**
     * Returns the result. The result might not be complete if the encoding
     * result materialize its input as formulas on the formula factory only
     * partially. E.g. the encoding result for a SAT Solver does not materialize
     * any formulas.
     * @return the result
     */
    List<Formula> getResult();

    /**
     * Constructs a new result which stores the result in a formula.
     * @param f the formula factory
     * @return the result
     */
    static EncodingResultFF forFormulaFactory(final FormulaFactory f) {
        return new EncodingResultFF(f);
    }

    /**
     * Constructs a new result which adds the result directly to a given solver.
     * @param solver      the solver
     * @param proposition the original proposition of the cardinality constraint
     * @return the result
     */
    static EncodingResultSolver forSatSolver(final FormulaFactory f, final LngCoreSolver solver,
                                             final Proposition proposition) {
        return new EncodingResultSolver(f, solver, proposition);
    }

}
