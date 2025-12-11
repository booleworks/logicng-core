// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCoreSolver;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Union type for storing variable to variable index mappings. This class either
 * contains a solver and uses the mappings of the solver, or it contains a map
 * and list storing the mappings.
 */
public class SddVariableProxy {
    private final SddCoreSolver solver;
    private final HashMap<Variable, Integer> var2idx;
    private final ArrayList<Variable> idx2var;

    private SddVariableProxy(final SddCoreSolver solver, final HashMap<Variable, Integer> var2idx,
                             final ArrayList<Variable> idx2var) {
        this.solver = solver;
        this.var2idx = var2idx;
        this.idx2var = idx2var;
    }

    /**
     * Constructs an instances that uses the mappings of the passed solver.
     * @param solver the solver
     * @return the proxy
     */
    public static SddVariableProxy fromSolver(final SddCoreSolver solver) {
        return new SddVariableProxy(solver, null, null);
    }

    /**
     * Constructs an instances that uses new maps for storing the mappings.
     * @return the proxy
     */
    public static SddVariableProxy empty() {
        return new SddVariableProxy(null, new HashMap<>(), new ArrayList<>());
    }

    /**
     * Converts the internal representation of a variable to the corresponding
     * LNG variable.
     * @param index the index of a variable
     * @return the corresponding LNG variable
     */
    public Variable indexToVariable(final FormulaFactory f, final int index) {
        if (solver == null) {
            if (index < idx2var.size()) {
                return idx2var.get(index);
            } else {
                return null;
            }
        } else {
            return f.variable(solver.nameForIdx(index));
        }
    }

    /**
     * Converts a LNG variable to the internal representation. If the variable
     * is unknown to this SDD container, it will return {@code -1}.
     * @param variable the LNG variable
     * @return the internal representation or {@code -1} if the variable is
     * unknown
     */
    public int variableToIndex(final Variable variable) {
        if (solver == null) {
            final Integer idx = var2idx.get(variable);
            return Objects.requireNonNullElse(idx, -1);
        } else {
            return solver.idxForName(variable.getName());
        }
    }

    /**
     * Constructs a literal in the internal representation from a variable index
     * and a phase.
     * @param varIdx the index of the variable
     * @param phase  the phase
     * @return the literal in internal representation
     */
    public int literalToIndex(final int varIdx, final boolean phase) {
        return LngCoreSolver.mkLit(varIdx, !phase);
    }

    /**
     * Converts a LNG literal to the internal representation. If the variable of
     * the literal is unknown to this SDD container, it will return {@code -1}.
     * @param literal the LNG literal
     * @return the internal representation or {@code -1} if the variable is
     * unknown
     */
    public int literalToIndex(final Literal literal) {
        final int varIdx = variableToIndex(literal.variable());
        if (varIdx == -1) {
            return -1;
        } else {
            return literalToIndex(varIdx, literal.getPhase());
        }
    }

    /**
     * Converts the internal representation of a literal to the corresponding
     * LNG literal.
     * @param litIdx the index of a literal
     * @return the corresponding LNG literal
     */
    public Literal indexToLiteral(final FormulaFactory f, final int litIdx) {
        final int varIdx = litIdxToVarIdx(litIdx);
        return litIdx > 0 ? indexToVariable(f, varIdx) : indexToVariable(f, varIdx).negate(f);
    }

    /**
     * Converts the internal representation of a literal to the internal
     * representation of the variable of the literal.
     * @param litIdx the literal index
     * @return the variable index
     */
    public int litIdxToVarIdx(final int litIdx) {
        return LngCoreSolver.var(litIdx);
    }

    /**
     * Negates a literal in internal representation.
     * @param litIdx the literal index
     * @return the index of the negated literal
     */
    public int negateLitIdx(final int litIdx) {
        return LngCoreSolver.not(litIdx);
    }

    /**
     * Returns whether there exists an internal representation of the variable.
     * <p>
     * An internal representation can be created by constructing a vtree leaf
     * for the variable.
     * @param variable the variable
     * @return whether there exists an internal representation.
     */
    public boolean knows(final Variable variable) {
        if (solver == null) {
            return var2idx.containsKey(variable);
        } else {
            return solver.idxForName(variable.getName()) != -1;
        }
    }

    /**
     * Adds a new variable to the mapping.
     * @param variable the variable
     * @return the new variable index
     * @throws UnsupportedOperationException if the mapping is stored in a
     *                                       solver
     */
    public int newVar(final Variable variable) {
        if (solver != null) {
            throw new UnsupportedOperationException("Cannot create a new variable for an existing solver");
        }
        final int idx = idx2var.size();
        idx2var.add(variable);
        var2idx.put(variable, idx);
        return idx;
    }
}
