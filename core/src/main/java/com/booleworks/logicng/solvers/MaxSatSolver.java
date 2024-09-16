// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import static com.booleworks.logicng.solvers.sat.SatSolverConfig.CnfMethod.FACTORY_CNF;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.CnfMethod.PG_ON_SOLVER;

import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.maxsat.algorithms.IncWbo;
import com.booleworks.logicng.solvers.maxsat.algorithms.LinearSu;
import com.booleworks.logicng.solvers.maxsat.algorithms.LinearUs;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatState;
import com.booleworks.logicng.solvers.maxsat.algorithms.Msu3;
import com.booleworks.logicng.solvers.maxsat.algorithms.Oll;
import com.booleworks.logicng.solvers.maxsat.algorithms.Wbo;
import com.booleworks.logicng.solvers.maxsat.algorithms.Wmsu3;
import com.booleworks.logicng.transformations.cnf.PlaistedGreenbaumTransformationMaxSatSolver;

/**
 * A wrapper for the OpenWBO solver.
 * @version 3.0.0
 * @since 1.0
 */
public class MaxSatSolver {

    public static final String SEL_PREFIX = "@SEL_SOFT_";

    public enum Algorithm {
        WBO,
        INC_WBO,
        LINEAR_SU,
        LINEAR_US,
        MSU3,
        WMSU3,
        OLL
    }

    protected final MaxSatConfig configuration;
    protected final Algorithm algorithm;
    protected final PlaistedGreenbaumTransformationMaxSatSolver pgTransformation;
    protected final FormulaFactory f;
    protected final MaxSat solver;
    protected int selectorCounter;
    protected LngResult<MaxSatResult> result;

    /**
     * Constructs a new MaxSAT solver with a given configuration.
     * @param f             the formula factory
     * @param configuration the configuration
     * @param algorithm     the algorithm
     * @throws IllegalArgumentException if the algorithm was unknown
     */
    protected MaxSatSolver(final FormulaFactory f, final MaxSatConfig configuration, final Algorithm algorithm) {
        this.f = f;
        this.algorithm = algorithm;
        this.configuration = configuration;
        result = null;
        switch (algorithm) {
            case WBO:
                solver = new Wbo(f, configuration);
                break;
            case INC_WBO:
                solver = new IncWbo(f, configuration);
                break;
            case LINEAR_SU:
                solver = new LinearSu(f, configuration);
                break;
            case LINEAR_US:
                solver = new LinearUs(f, configuration);
                break;
            case MSU3:
                solver = new Msu3(f, configuration);
                break;
            case WMSU3:
                solver = new Wmsu3(f, configuration);
                break;
            case OLL:
                solver = new Oll(f, configuration);
                break;
            default:
                throw new IllegalArgumentException("Unknown MaxSAT algorithm: " + algorithm);
        }
        pgTransformation = configuration.getCnfMethod() == FACTORY_CNF
                ? null
                : new PlaistedGreenbaumTransformationMaxSatSolver(f, configuration.getCnfMethod() == PG_ON_SOLVER, solver);
    }

    /**
     * Returns a new MaxSAT solver using incremental WBO as algorithm with the
     * MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver incWbo(final FormulaFactory f) {
        return new MaxSatSolver(f, (MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.INC_WBO);
    }

    /**
     * Returns a new MaxSAT solver using incremental WBO as algorithm with the
     * given configuration.
     * @param f      the formula factory
     * @param config the configuration
     * @return the MaxSAT solver
     */
    public static MaxSatSolver incWbo(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.INC_WBO);
    }

    /**
     * Returns a new MaxSAT solver using LinearSU as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver linearSu(final FormulaFactory f) {
        final MaxSatConfig conf = new MaxSatConfig((MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT),
                MaxSatConfig.CardinalityEncoding.MTOTALIZER);
        return new MaxSatSolver(f, conf, Algorithm.LINEAR_SU);
    }

    /**
     * Returns a new MaxSAT solver using LinearSU as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver linearSu(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.LINEAR_SU);
    }

    /**
     * Returns a new MaxSAT solver using LinearUS as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver linearUs(final FormulaFactory f) {
        return new MaxSatSolver(f, (MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.LINEAR_US);
    }

    /**
     * Returns a new MaxSAT solver using LinearUS as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver linearUs(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.LINEAR_US);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver msu3(final FormulaFactory f) {
        return new MaxSatSolver(f, (MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.MSU3);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver msu3(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.MSU3);
    }

    /**
     * Returns a new MaxSAT solver using WBO as algorithm with the MaxSAT
     * configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver wbo(final FormulaFactory f) {
        return new MaxSatSolver(f, (MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.WBO);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the given
     * configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver wbo(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.WBO);
    }

    /**
     * Returns a new MaxSAT solver using weighted MSU3 as algorithm with the
     * MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver wmsu3(final FormulaFactory f) {
        final MaxSatConfig conf = new MaxSatConfig((MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT),
                MaxSatConfig.IncrementalStrategy.ITERATIVE);
        return new MaxSatSolver(f, conf, Algorithm.WMSU3);
    }

    /**
     * Returns a new MaxSAT solver using weighted MSU3 as algorithm with the
     * given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver wmsu3(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.WMSU3);
    }

    /**
     * Returns a new MaxSAT solver using weighted OLL as algorithm with the
     * MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver oll(final FormulaFactory f) {
        final MaxSatConfig conf = new MaxSatConfig((MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT),
                MaxSatConfig.IncrementalStrategy.ITERATIVE);
        return new MaxSatSolver(f, conf, Algorithm.OLL);
    }

    /**
     * Returns a new MaxSAT solver using weighted OLL as algorithm with the
     * given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSatSolver oll(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config, Algorithm.OLL);
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
        final Variable selVar = f.variable(SEL_PREFIX + selectorCounter++);
        addFormulaAsCnf(f.or(selVar.negate(f), formula), -1);
        addFormulaAsCnf(f.or(formula.negate(f), selVar), -1);
        addFormulaAsCnf(selVar, weight);
    }

    /**
     * Saves the current solver state.
     * @return the current solver state
     */
    public MaxSatState saveState() {
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
    public void loadState(final MaxSatState state) {
        result = null;
        solver.loadState(state);
        if (pgTransformation != null) {
            pgTransformation.clearCache();
        }
    }

    private void addFormulaAsCnf(final Formula formula, final int weight) {
        result = null;
        if (configuration.getCnfMethod() == FACTORY_CNF) {
            addCnf(formula.cnf(f), weight);
        } else {
            pgTransformation.addCnfToSolver(formula, weight);
        }
    }

    /**
     * Adds a formula which is already in CNF to the solver.
     * @param formula the formula in CNF
     * @param weight  the weight of this CNF (or -1 for a hard constraint)
     */
    protected void addCnf(final Formula formula, final int weight) {
        switch (formula.getType()) {
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
    public MaxSatResult solve() {
        return solve(NopHandler.get()).getResult();
    }

    /**
     * Solves the formula on the solver and returns the result.
     * @param handler a MaxSAT handler
     * @return the result (SAT, UNSAT, Optimum found, or UNDEF if canceled by
     * the handler)
     */
    public LngResult<MaxSatResult> solve(final ComputationHandler handler) {
        if (result != null && result.isSuccess()) {
            return result;
        }
        if (solver.currentWeight() == 1) {
            solver.setProblemType(MaxSat.ProblemType.UNWEIGHTED);
        } else {
            solver.setProblemType(MaxSat.ProblemType.WEIGHTED);
        }
        result = solver.search(handler);
        return result;
    }

    /**
     * Returns the stats of the underlying solver.
     * @return the stats of the underlying solver
     */
    public MaxSat.Stats getStats() {
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
    public FormulaFactory getFactory() {
        return f;
    }

    @Override
    public String toString() {
        return String.format("MaxSatSolver{result=%s}", result);
    }
}
