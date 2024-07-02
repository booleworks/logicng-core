// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.encodings.CcEncoder;
import com.booleworks.logicng.encodings.CcIncrementalData;
import com.booleworks.logicng.encodings.PbEncoder;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.SolverFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;
import com.booleworks.logicng.solvers.sat.SATCall;
import com.booleworks.logicng.solvers.sat.SATCallBuilder;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.transformations.cnf.PlaistedGreenbaumTransformationSolver;

import java.util.Collection;
import java.util.List;

/**
 * A generic interface for LogicNG's SAT solvers.
 * @version 3.0.0
 * @since 1.0
 */
// TODO sort and cleanup methods
public class SATSolver {

    protected final FormulaFactory f;
    protected final SATSolverConfig config;
    protected final LNGCoreSolver solver;
    protected final PlaistedGreenbaumTransformationSolver pgTransformation;
    protected final PlaistedGreenbaumTransformationSolver fullPgTransformation;

    /**
     * Constructs a new SAT solver instance.
     * @param f      the formula factory
     * @param config the solver configuration, must not be {@code null}
     */
    protected SATSolver(final FormulaFactory f, final SATSolverConfig config) {
        this.f = f;
        this.config = config;
        solver = new LNGCoreSolver(f, config);
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, solver);
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, solver);
    }

    /**
     * Constructs a new SAT solver with a given underlying solver core. This
     * method is primarily used for serialization purposes and should not be
     * required in any other application use case.
     * @param f                the formula factory
     * @param underlyingSolver the underlying solver core
     */
    public SATSolver(final FormulaFactory f, final LNGCoreSolver underlyingSolver) {
        this.f = f;
        config = underlyingSolver.config();
        solver = underlyingSolver;
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, underlyingSolver);
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, underlyingSolver);
    }

    /**
     * Returns a new SAT solver with the solver configuration from the formula
     * factory.
     * @param f the formula factory
     * @return the solver
     */
    public static SATSolver newSolver(final FormulaFactory f) {
        return new SATSolver(f, (SATSolverConfig) f.configurationFor(ConfigurationType.SAT));
    }

    /**
     * Returns a new SAT solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration, must not be {@code null}
     * @return the solver
     */
    public static SATSolver newSolver(final FormulaFactory f, final SATSolverConfig config) {
        return new SATSolver(f, config);
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
    public void add(final Formula formula, final Proposition proposition) {
        if (formula.type() == FType.PBC) {
            final PBConstraint constraint = (PBConstraint) formula;
            if (constraint.isCC()) {
                if (config.useAtMostClauses()) {
                    if (constraint.comparator() == CType.LE) {
                        solver.addAtMost(LNGCoreSolver.generateClauseVector(constraint.operands(), solver),
                                constraint.rhs());
                    } else if (constraint.comparator() == CType.LT && constraint.rhs() > 3) {
                        solver.addAtMost(LNGCoreSolver.generateClauseVector(constraint.operands(), solver),
                                constraint.rhs() - 1);
                    } else if (constraint.comparator() == CType.EQ && constraint.rhs() == 1) {
                        solver.addAtMost(LNGCoreSolver.generateClauseVector(constraint.operands(), solver),
                                constraint.rhs());
                        solver.addClause(LNGCoreSolver.generateClauseVector(constraint.operands(), solver),
                                proposition);
                    } else {
                        CcEncoder.encode((CardinalityConstraint) constraint,
                                EncodingResult.resultForSATSolver(f, solver, proposition));
                    }
                } else {
                    CcEncoder.encode((CardinalityConstraint) constraint,
                            EncodingResult.resultForSATSolver(f, solver, proposition));
                }
            } else {
                PbEncoder.encode(constraint, EncodingResult.resultForSATSolver(f, solver, proposition));
            }
        } else {
            addFormulaAsCNF(formula, proposition);
        }
    }

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
     * right-hand side 1, but constraint is added to solver.</li>
     * <li>"&lt;=": Cannot be used with right-hand side 1, returns null for
     * right-hand side 0, but constraint is added to solver.</li>
     * <li>"&gt;": Returns null for right-hand side 0 or number of variables -1,
     * but constraint is added to solver. Adds false to solver for right-hand
     * side &gt;= number of variables.</li>
     * <li>"&gt;=": Returns null for right-hand side 1 or number of variables,
     * but constraint is added to solver. Adds false to solver for right-hand
     * side &gt; number of variables.</li>
     * </ul>
     * @param cc the cardinality constraint
     * @return the incremental data of this constraint, or null if the
     *         right-hand side of cc is 1
     */
    public CcIncrementalData addIncrementalCc(final CardinalityConstraint cc) {
        final EncodingResult result = EncodingResult.resultForSATSolver(f, solver, null);
        return CcEncoder.encodeIncremental(cc, result);
    }

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
    protected void addClause(final Formula formula, final Proposition proposition) {
        final LNGIntVector ps = LNGCoreSolver.generateClauseVector(formula.literals(f), solver);
        solver.addClause(ps, proposition);
    }

    /**
     * Central method for building a SAT call. This method returns a
     * {@link SATCallBuilder} which can be enriched with assumptions, additional
     * formulas, handlers, etc. {@link SATCallBuilder#solve()} then performs the
     * actual SAT call and returns a {@link SATCall} object from which a
     * {@link SATCall#model model} or {@link SATCall#unsatCore() UNSAT core} can
     * be generated.
     * <p>
     * <b>A SAT solver may only have one &quot;open&quot; SATCall at a time. So
     * a an existing SAT call must always be {@link SATCall#close() closed}
     * (ideally using a try-with construct) before the next call to this
     * method.</b>
     * @return a new SATCall builder
     * @see SATCallBuilder
     */
    public SATCallBuilder satCall() {
        return SATCall.builder(this);
    }

    /**
     * Returns {@code true} if the current formula in the solver is
     * satisfiable, @{code false} if it is unsatisfiable.
     * <p>
     * This is a shortcut for {@code satCall().sat()} (since no handler is used,
     * the result is directly transformed to a {@code boolean}).
     * @return the satisfiability of the formula on the solver
     */
    public boolean sat() {
        try (final SATCall call = satCall().solve()) {
            return call.getSatResult().getResult();
        }
    }

    /**
     * Executes a solver function on this solver.
     * @param function the solver function
     * @param <RESULT> the result type of the function
     * @return the result of executing the solver function on the current solver
     * @throws IllegalStateException if this solver is currently used in a
     *                               {@link SATCall}
     */
    public <RESULT> RESULT execute(final SolverFunction<RESULT> function) {
        solver.assertNotInSatCall();
        return function.apply(this);
    }

    /**
     * Executes a solver function on this solver.
     * @param function the solver function
     * @param handler  the computation handler
     * @param <RESULT> the result type of the function
     * @return the (potentially aborted) result of executing the solver
     *         function on the current solver
     * @throws IllegalStateException if this solver is currently used in a
     *                               {@link SATCall}
     */
    public <RESULT> LNGResult<RESULT> execute(final SolverFunction<RESULT> function, final ComputationHandler handler) {
        solver.assertNotInSatCall();
        return function.apply(this, handler);
    }

    /**
     * Enumerates all models of the current formula wrt. a given set of
     * variables. If the set is {@code null}, all variables are considered
     * relevant.
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
     * Enumerates all models of the current formula wrt. a given set of
     * variables. If the set is {@code null}, all variables are considered
     * relevant.
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
     */
    public SolverState saveState() {
        solver.assertNotInSatCall();
        return solver.saveState();
    }

    /**
     * Loads a given solver state.
     * @param state the solver state
     * @throws IllegalArgumentException if the given state has become invalid
     */
    public void loadState(final SolverState state) {
        solver.assertNotInSatCall();
        solver.loadState(state);
        pgTransformation.clearCache();
        fullPgTransformation.clearCache();
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
     * Returns the initial phase of literals of this solver.
     * @return the initial phase of literals of this solver
     */
    public SATSolverConfig config() {
        return config;
    }

    /**
     * Returns the underlying core solver.
     * <p>
     * ATTENTION: by influencing the underlying solver directly, you can mess
     * things up completely! You should really know what you are doing.
     * @return the underlying core solver
     */
    public LNGCoreSolver underlyingSolver() {
        return solver;
    }

    protected void addFormulaAsCNF(final Formula formula, final Proposition proposition) {
        if (config.cnfMethod() == SATSolverConfig.CNFMethod.FACTORY_CNF) {
            addClauseSet(formula.cnf(f), proposition);
        } else if (config.cnfMethod() == SATSolverConfig.CNFMethod.PG_ON_SOLVER) {
            pgTransformation.addCNFtoSolver(formula, proposition);
        } else if (config.cnfMethod() == SATSolverConfig.CNFMethod.FULL_PG_ON_SOLVER) {
            fullPgTransformation.addCNFtoSolver(formula, proposition);
        } else {
            throw new IllegalStateException("Unknown Solver CNF method: " + config.cnfMethod());
        }
    }
}
