// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT.MaxSATResult.OPTIMUM;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT.MaxSATResult.UNDEF;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT.MaxSATResult.UNSATISFIABLE;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.MaxSATHandler;
import com.booleworks.logicng.solvers.maxsat.algorithms.IncWBO;
import com.booleworks.logicng.solvers.maxsat.algorithms.LinearSU;
import com.booleworks.logicng.solvers.maxsat.algorithms.LinearUS;
import com.booleworks.logicng.solvers.maxsat.algorithms.MSU3;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.OLL;
import com.booleworks.logicng.solvers.maxsat.algorithms.WBO;
import com.booleworks.logicng.solvers.maxsat.algorithms.WMSU3;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A wrapper for the OpenWBO solver.
 * @version 3.0.0
 * @since 1.0
 */
public class MaxSATSolver {

    private static final String SEL_PREFIX = "@SEL_SOFT_";

    protected enum Algorithm {WBO, INC_WBO, LINEAR_SU, LINEAR_US, MSU3, WMSU3, OLL}

    protected final MaxSATConfig configuration;
    protected final Algorithm algorithm;
    protected FormulaFactory f;
    protected MaxSAT.MaxSATResult result;
    protected MaxSAT solver;
    protected SortedMap<Variable, Integer> var2index;
    protected SortedMap<Integer, Variable> index2var;
    protected SortedSet<Variable> selectorVariables;

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
        reset();
    }

    /**
     * Returns a new MaxSAT solver using incremental WBO as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver incWBO(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.INC_WBO);
    }

    /**
     * Returns a new MaxSAT solver using incremental WBO as algorithm with the given configuration.
     * @param f      the formula factory
     * @param config the configuration
     * @return the MaxSAT solver
     */
    public static MaxSATSolver incWBO(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.INC_WBO);
    }

    /**
     * Returns a new MaxSAT solver using LinearSU as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearSU(final FormulaFactory f) {
        final MaxSATConfig conf = new MaxSATConfig((MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), MaxSATConfig.CardinalityEncoding.MTOTALIZER);
        return new MaxSATSolver(f, conf, Algorithm.LINEAR_SU);
    }

    /**
     * Returns a new MaxSAT solver using LinearSU as algorithm with the given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearSU(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.LINEAR_SU);
    }

    /**
     * Returns a new MaxSAT solver using LinearUS as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearUS(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.LINEAR_US);
    }

    /**
     * Returns a new MaxSAT solver using LinearUS as algorithm with the given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver linearUS(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.LINEAR_US);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver msu3(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.MSU3);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver msu3(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.MSU3);
    }

    /**
     * Returns a new MaxSAT solver using WBO as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wbo(final FormulaFactory f) {
        return new MaxSATSolver(f, (MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), Algorithm.WBO);
    }

    /**
     * Returns a new MaxSAT solver using MSU3 as algorithm with the given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wbo(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.WBO);
    }

    /**
     * Returns a new MaxSAT solver using weighted MSU3 as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wmsu3(final FormulaFactory f) {
        final MaxSATConfig conf = new MaxSATConfig((MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), MaxSATConfig.IncrementalStrategy.ITERATIVE);
        return new MaxSATSolver(f, conf, Algorithm.WMSU3);
    }

    /**
     * Returns a new MaxSAT solver using weighted MSU3 as algorithm with the given configuration.
     * @param config the configuration
     * @param f      the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver wmsu3(final FormulaFactory f, final MaxSATConfig config) {
        return new MaxSATSolver(f, config, Algorithm.WMSU3);
    }

    /**
     * Returns a new MaxSAT solver using weighted OLL as algorithm with the MaxSAT configuration from the formula factory.
     * @param f the formula factory
     * @return the MaxSAT solver
     */
    public static MaxSATSolver oll(final FormulaFactory f) {
        final MaxSATConfig conf = new MaxSATConfig((MaxSATConfig) f.configurationFor(ConfigurationType.MAXSAT), MaxSATConfig.IncrementalStrategy.ITERATIVE);
        return new MaxSATSolver(f, conf, Algorithm.OLL);
    }

    /**
     * Returns a new MaxSAT solver using weighted OLL as algorithm with the given configuration.
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
        return algorithm == Algorithm.INC_WBO || algorithm == Algorithm.WMSU3 || algorithm == Algorithm.WBO || algorithm == Algorithm.OLL;
    }

    /**
     * Resets the solver.
     * @throws IllegalArgumentException if the algorithm was unknown
     */
    public void reset() {
        result = UNDEF;
        var2index = new TreeMap<>();
        index2var = new TreeMap<>();
        selectorVariables = new TreeSet<>();
        switch (algorithm) {
            case WBO:
                solver = new WBO(configuration);
                break;
            case INC_WBO:
                solver = new IncWBO(configuration);
                break;
            case LINEAR_SU:
                solver = new LinearSU(configuration);
                break;
            case LINEAR_US:
                solver = new LinearUS(configuration);
                break;
            case MSU3:
                solver = new MSU3(configuration);
                break;
            case WMSU3:
                solver = new WMSU3(configuration);
                break;
            case OLL:
                solver = new OLL(configuration);
                break;
            default:
                throw new IllegalArgumentException("Unknown MaxSAT algorithm: " + algorithm);
        }
    }

    /**
     * Adds a new hard formula to the solver.  Hard formulas must always be true.
     * @param formula the formula
     * @throws IllegalStateException if a formula is added to a solver which is already solved.
     */
    public void addHardFormula(final Formula formula) {
        if (result != UNDEF) {
            throw new IllegalStateException("The MaxSAT solver does currently not support an incremental interface.  Reset the solver.");
        }
        addCNF(formula.cnf(f), -1);
    }

    /**
     * Adds a new soft formula to the solver.
     * @param formula the formula
     * @param weight  the weight
     * @throws IllegalStateException    if a formula is added to a solver which is already solved.
     * @throws IllegalArgumentException if the weight is &lt;1
     */
    public void addSoftFormula(final Formula formula, final int weight) {
        if (result != UNDEF) {
            throw new IllegalStateException("The MaxSAT solver does currently not support an incremental interface.  Reset the solver.");
        }
        if (weight < 1) {
            throw new IllegalArgumentException("The weight of a formula must be > 0");
        }
        final Variable selVar = f.variable(SEL_PREFIX + selectorVariables.size());
        selectorVariables.add(selVar);
        addHardFormula(f.or(selVar.negate(f), formula));
        addHardFormula(f.or(formula.negate(f), selVar));
        addClause(selVar, weight);
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
                addClause(formula, weight);
                break;
            case AND:
                for (final Formula op : formula) {
                    addClause(op, weight);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + formula);
        }
    }

    /**
     * Adds a clause to the solver.
     * @param formula the clause
     * @param weight  the weight of the clause (or -1 for a hard clause)
     */
    protected void addClause(final Formula formula, final int weight) {
        result = UNDEF;
        final LNGIntVector clauseVec = new LNGIntVector((int) formula.numberOfAtoms(f));
        for (final Literal lit : formula.literals(f)) {
            Integer index = var2index.get(lit.variable());
            if (index == null) {
                index = solver.newLiteral(false) >> 1;
                var2index.put(lit.variable(), index);
                index2var.put(index, lit.variable());
            }
            final int litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
            clauseVec.push(litNum);
        }
        if (weight == -1) {
            solver.addHardClause(clauseVec);
        } else {
            solver.setCurrentWeight(weight);
            solver.updateSumWeights(weight);
            solver.addSoftClause(weight, clauseVec);
        }
    }

    /**
     * Solves the formula on the solver and returns the result.
     * @return the result (SAT, UNSAT, Optimum found)
     */
    public MaxSAT.MaxSATResult solve() {
        return solve(null);
    }

    /**
     * Solves the formula on the solver and returns the result.
     * @param handler a MaxSAT handler
     * @return the result (SAT, UNSAT, Optimum found, or UNDEF if canceled by the handler)
     */
    public MaxSAT.MaxSATResult solve(final MaxSATHandler handler) {
        if (result != UNDEF) {
            return result;
        }
        if (solver.currentWeight() == 1) {
            solver.setProblemType(MaxSAT.ProblemType.UNWEIGHTED);
        } else {
            solver.setProblemType(MaxSAT.ProblemType.WEIGHTED);
        }
        result = solver.search(handler);
        return result;
    }

    /**
     * Returns the minimum weight (or number of clauses if unweighted) of clauses which have to be unsatisfied.
     * Therefore, if the minimum number of weights is 0, the formula is satisfiable.
     * @return the minimum weight of clauses which have to be unsatisfied
     * @throws IllegalStateException if the formula is not yet solved
     */
    public int result() {
        if (result == UNDEF) {
            throw new IllegalStateException("Cannot get a result as long as the formula is not solved.  Call 'solver' first.");
        }
        return result == OPTIMUM ? solver.result() : -1;
    }

    /**
     * Returns the model of the current result.
     * @return the model of the current result
     * @throws IllegalStateException if the formula is not yet solved
     */
    public Assignment model() {
        if (result == UNDEF) {
            throw new IllegalStateException("Cannot get a model as long as the formula is not solved.  Call 'solver' first.");
        }
        return result != UNSATISFIABLE ? createAssignment(solver.model()) : null;
    }

    /**
     * Creates an assignment from a Boolean vector of the solver.
     * @param vec the vector of the solver
     * @return the assignment
     */
    protected Assignment createAssignment(final LNGBooleanVector vec) {
        final Assignment model = new Assignment();
        for (int i = 0; i < vec.size(); i++) {
            final Literal lit = index2var.get(i);
            if (lit != null && !selectorVariables.contains(lit.variable())) {
                if (vec.get(i)) {
                    model.addLiteral(lit);
                } else {
                    model.addLiteral(lit.negate(f));
                }
            }
        }
        return model;
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
        return String.format("MaxSATSolver{result=%s, var2index=%s}", result, var2index);
    }
}
