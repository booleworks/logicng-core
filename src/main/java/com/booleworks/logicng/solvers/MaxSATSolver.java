// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.FACTORY_CNF;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.PG_ON_SOLVER;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.maxsat.InternalMaxSATResult;
import com.booleworks.logicng.solvers.maxsat.algorithms.IncWBO;
import com.booleworks.logicng.solvers.maxsat.algorithms.LinearSU;
import com.booleworks.logicng.solvers.maxsat.algorithms.LinearUS;
import com.booleworks.logicng.solvers.maxsat.algorithms.MSU3;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATState;
import com.booleworks.logicng.solvers.maxsat.algorithms.OLL;
import com.booleworks.logicng.solvers.maxsat.algorithms.WBO;
import com.booleworks.logicng.solvers.maxsat.algorithms.WMSU3;
import com.booleworks.logicng.transformations.cnf.PlaistedGreenbaumTransformationMaxSATSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A wrapper for the OpenWBO solver.
 * @version 3.0.0
 * @since 1.0
 */
public class MaxSATSolver {

    private static final String SEL_PREFIX = "@SEL_SOFT_";

    public enum Algorithm {
        WBO,
        INC_WBO,
        LINEAR_SU,
        LINEAR_US,
        MSU3,
        WMSU3,
        OLL
    }

    protected final MaxSATConfig configuration;
    protected final Algorithm algorithm;
    protected final PlaistedGreenbaumTransformationMaxSATSolver pgTransformation;
    protected final FormulaFactory f;
    protected final MaxSAT solver;
    protected final SortedSet<Variable> selectorVariables;
    protected LNGResult<MaxSATResult> result;

    /**
     * Constructs a new MaxSAT solver with a given configuration.
     * @param f             the formula factory
     * @param configuration the configuration
     * @param algorithm     the algorithm
     * @throws IllegalArgumentException if the algorithm was unknown
     */
    protected MaxSATSolver(final FormulaFactory f, final MaxSATConfig configuration, final Algorithm algorithm) {
        this.f = f;
        this.algorithm = algorithm;
        this.configuration = configuration;
        result = null;
        selectorVariables = new TreeSet<>();
        switch (algorithm) {
            case WBO:
                solver = new WBO(f, configuration);
                break;
            case INC_WBO:
                solver = new IncWBO(f, configuration);
                break;
            case LINEAR_SU:
                solver = new LinearSU(f, configuration);
                break;
            case LINEAR_US:
                solver = new LinearUS(f, configuration);
                break;
            case MSU3:
                solver = new MSU3(f, configuration);
                break;
            case WMSU3:
                solver = new WMSU3(f, configuration);
                break;
            case OLL:
                solver = new OLL(f, configuration);
                break;
            default:
                throw new IllegalArgumentException("Unknown MaxSAT algorithm: " + algorithm);
        }
        pgTransformation = configuration.getCnfMethod() == FACTORY_CNF
                ? null
                : new PlaistedGreenbaumTransformationMaxSATSolver(f, configuration.getCnfMethod() == PG_ON_SOLVER, solver);
    }

    /**
     * Returns a new MaxSAT solver using incremental WBO as algorithm with the
     * MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver incWBO(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.INC_WBO);
    }

    /**
     * Returns a new MaxSAT solver using incremental WBO as algorithm with the
     * given configuration.
     * @param f      the formula factory
     * @param config the configuration
     * @return the MaxSAT solver
     */
    public static MaxSATSolver incWBO(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.INC_WBO);
    }

    /**
     * Returns a new MaxSAT solver using LinearSU as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearSU(final FormulaFactory f) {
        final MaxSATConfig conf = new MaxSATConfig((MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT),
                MaxSATConfig.CardinalityEncoding.MTOTALIZER);
        return new MaxSATSolver(f, conf, Algorithm.LINEAR_SU);
    }

    /**
     * Returns a new MaxSAT solver using LinearSU as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearSU(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.LINEAR_SU);
    }

    /**
     * Returns a new MaxSAT solver using LinearUS as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearUS(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.LINEAR_US);
    }

    /**
     * Returns a new MaxSAT solver using LinearUS as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearUS(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.LINEAR_US);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver msu3(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.MSU3);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver msu3(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.MSU3);
    }

    /**
     * Returns a new MaxSAT solver using WBO as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wbo(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.WBO);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wbo(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.WBO);
    }

    /**
     * Returns a new MaxSAT solver using weighted MSU3 as algorithm with the
     * MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wmsu3(final FormulaFactory f) {
        final MaxSATConfig conf = new MaxSATConfig((MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT),
                MaxSATConfig.IncrementalStrategy.ITERATIVE);
        return new MaxSATSolver(f, conf, Algorithm.WMSU3);
    }

    /**
     * Returns a new MaxSAT solver using weighted MSU3 as algorithm with the
     * given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wmsu3(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.WMSU3);
    }

    /**
     * Returns a new MaxSAT solver using weighted OLL as algorithm with the
     * MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver oll(final FormulaFactory f) {
        final MaxSATConfig conf = new MaxSATConfig((MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT),
                MaxSATConfig.IncrementalStrategy.ITERATIVE);
        return new MaxSATSolver(f, conf, Algorithm.OLL);
    }

    /**
     * Returns a new MaxSAT solver using weighted OLL as algorithm with the
     * given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver oll(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.OLL);
    }

    /**
     * Returns whether this solver can handle weighted instances or not.
     * @return whether this solver can handle weighted instances or not
     */
    public boolean isWeighted() {
        return algorithm == Algorithm.INC_WBO || algorithm == Algorithm.WMSU3 || algorithm == Algorithm.WBO ||
                algorithm == Algorithm.OLL;
    }

    /**
     * Adds a new hard formula to the solver. Hard formulas must always be true.
     * @param formula the formula
     */
    public void addHardFormula(final Formula formula) {
        addFormulaAsCnf(formula, -1);
    }

    /**
     * Adds a new soft formula to the solver.
     * @param formula the formula
     * @param weight  the weight
     * @throws IllegalArgumentException if the weight is &lt;1
     */
    public void addSoftFormula(final Formula formula, final int weight) {
        if (weight < 1) {
            throw new IllegalArgumentException("The weight of a formula must be > 0");
        }
        final Variable selVar = f.variable(SEL_PREFIX + selectorVariables.size());
        selectorVariables.add(selVar);
        addFormulaAsCnf(f.or(selVar.negate(f), formula), -1);
        addFormulaAsCnf(f.or(formula.negate(f), selVar), -1);
        addFormulaAsCnf(selVar, weight);
    }

    /**
     * Saves the current solver state.
     * @return the current solver state
     */
    public MaxSATState saveState() {
        return solver.saveState();
    }

    /**
     * Loads a given state in the solver.
     * <p>
     * ATTENTION: You can only load a state which was created by this instance
     * of the solver before the current state. Only the sizes of the internal
     * data structures are stored, meaning you can go back in time and
     * restore a solver state with fewer variables and/or fewer clauses. It is
     * not possible to import a solver state from another solver or another
     * solving execution.
     * @param state the solver state to load
     * @throws IllegalArgumentException if the solver state is not valid anymore
     */
    public void loadState(final MaxSATState state) {
        result = null;
        solver.loadState(state);
    }

    private void addFormulaAsCnf(final Formula formula, final int weight) {
        result = null;
        if (configuration.getCnfMethod() == FACTORY_CNF) {
            addCNF(formula.cnf(f), weight);
        } else {
            pgTransformation.addCnfToSolver(formula, weight);
        }
    }

    /**
     * Adds a formula which is already in CNF to the solver.
     * @param formula the formula in CNF
     * @param weight  the weight of this CNF (or -1 for a hard constraint)
     */
    protected void addCNF(final Formula formula, final int weight) {
        switch (formula.type()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                solver.addClause(formula, weight);
                break;
            case AND:
                for (final Formula op : formula) {
                    solver.addClause(op, weight);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + formula);
        }
    }

    /**
     * Solves the formula on the solver and returns the result.
     * @return the result (SAT, UNSAT, Optimum found)
     */
    public MaxSATResult solve() {
        return solve(NopHandler.get()).getResult();
    }

    /**
     * Solves the formula on the solver and returns the result.
     * @param handler a MaxSAT handler
     * @return the result (SAT, UNSAT, Optimum found, or UNDEF if canceled by
     * the handler)
     */
    public LNGResult<MaxSATResult> solve(final ComputationHandler handler) {
        if (result != null && result.isSuccess()) {
            return result;
        }
        if (solver.currentWeight() == 1) {
            solver.setProblemType(MaxSAT.ProblemType.UNWEIGHTED);
        } else {
            solver.setProblemType(MaxSAT.ProblemType.WEIGHTED);
        }
        final LNGResult<InternalMaxSATResult> internalResult = solver.search(handler);
        result = internalResult.map(res -> res.toMaxSATResult(this::createModel));
        return result;
    }

    /**
     * Creates a model from a Boolean vector of the solver.
     * @param vec the vector of the solver
     * @return the model
     */
    protected Model createModel(final LNGBooleanVector vec) {
        final List<Literal> model = new ArrayList<>();
        for (int i = 0; i < vec.size(); i++) {
            final Variable var = solver.varForIndex(i);
            if (var != null && !selectorVariables.contains(var)) {
                model.add(vec.get(i) ? var : var.negate(f));
            }
        }
        return new Model(model);
    }

    /**
     * Returns the stats of the underlying solver.
     * @return the stats of the underlying solver
     */
    public MaxSAT.Stats stats() {
        return solver.stats();
    }

    /**
     * Returns the algorithm for this solver.
     * @return the algorithm
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the formula factory for this solver.
     * @return the formula factory
     */
    public FormulaFactory factory() {
        return f;
    }

    @Override
    public String toString() {
        return String.format("MaxSATSolver{result=%s}", result);
    }
}
