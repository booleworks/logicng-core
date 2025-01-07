package com.booleworks.logicng.solvers;

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.OLL;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.WMSU3;
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

    protected final MaxSatConfig configuration;
    protected final PlaistedGreenbaumTransformationMaxSatSolver pgTransformation;
    protected final FormulaFactory f;
    protected final MaxSat solver;
    protected int selectorCounter;
    protected LngResult<MaxSatResult> result;

    /**
     * Constructs a new MaxSAT solver with a given configuration.
     * @param f             the formula factory
     * @param configuration the configuration
     * @throws IllegalArgumentException if the algorithm was unknown
     */
    protected MaxSatSolver(final FormulaFactory f, final MaxSatConfig configuration) {
        this.f = f;
        this.configuration = configuration;
        solver = initSolver(configuration);
        pgTransformation = configuration.getCnfMethod() == FACTORY_CNF
                           ? null
                           : new PlaistedGreenbaumTransformationMaxSatSolver(f,
                                   configuration.getCnfMethod() == PG_ON_SOLVER, solver);
    }

    private MaxSat initSolver(final MaxSatConfig configuration) {
        switch (configuration.getAlgorithm()) {
            case WBO:
                return new Wbo(f, configuration);
            case INC_WBO:
                return new IncWbo(f, configuration);
            case LINEAR_SU:
                return new LinearSu(f, configuration);
            case LINEAR_US:
                return new LinearUs(f, configuration);
            case MSU3:
                return new Msu3(f, configuration);
            case WMSU3:
                return new Wmsu3(f, configuration);
            case OLL:
                return new Oll(f, configuration);
            default:
                throw new IllegalArgumentException("Unknown MaxSAT algorithm: " + configuration.getAlgorithm());
        }
    }

    /**
     * Returns a new MaxSAT solver with the solver configuration from the formula
     * factory.
     * @param f the formula factory
     * @return the solver
     */
    public static MaxSatSolver newSolver(final FormulaFactory f) {
        return new MaxSatSolver(f, (MaxSatConfig) f.configurationFor(ConfigurationType.MAXSAT));
    }

    /**
     * Returns a new MaxSAT solver with the given configuration.
     * @param f      the formula factory
     * @param config the configuration
     * @return the solver
     */
    public static MaxSatSolver newSolver(final FormulaFactory f, final MaxSatConfig config) {
        return new MaxSatSolver(f, config);
    }

    /**
     * Returns whether this solver can handle weighted instances or not.
     * @return whether this solver can handle weighted instances or not
     */
    public boolean supportsWeighted() {
        final MaxSatConfig.Algorithm algorithm = configuration.getAlgorithm();
        return algorithm == INC_WBO || algorithm == WMSU3 || algorithm == WBO || algorithm == OLL;
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
     * Erases the cached result.
     * <p>
     * The result of the last computation is cached and reused if the state of
     * the solver does not change. However, it does not recognize manipulation
     * of the underlying solver. It is necessary to reset the solver's result
     * manually in those cases. It is not necessary to manually reset the
     * solver's result in all other cases.
     */
    public void resetResult() {
        result = null;
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
    public MaxSatConfig.Algorithm getAlgorithm() {
        return configuration.getAlgorithm();
    }

    /**
     * Returns the formula factory for this solver.
     * @return the formula factory
     */
    public FormulaFactory getFactory() {
        return f;
    }

    /**
     * Returns the underlying MaxSat solver.
     * <p>
     * ATTENTION: by influencing the underlying solver directly, you can mess
     * things up completely! You should really know what you are doing.
     * @return the underlying solver
     */
    public MaxSat getUnderlyingSolver() {
        return solver;
    }

    @Override
    public String toString() {
        return String.format("MaxSatSolver{result=%s}", result);
    }
}
