// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.cardinalityconstraints.CCEncoder;
import com.booleworks.logicng.cardinalityconstraints.CCIncrementalData;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.pseudobooleans.PBEncoder;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.SolverFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.sat.MiniSat2Solver;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;
import com.booleworks.logicng.solvers.sat.SATCall;
import com.booleworks.logicng.solvers.sat.SATCallBuilder;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.transformations.cnf.PlaistedGreenbaumTransformationSolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A generic interface for LogicNG's SAT solvers.
 * @version 3.0.0
 * @since 1.0
 */
public class SATSolver {

    protected final FormulaFactory f;
    protected final SATSolverConfig config;
    protected final MiniSat2Solver solver;
    protected final PlaistedGreenbaumTransformationSolver pgTransformation;
    protected final PlaistedGreenbaumTransformationSolver fullPgTransformation;

    /**
     * Constructs a new SAT solver instance.
     * @param f      the formula factory
     * @param config the MiniSat configuration, must not be {@code null}
     */
    protected SATSolver(final FormulaFactory f, final SATSolverConfig config) {
        this.f = f;
        this.config = config;
        solver = new MiniSat2Solver(f, config);
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, solver, config.initialPhase());
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, solver, config.initialPhase());
    }

    /**
     * Constructs a new SAT solver with a given underlying solver core.
     * This method is primarily used for serialization purposes and should not be required in any
     * other application use case.
     * @param f                the formula factory
     * @param underlyingSolver the underlying solver core
     */
    public SATSolver(final FormulaFactory f, final MiniSat2Solver underlyingSolver) {
        this.f = f;
        config = underlyingSolver.getConfig();
        solver = underlyingSolver;
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, underlyingSolver, config.initialPhase());
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, underlyingSolver, config.initialPhase());
    }

    /**
     * Returns a new MiniSat solver with the MiniSat configuration from the formula factory.
     * @param f the formula factory
     * @return the solver
     */
    public static SATSolver miniSat(final FormulaFactory f) {
        return new SATSolver(f, (SATSolverConfig) f.configurationFor(ConfigurationType.MINISAT));
    }

    /**
     * Returns a new MiniSat solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration, must not be {@code null}
     * @return the solver
     */
    public static SATSolver miniSat(final FormulaFactory f, final SATSolverConfig config) {
        return new SATSolver(f, config);
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
    public void add(final Formula formula, final Proposition proposition) {
        if (formula.type() == FType.PBC) {
            final PBConstraint constraint = (PBConstraint) formula;
            if (constraint.isCC()) {
                if (config.useAtMostClauses()) {
                    if (constraint.comparator() == CType.LE) {
                        solver.addAtMost(generateClauseVector(constraint.operands()), constraint.rhs());
                    } else if (constraint.comparator() == CType.LT && constraint.rhs() > 3) {
                        solver.addAtMost(generateClauseVector(constraint.operands()), constraint.rhs() - 1);
                    } else if (constraint.comparator() == CType.EQ && constraint.rhs() == 1) {
                        solver.addAtMost(generateClauseVector(constraint.operands()), constraint.rhs());
                        solver.addClause(generateClauseVector(constraint.operands()), proposition);
                    } else {
                        CCEncoder.encode((CardinalityConstraint) constraint, EncodingResult.resultForMiniSat(f, this, proposition));
                    }
                } else {
                    CCEncoder.encode((CardinalityConstraint) constraint, EncodingResult.resultForMiniSat(f, this, proposition));
                }
            } else {
                PBEncoder.encode(constraint, EncodingResult.resultForMiniSat(f, this, proposition));
            }
        } else {
            addFormulaAsCNF(formula, proposition);
        }
        addAllOriginalVariables(formula);
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
    public CCIncrementalData addIncrementalCC(final CardinalityConstraint cc) {
        final EncodingResult result = EncodingResult.resultForMiniSat(f, this, null);
        return CCEncoder.encodeIncremental(cc, result);
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
        final LNGIntVector ps = generateClauseVector(formula.literals(f));
        solver.addClause(ps, proposition);
    }

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
    public SATCallBuilder satCall() {
        return SATCall.builder(f, this);
    }

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
     * {@link SATSolverConfig#proofGeneration() Proof generation} must be enabled in order to use this method,
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
    public <RESULT> RESULT execute(final SolverFunction<RESULT> function) {
        return function.apply(this, i -> {
        });
    }

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
     */
    public SolverState saveState() {
        return solver.saveState();
    }

    /**
     * Loads a given solver state.
     * @param state the solver state
     * @throws IllegalArgumentException if the given state has become invalid
     */
    public void loadState(final SolverState state) {
        solver.loadState(state);
        pgTransformation.clearCache();
        fullPgTransformation.clearCache();
    }

    /**
     * Returns the set of variables currently known by the solver.
     * NOTE: Due to the incremental/decremental interface of some solvers, this set is generated each time,
     * the method is called.  So if you can maintain a list of relevant/known variables in your own application,
     * this is recommended.
     * @return the set of variables currently known by the solver
     */
    public SortedSet<Variable> knownVariables() {
        final SortedSet<Variable> result = new TreeSet<>();
        final int nVars = solver.nVars();
        for (final Map.Entry<String, Integer> entry : solver.name2idx().entrySet()) {
            if (entry.getValue() < nVars) {
                result.add(f.variable(entry.getKey()));
            }
        }
        return result;
    }

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
     * ATTENTION: by influencing the underlying solver directly, you can mess things up completely!  You should really
     * know what you are doing.
     * @return the underlying core solver
     */
    public MiniSatStyleSolver underlyingSolver() {
        return solver;
    }

    /**
     * Generates a clause vector of a collection of literals.
     * @param literals the literals
     * @return the clause vector
     */
    protected LNGIntVector generateClauseVector(final Collection<? extends Literal> literals) {
        final LNGIntVector clauseVec = new LNGIntVector(literals.size());
        for (final Literal lit : literals) {
            final int index = getOrAddIndex(lit);
            final int litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
            clauseVec.push(litNum);
        }
        return clauseVec;
    }

    protected int getOrAddIndex(final Literal lit) {
        int index = solver.idxForName(lit.name());
        if (index == -1) {
            index = solver.newVar(!config.initialPhase(), true);
            solver.addName(lit.name(), index);
        }
        return index;
    }

    public Model createModel(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
        return new Model(createLiterals(vec, relevantIndices));
    }

    private List<Literal> createLiterals(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
        final List<Literal> literals = new ArrayList<>(vec.size());
        for (int i = 0; i < relevantIndices.size(); i++) {
            final int index = relevantIndices.get(i);
            if (index != -1) {
                final String name = solver.nameForIdx(index);
                literals.add(f.literal(name, vec.get(index)));
            }
        }
        return literals;
    }

    /**
     * Adds all variables of the given formula to the solver if not already present.
     * This method can be used to ensure that the internal solver knows the given variables.
     * @param originalFormula the original formula
     */
    private void addAllOriginalVariables(final Formula originalFormula) {
        for (final Variable var : originalFormula.variables(f)) {
            getOrAddIndex(var);
        }
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
