// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import static com.booleworks.logicng.datastructures.Tristate.TRUE;
import static com.booleworks.logicng.datastructures.Tristate.UNDEF;

import com.booleworks.logicng.cardinalityconstraints.CCEncoder;
import com.booleworks.logicng.cardinalityconstraints.CCIncrementalData;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.pseudobooleans.PBEncoder;
import com.booleworks.logicng.solvers.functions.SolverFunction;
import com.booleworks.logicng.solvers.sat.MiniSat2Solver;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;
import com.booleworks.logicng.transformations.cnf.PlaistedGreenbaumTransformationSolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Wrapper for the MiniSAT-style SAT solvers.
 * @version 3.0.0
 * @since 1.0
 */
public class MiniSat extends SATSolver {

    protected final MiniSatConfig config;
    protected MiniSatStyleSolver solver;
    protected final PlaistedGreenbaumTransformationSolver pgTransformation;
    protected final PlaistedGreenbaumTransformationSolver fullPgTransformation;
    protected boolean lastComputationWithAssumptions;

    /**
     * Constructs a new SAT solver instance.
     * @param f      the formula factory
     * @param config the MiniSat configuration, must not be {@code null}
     * @throws IllegalArgumentException if the solver style is unknown
     */
    protected MiniSat(final FormulaFactory f, final MiniSatConfig config) {
        super(f);
        this.config = config;
        solver = new MiniSat2Solver(config);
        result = UNDEF;
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, underlyingSolver(), config.initialPhase);
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, underlyingSolver(), config.initialPhase);
    }

    /**
     * Constructs a new MiniSat solver with a given underlying solver core.
     * This method is primarily used for serialization purposes and should not be required in any
     * other application use case.
     * @param f                the formula factory
     * @param underlyingSolver the underlying solver core
     */
    public MiniSat(final FormulaFactory f, final MiniSatStyleSolver underlyingSolver) {
        super(f);
        config = underlyingSolver.getConfig();
        solver = underlyingSolver;
        result = UNDEF;
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, underlyingSolver, config.initialPhase);
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, underlyingSolver, config.initialPhase);
    }

    /**
     * Returns a new MiniSat solver with the MiniSat configuration from the formula factory.
     * @param f the formula factory
     * @return the solver
     */
    public static MiniSat miniSat(final FormulaFactory f) {
        return new MiniSat(f, (MiniSatConfig) f.configurationFor(ConfigurationType.MINISAT));
    }

    /**
     * Returns a new MiniSat solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration, must not be {@code null}
     * @return the solver
     */
    public static MiniSat miniSat(final FormulaFactory f, final MiniSatConfig config) {
        return new MiniSat(f, config);
    }

    @Override
    public void add(final Formula formula, final Proposition proposition) {
        result = UNDEF;
        if (formula.type() == FType.PBC) {
            final PBConstraint constraint = (PBConstraint) formula;
            if (constraint.isCC()) {
                if (config.useAtMostClauses) {
                    if (constraint.comparator() == CType.LE) {
                        ((MiniSat2Solver) solver).addAtMost(generateClauseVector(constraint.operands()), constraint.rhs());
                    } else if (constraint.comparator() == CType.LT && constraint.rhs() > 3) {
                        ((MiniSat2Solver) solver).addAtMost(generateClauseVector(constraint.operands()), constraint.rhs() - 1);
                    } else if (constraint.comparator() == CType.EQ && constraint.rhs() == 1) {
                        ((MiniSat2Solver) solver).addAtMost(generateClauseVector(constraint.operands()), constraint.rhs());
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
        if (config.getCnfMethod() == MiniSatConfig.CNFMethod.FACTORY_CNF) {
            addClauseSet(formula.cnf(f), proposition);
        } else if (config.getCnfMethod() == MiniSatConfig.CNFMethod.PG_ON_SOLVER) {
            pgTransformation.addCNFtoSolver(formula, proposition);
        } else if (config.getCnfMethod() == MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER) {
            fullPgTransformation.addCNFtoSolver(formula, proposition);
        } else {
            throw new IllegalStateException("Unknown Solver CNF method: " + config.getCnfMethod());
        }
    }

    @Override
    public CCIncrementalData addIncrementalCC(final CardinalityConstraint cc) {
        final EncodingResult result = EncodingResult.resultForMiniSat(f, this, null);
        return CCEncoder.encodeIncremental(cc, result);
    }

    @Override
    protected void addClause(final Formula formula, final Proposition proposition) {
        result = UNDEF;
        final LNGIntVector ps = generateClauseVector(formula.literals(f));
        solver.addClause(ps, proposition);
    }

    @Override
    public Tristate sat(final SATHandler handler) {
        if (lastResultIsUsable()) {
            return result;
        }
        result = solver.solve(handler);
        lastComputationWithAssumptions = false;
        return result;
    }

    @Override
    public Tristate sat(final SATHandler handler, final Literal literal) {
        final LNGIntVector clauseVec = new LNGIntVector(1);
        final int index = getOrAddIndex(literal);
        final int litNum = literal.phase() ? index * 2 : (index * 2) ^ 1;
        clauseVec.push(litNum);
        result = solver.solve(handler, clauseVec);
        lastComputationWithAssumptions = true;
        return result;
    }

    @Override
    public Tristate sat(final SATHandler handler, final Collection<? extends Literal> assumptions) {
        final LNGIntVector assumptionVec = generateClauseVector(assumptions);
        result = solver.solve(handler, assumptionVec);
        lastComputationWithAssumptions = true;
        return result;
    }

    @Override
    public void reset() {
        solver.reset();
        lastComputationWithAssumptions = false;
        pgTransformation.clearCache();
        fullPgTransformation.clearCache();
        result = UNDEF;
    }

    @Override
    public Assignment model(final Collection<Variable> variables) {
        if (result == UNDEF) {
            throw new IllegalStateException("Cannot get a model as long as the formula is not solved.  Call 'sat' first.");
        }
        final LNGIntVector relevantIndices = variables == null ? null : new LNGIntVector(variables.size());
        if (relevantIndices != null) {
            for (final Variable var : variables) {
                relevantIndices.push(solver.idxForName(var.name()));
            }
        }
        return result == TRUE ? createAssignment(solver.model(), relevantIndices) : null;
    }

    @Override
    public <RESULT> RESULT execute(final SolverFunction<RESULT> function) {
        return function.apply(this, this::setResult);
    }

    @Override
    public SolverState saveState() {
        return solver.saveState();
    }

    @Override
    public void loadState(final SolverState state) {
        solver.loadState(state);
        result = UNDEF;
        pgTransformation.clearCache();
        fullPgTransformation.clearCache();
    }

    @Override
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
            index = solver.newVar(!config.initialPhase, true);
            solver.addName(lit.name(), index);
        }
        return index;
    }

    /**
     * Creates an assignment from a Boolean vector of the solver.
     * @param vec             the vector of the solver
     * @param relevantIndices the solver's indices of the relevant variables for the model.
     * @return the assignment
     */
    public Assignment createAssignment(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
        return new Assignment(createLiterals(vec, relevantIndices));
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
     * Returns the initial phase of literals of this solver.
     * @return the initial phase of literals of this solver
     */
    public boolean initialPhase() {
        return config.initialPhase;
    }

    @Override
    public String toString() {
        return String.format("%s{result=%s, incremental=%s}", solver.getClass().getSimpleName(), result, config.incremental);
    }

    protected boolean lastResultIsUsable() {
        return result != UNDEF && !lastComputationWithAssumptions;
    }

    /**
     * Returns this solver's configuration.
     * @return this solver's configuration
     */
    public MiniSatConfig getConfig() {
        return config;
    }

    @Override
    public void setSelectionOrder(final List<? extends Literal> selectionOrder) {
        solver.setSelectionOrder(selectionOrder);
    }

    @Override
    public void resetSelectionOrder() {
        solver.resetSelectionOrder();
    }

    @Override
    public boolean canSaveLoadState() {
        return config.incremental;
    }

    @Override
    public boolean canGenerateProof() {
        return config.proofGeneration();
    }

    /**
     * Returns whether this solver is incremental.
     * @return {@code true} if this solver is incremental, {@code false} otherwise
     */
    public boolean isIncremental() {
        return config.incremental;
    }

    /**
     * Returns the current result, e.g. the result of the last {@link #sat()} call.
     * @return the current result
     */
    public Tristate getResult() {
        return result;
    }

    protected void setResult(final Tristate tristate) {
        result = tristate;
    }

    /**
     * Returns whether the last computation was using assumption literals.
     * @return {@code true} if the last computation used assumption literals, {@code false} otherwise
     */
    public boolean isLastComputationWithAssumptions() {
        return lastComputationWithAssumptions;
    }
}
