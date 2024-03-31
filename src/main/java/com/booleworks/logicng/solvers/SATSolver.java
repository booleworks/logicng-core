// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.cardinalityconstraints.CCIncrementalData;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.SolverFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.solvers.sat.SATCall;
import com.booleworks.logicng.solvers.sat.SATCallBuilder;

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
     * Adds a formula to the solver.  The formula is first converted to CNF.
     * @param formula the formula
     */
    public void add(final Formula formula) {
        add(formula, null);
    }

    /**
     * Adds a formula to the solver.  The formula is first converted to CNF.
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
     * Adds a proposition to the solver.  The formulas of the proposition are first converted to CNF.
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
     * Adds a formula to the solver by using the given relaxation variable,
     * i.e. by adding the disjunction of the relaxation variable and the formula.
     * @param relaxationVar the relaxation variable
     * @param formula       the formula
     */
    public void addWithRelaxation(final Variable relaxationVar, final Formula formula) {
        add(f.or(relaxationVar, formula));
    }

    /**
     * Adds a collection of formulas to the solver by using the given relaxation variable,
     * i.e. for each formula adding the disjunction of the relaxation variable and the formula.
     * @param relaxationVar the relaxation variable
     * @param formulas      the collection of formulas
     */
    public void addWithRelaxation(final Variable relaxationVar, final Collection<? extends Formula> formulas) {
        for (final Formula formula : formulas) {
            addWithRelaxation(relaxationVar, formula);
        }
    }

    /**
     * Adds a cardinality constraint and returns its incremental data in order to refine the constraint on the solver.
     * <p>
     * Usage constraints:
     * <ul>
     * <li>"&lt;": Cannot be used with right-hand side 2, returns null for right-hand side 1, but constraint is added to solver.</li>
     * <li>"&lt;=": Cannot be used with right-hand side 1, returns null for right-hand side 0, but constraint is added to solver.</li>
     * <li>"&gt;": Returns null for right-hand side 0 or number of variables -1, but constraint is added to solver. Adds false to solver for right-hand side
     * &gt;= number of variables.</li>
     * <li>"&gt;=": Returns null for right-hand side 1 or number of variables, but constraint is added to solver. Adds false to solver for right-hand side &gt;</li>
     * </ul>
     * number of variables.
     * @param cc the cardinality constraint
     * @return the incremental data of this constraint, or null if the right-hand side of cc is 1
     */
    public abstract CCIncrementalData addIncrementalCC(final CardinalityConstraint cc);

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
     * Central method for building a SAT call. This method returns a {@link SATCallBuilder} which can be enriched
     * with assumptions, additional formulas, handlers, etc. {@link SATCallBuilder#solve()} then performs the actual
     * SAT call and returns a {@link SATCall} object from which a {@link SATCall#model model} or
     * {@link SATCall#unsatCore() UNSAT core} can be generated.
     * <p>
     * There are also useful shortcuts: If you only require a quick SAT check, you can just use {@link #sat()}.
     * If you already know that the solver is satisfiable/unsatisfiable, you can directly call {@link #model} /
     * {@link #unsatCore()}.
     * <p>
     * <b>A SAT solver may only have one &quot;open&quot; SATCall at a time. So a an existing SAT call must
     * always be {@link SATCall#close() closed} (ideally using a try-with construct) before the next call
     * to this method.</b>
     * @return a new SATCall builder
     * @see SATCallBuilder
     */
    public abstract SATCallBuilder satCall();

    /**
     * Returns {@code true} if the current formula in the solver is satisfiable, @{code false} if it is unsatisfiable.
     * <p>
     * This is a shortcut for {@code satCall().sat()} (since no handler is used, the result is directly transformed
     * to a {@code boolean}).
     * @return the satisfiability of the formula on the solver
     */
    public boolean sat() {
        try (final SATCall call = satCall().solve()) {
            return call.getSatResult() == Tristate.TRUE;
        }
    }

    /**
     * Returns a model of the current formula on the solver wrt. a given set of variables.
     * The variables must not be {@code null}. If you just want to get all variables, you
     * can use {@link SATSolver#knownVariables() all variables known by the solver}.
     * <p>
     * If the formula is UNSAT, {@code null} will be returned.
     * <p>
     * This is a shortcut for {@code satCall().model()}.
     * @param variables the set of variables
     * @return a model of the current formula or {@code null} if the SAT call was unsatisfiable
     */
    public Assignment model(final Collection<Variable> variables) {
        try (final SATCall call = satCall().solve()) {
            return call.model(variables);
        }
    }

    /**
     * Returns an unsat core of the current problem.
     * <p>
     * {@link MiniSatConfig#proofGeneration() Proof generation} must be enabled in order to use this method,
     * otherwise an {@link IllegalStateException} is thrown.
     * <p>
     * If the formula on the solver is satisfiable, {@code null} is returned.
     * <p>
     * This is a shortcut for {@code satCall().unsatCore()}.
     * @return the unsat core or {@code null} if the SAT call was satisfiable
     */
    public UNSATCore<Proposition> unsatCore() {
        try (final SATCall call = satCall().solve()) {
            return call.unsatCore();
        }
    }

    /**
     * Executes a solver function on this solver.
     * @param function the solver function
     * @param <RESULT> the result type of the function
     * @return the result of executing the solver function on the current solver
     */
    public abstract <RESULT> RESULT execute(final SolverFunction<RESULT> function);

    /**
     * Enumerates all models of the current formula wrt. a given set of variables.  If the set is {@code null},
     * all variables are considered relevant.
     * @param variables the set of variables
     * @return the list of models
     */
    public List<Model> enumerateAllModels(final Collection<Variable> variables) {
        return execute(ModelEnumerationFunction.builder(variables)
                .configuration(ModelEnumerationConfig.builder()
                        .strategy(DefaultModelEnumerationStrategy.builder().build()).build())
                .build());
    }

    /**
     * Enumerates all models of the current formula wrt. a given set of variables.  If the set is {@code null},
     * all variables are considered relevant.
     * @param variables the set of variables
     * @return the list of models
     */
    public List<Model> enumerateAllModels(final Variable[] variables) {
        return execute(ModelEnumerationFunction.builder(variables)
                .configuration(ModelEnumerationConfig.builder()
                        .strategy(DefaultModelEnumerationStrategy.builder().build()).build())
                .build());
    }

    /**
     * Saves the current solver state.
     * @return the current solver state
     * @throws UnsupportedOperationException if the solver does not support state saving/loading
     * @throws IllegalStateException         if the solver is not in incremental mode
     */
    public abstract SolverState saveState();

    /**
     * Loads a given solver state.
     * @param state the solver state
     * @throws UnsupportedOperationException if the solver does not support state saving/loading
     * @throws IllegalStateException         if the solver is not in incremental mode
     * @throws IllegalArgumentException      if the given state has become invalid
     */
    public abstract void loadState(final SolverState state);

    /**
     * Sets the solver state to UNDEF (required if you fiddle e.g. with the underlying solver).
     */
    public void setSolverToUndef() {
        result = Tristate.UNDEF;
    }

    /**
     * Returns the set of variables currently known by the solver.
     * NOTE: Due to the incremental/decremental interface of some solvers, this set is generated each time,
     * the method is called.  So if you can maintain a list of relevant/known variables in your own application,
     * this is recommended.
     * @return the set of variables currently known by the solver
     */
    public abstract SortedSet<Variable> knownVariables();

    /**
     * Computes a backbone with both positive and negative variables of the current formula on the solver.
     * @param relevantVariables the variables which should be considered for the backbone
     * @return the backbone
     */
    public Backbone backbone(final Collection<Variable> relevantVariables) {
        return backbone(relevantVariables, BackboneType.POSITIVE_AND_NEGATIVE);
    }

    /**
     * Computes a backbone of the current formula on the solver.
     * @param relevantVariables the variables which should be considered for the backbone
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
}
