// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.encodings.CcIncrementalData;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.SolverFunction;
import com.booleworks.logicng.solvers.functions.UnsatCoreFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.NoSplitModelEnumerationStrategy;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/**
 * A generic interface for LogicNG's SAT solvers.
 * @version 3.0.0
 * @since 1.0
 */
public abstract class SATSolver {

    protected final FormulaFactory f;
    protected Tristate result;

    /**
     * Constructor.
     * @param f the formula factory
     */
    protected SATSolver(final FormulaFactory f) {
        this.f = f;
    }

    /**
     * Adds a formula to the solver. The formula is first converted to CNF.
     * @param formula the formula
     */
    public void add(final Formula formula) {
        add(formula, null);
    }

    /**
     * Adds a formula to the solver. The formula is first converted to CNF.
     * @param formula     the formula
     * @param proposition the proposition of this formula
     */
    public abstract void add(final Formula formula, Proposition proposition);

    /**
     * Adds a given set of propositions to the solver.
     * @param propositions the set of propositions
     */
    public void addPropositions(final Collection<? extends Proposition> propositions) {
        for (final Proposition proposition : propositions) {
            add(proposition);
        }
    }

    /**
     * Adds a given set of propositions to the solver.
     * @param propositions the set of propositions
     */
    public void addPropositions(final Proposition... propositions) {
        for (final Proposition proposition : propositions) {
            add(proposition);
        }
    }

    /**
     * Adds a proposition to the solver. The formulas of the proposition are
     * first converted to CNF.
     * @param proposition the proposition
     */
    public void add(final Proposition proposition) {
        add(proposition.formula(), proposition);
    }

    /**
     * Adds a collection of formulas to the solver.
     * @param formulas the collection of formulas
     */
    public void add(final Collection<? extends Formula> formulas) {
        for (final Formula formula : formulas) {
            add(formula);
        }
    }

    /**
     * Adds a formula to the solver by using the given relaxation variable, i.e.
     * by adding the disjunction of the relaxation variable and the formula.
     * @param relaxationVar the relaxation variable
     * @param formula       the formula
     */
    public void addWithRelaxation(final Variable relaxationVar, final Formula formula) {
        add(f.or(relaxationVar, formula));
    }

    /**
     * Adds a collection of formulas to the solver by using the given relaxation
     * variable, i.e. for each formula adding the disjunction of the relaxation
     * variable and the formula.
     * @param relaxationVar the relaxation variable
     * @param formulas      the collection of formulas
     */
    public void addWithRelaxation(final Variable relaxationVar, final Collection<? extends Formula> formulas) {
        for (final Formula formula : formulas) {
            addWithRelaxation(relaxationVar, formula);
        }
    }

    /**
     * Adds a cardinality constraint and returns its incremental data in order
     * to refine the constraint on the solver.
     * <p>
     * Usage constraints:
     * <ul>
     * <li>"&lt;": Cannot be used with right-hand side 2, returns null for
     * right-hand side 1, but constraint is added to solver</li>
     * <li>"&lt;=": Cannot be used with right-hand side 1, returns null for
     * right-hand side 0, but constraint is added to solver</li>
     * <li>"&gt;": Returns null for right-hand side 0 or number of variables -1,
     * but constraint is added to solver. Adds false to solver for right-hand
     * side &gt;= number of variables</li>
     * <li>"&gt;=": Returns null for right-hand side 1 or number of variables,
     * but constraint is added to solver. Adds false to solver for right-hand
     * side &gt; number of variables</li>
     * </ul>
     * @param cc the cardinality constraint
     * @return the incremental data of this constraint, or null if the
     *         right-hand side of cc is 1
     */
    public abstract CcIncrementalData addIncrementalCC(final CardinalityConstraint cc);

    /**
     * Adds a formula which is already in CNF to the solver.
     * @param proposition a proposition (if required for proof tracing)
     * @param formula     the formula in CNF
     */
    void addClauseSet(final Formula formula, final Proposition proposition) {
        switch (formula.type()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                addClause(formula, proposition);
                break;
            case AND:
                for (final Formula op : formula) {
                    addClause(op, proposition);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + formula);
        }
    }

    /**
     * Adds a formula which must be a clause to the solver.
     * @param formula     the clause
     * @param proposition a proposition (if required for proof tracing)
     */
    protected abstract void addClause(final Formula formula, final Proposition proposition);

    /**
     * Returns {@code Tristate.TRUE} if the current formula in the solver is
     * satisfiable, @{code Tristate.FALSE} if it is unsatisfiable, or
     * {@code UNDEF} if the solving process was aborted.
     * @return the satisfiability of the formula in the solver
     */
    public Tristate sat() {
        return sat((SATHandler) null);
    }

    /**
     * Returns {@code Tristate.TRUE} if the current formula in the solver is
     * satisfiable, @{code Tristate.FALSE} if it is unsatisfiable, or
     * {@code UNDEF} if the solving process was aborted.
     * @param handler the SAT handler
     * @return the satisfiability of the formula in the solver
     */
    public abstract Tristate sat(final SATHandler handler);

    /**
     * Returns {@code Tristate.TRUE} if the current formula in the solver and a
     * given literal are satisfiable, {@code Tristate.FALSE} if it is
     * unsatisfiable, or {@code UNDEF} if the solving process was aborted.
     * <p>
     * Side effect: Solving with assumptions adds the assumption literals as
     * known variables to the solver if not already known. This change lasts
     * beyond the assumption solving call and can have unintended results for
     * subsequent solver calls. For example, a subsequent model enumeration call
     * will produce models containing the now known variables. A reliable
     * workaround for this side effect is to save the state of the solver with
     * {@link #saveState()} and load the state of the solver after the
     * assumption call(s) with {@link #loadState(SolverState)}.
     * @param literal the assumed literal
     * @return the satisfiability of the formula in the solver
     */
    public Tristate sat(final Literal literal) {
        return sat(null, literal);
    }

    /**
     * Returns {@code Tristate.TRUE} if the current formula in the solver and a
     * given collection of assumed literals are satisfiable,
     * {@code Tristate.FALSE} if it is unsatisfiable, or {@code UNDEF} if the
     * solving process was aborted. The assumptions can be seen as an additional
     * conjunction of literals. Note: Use ordered collections to ensure
     * determinism in the solving process and thus in the resulting model or
     * conflict.
     * <p>
     * Side effect: Solving with assumptions adds the assumption literals as
     * known variables to the solver if not already known. This change lasts
     * beyond the assumption solving call and can have unintended results for
     * subsequent solver calls. For example, a subsequent model enumeration call
     * will produce models containing the now known variables. A reliable
     * workaround for this side effect is to save the state of the solver with
     * {@link #saveState()} and load the state of the solver after the
     * assumption call(s) with {@link #loadState(SolverState)}.
     * @param assumptions a collection of literals
     * @return the satisfiability of the formula in the solver
     */
    public Tristate sat(final Collection<? extends Literal> assumptions) {
        return sat(null, assumptions);
    }

    /**
     * Returns {@code Tristate.TRUE} if the current formula in the solver and a
     * given literal are satisfiable, {@code Tristate.FALSE} if it is
     * unsatisfiable, or {@code UNDEF} if the solving process was aborted.
     * <p>
     * Side effect: Solving with assumptions adds the assumption literals as
     * known variables to the solver if not already known. This change lasts
     * beyond the assumption solving call and can have unintended results for
     * subsequent solver calls. For example, a subsequent model enumeration call
     * will produce models containing the now known variables. A reliable
     * workaround for this side effect is to save the state of the solver with
     * {@link #saveState()} and load the state of the solver after the
     * assumption call(s) with {@link #loadState(SolverState)}.
     * @param handler the SAT handler
     * @param literal the assumed literal
     * @return the satisfiability of the formula in the solver
     */
    public abstract Tristate sat(final SATHandler handler, final Literal literal);

    /**
     * Returns {@code Tristate.TRUE} if the current formula in the solver and a
     * given collection of assumed literals are satisfiable,
     * {@code Tristate.FALSE} if it is unsatisfiable, or {@code UNDEF} if the
     * solving process was aborted. The assumptions can be seen as an additional
     * conjunction of literals. Note: Use ordered collections to ensure
     * determinism in the solving process and thus in the resulting model or
     * conflict.
     * <p>
     * Side effect: Solving with assumptions adds the assumption literals as
     * known variables to the solver if not already known. This change lasts
     * beyond the assumption solving call and can have unintended results for
     * subsequent solver calls. For example, a subsequent model enumeration call
     * will produce models containing the now known variables. A reliable
     * workaround for this side effect is to save the state of the solver with
     * {@link #saveState()} and load the state of the solver after the
     * assumption call(s) with {@link #loadState(SolverState)}.
     * @param handler     the SAT handler
     * @param assumptions a collection of literals
     * @return the satisfiability of the formula in the solver
     */
    public abstract Tristate sat(final SATHandler handler, final Collection<? extends Literal> assumptions);

    /**
     * Solves the formula on the solver with a given selection order.
     * <p>
     * If a custom selection order is set, the solver will pick a variable from
     * the custom order in order to branch on it during the search. The given
     * polarity in the selection order is used as assignment for the variable.
     * If all variables in the custom order are already assigned, the solver
     * falls back to the activity based variable selection.
     * <p>
     * Example: Order a, ~b, c. The solver picks variable `a`, if not assigned
     * yet, and checks if setting `a` to true leads to a satisfying assignment.
     * Next, the solver picks variable b and checks if setting b to false leads
     * to a satisfying assignment.
     * @param selectionOrder the order of the literals for the selection order
     * @return the satisfiability of the formula in the solver
     */
    public Tristate satWithSelectionOrder(final List<? extends Literal> selectionOrder) {
        return satWithSelectionOrder(selectionOrder, null, null);
    }

    /**
     * Solves the formula on the solver with a given selection order, a given
     * SAT handler and a list of additional assumptions.
     * <p>
     * If a custom selection order is set, the solver will pick a variable from
     * the custom order in order to branch on it during the search. The given
     * polarity in the selection order is used as assignment for the variable.
     * If all variables in the custom order are already assigned, the solver
     * falls back to the activity based variable selection.
     * <p>
     * Example: Order a, ~b, c. The solver picks variable `a`, if not assigned
     * yet, and checks if setting `a` to true leads to a satisfying assignment.
     * Next, the solver picks variable b and checks if setting b to false leads
     * to a satisfying assignment.
     * @param selectionOrder the order of the literals for the selection order
     * @param handler        the SAT handler
     * @param assumptions    a collection of literals
     * @return the satisfiability of the formula in the solver
     */
    public Tristate satWithSelectionOrder(final List<? extends Literal> selectionOrder, final SATHandler handler,
                                          final Collection<? extends Literal> assumptions) {
        setSolverToUndef();
        setSelectionOrder(selectionOrder);
        final Tristate sat = assumptions != null ? sat(handler, assumptions) : sat(handler);
        resetSelectionOrder();
        return sat;
    }

    /**
     * Resets the SAT solver.
     */
    public abstract void reset();

    /**
     * Returns a model of the current formula on the solver wrt. a given set of
     * variables. If the set is {@code null}, all variables are considered
     * relevant. If the formula is UNSAT, {@code null} will be returned. The
     * formula in the solver has to be solved first, before a model can be
     * obtained.
     * @param variables the set of variables
     * @return a model of the current formula
     * @throws IllegalStateException if the formula is not yet solved
     */
    public Assignment model(final Variable[] variables) {
        return model(Arrays.asList(variables));
    }

    /**
     * Returns a model of the current formula on the solver wrt. a given set of
     * variables. If the set is {@code null}, all variables are considered
     * relevant. If the formula is UNSAT, {@code null} will be returned.
     * @param variables the set of variables
     * @return a model of the current formula
     */
    public abstract Assignment model(final Collection<Variable> variables);

    /**
     * Executes a solver function on this solver.
     * @param function the solver function
     * @param <RESULT> the result type of the function
     * @return the result of executing the solver function on the current solver
     */
    public abstract <RESULT> RESULT execute(final SolverFunction<RESULT> function);

    /**
     * Enumerates all models of the current formula wrt. a given set of
     * variables. If the set is {@code null}, all variables are considered
     * relevant.
     * @param variables the set of variables
     * @return the list of models
     */
    public List<Model> enumerateAllModels(final Collection<Variable> variables) {
        final ModelEnumerationStrategy strategy = canSaveLoadState() ? DefaultModelEnumerationStrategy.builder().build()
                : NoSplitModelEnumerationStrategy.get();
        return execute(ModelEnumerationFunction.builder(variables)
                .configuration(ModelEnumerationConfig.builder().strategy(strategy).build())
                .build());
    }

    /**
     * Enumerates all models of the current formula wrt. a given set of
     * variables. If the set is {@code null}, all variables are considered
     * relevant.
     * @param variables the set of variables
     * @return the list of models
     */
    public List<Model> enumerateAllModels(final Variable[] variables) {
        final ModelEnumerationStrategy strategy = canSaveLoadState() ? DefaultModelEnumerationStrategy.builder().build()
                : NoSplitModelEnumerationStrategy.get();
        return execute(ModelEnumerationFunction.builder(variables)
                .configuration(ModelEnumerationConfig.builder().strategy(strategy).build())
                .build());
    }

    /**
     * Saves the current solver state.
     * @return the current solver state
     * @throws UnsupportedOperationException if the solver does not support
     *                                       state saving/loading
     * @throws IllegalStateException         if the solver is not in incremental
     *                                       mode
     */
    public abstract SolverState saveState();

    /**
     * Loads a given solver state.
     * @param state the solver state
     * @throws UnsupportedOperationException if the solver does not support
     *                                       state saving/loading
     * @throws IllegalStateException         if the solver is not in incremental
     *                                       mode
     * @throws IllegalArgumentException      if the given state has become
     *                                       invalid
     */
    public abstract void loadState(final SolverState state);

    /**
     * Sets the solver state to UNDEF (required if you fiddle e.g. with the
     * underlying solver).
     */
    public void setSolverToUndef() {
        result = Tristate.UNDEF;
    }

    /**
     * Returns the set of variables currently known by the solver. NOTE: Due to
     * the incremental/decremental interface of some solvers, this set is
     * generated each time, the method is called. So if you can maintain a list
     * of relevant/known variables in your own application, this is recommended.
     * @return the set of variables currently known by the solver
     */
    public abstract SortedSet<Variable> knownVariables();

    /**
     * Returns an unsat core of the current problem. Only works if the SAT
     * solver is configured to record the information required to generate a
     * proof trace and an unsat core. In particular, this method returns the
     * unsat core only if the parameter "proofGeneration" in the MiniSatConfig
     * is set to "true".
     * @return the unsat core
     */
    public UNSATCore<Proposition> unsatCore() {
        return execute(UnsatCoreFunction.get());
    }

    /**
     * Computes a backbone with both positive and negative variables of the
     * current formula on the solver.
     * @param relevantVariables the variables which should be considered for the
     *                          backbone
     * @return the backbone
     */
    public Backbone backbone(final Collection<Variable> relevantVariables) {
        return backbone(relevantVariables, BackboneType.POSITIVE_AND_NEGATIVE);
    }

    /**
     * Computes a backbone of the current formula on the solver.
     * @param relevantVariables the variables which should be considered for the
     *                          backbone
     * @param type              the type of backbone which should be computed
     * @return the backbone
     */
    public Backbone backbone(final Collection<Variable> relevantVariables, final BackboneType type) {
        return execute(BackboneFunction.builder().variables(relevantVariables).type(type).build());
    }

    /**
     * Returns the formula factory for this solver.
     * @return the formula factory
     */
    public FormulaFactory factory() {
        return f;
    }

    /**
     * Sets the selection order of the variables and their polarity.
     * <p>
     * @param selectionOrder the variable order and their polarity that should
     *                       be checked first
     */
    protected abstract void setSelectionOrder(List<? extends Literal> selectionOrder);

    /**
     * Resets the selection order on the solver. The internal activity
     * heuristics for the variable ordering will be used again.
     */
    protected abstract void resetSelectionOrder();

    /**
     * Returns whether this solver instance can save and load solver states.
     * @return true when the solver can save and load states, false otherwise
     */
    public abstract boolean canSaveLoadState();

    /**
     * Returns whether this solver instance can generate proofs.
     * @return true when the solver can generate proofs, false otherwise
     */
    public abstract boolean canGenerateProof();
}
