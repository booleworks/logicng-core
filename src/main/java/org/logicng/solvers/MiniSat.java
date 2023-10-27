// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.solvers;

import static org.logicng.datastructures.Tristate.TRUE;
import static org.logicng.datastructures.Tristate.UNDEF;

import org.logicng.cardinalityconstraints.CCEncoder;
import org.logicng.cardinalityconstraints.CCIncrementalData;
import org.logicng.collections.LNGBooleanVector;
import org.logicng.collections.LNGIntVector;
import org.logicng.configurations.ConfigurationType;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.EncodingResult;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.CType;
import org.logicng.formulas.CardinalityConstraint;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.Variable;
import org.logicng.handlers.SATHandler;
import org.logicng.propositions.Proposition;
import org.logicng.solvers.functions.SolverFunction;
import org.logicng.solvers.sat.GlucoseConfig;
import org.logicng.solvers.sat.GlucoseSyrup;
import org.logicng.solvers.sat.MiniCard;
import org.logicng.solvers.sat.MiniSat2Solver;
import org.logicng.solvers.sat.MiniSatConfig;
import org.logicng.solvers.sat.MiniSatStyleSolver;
import org.logicng.transformations.cnf.PlaistedGreenbaumTransformationSolver;

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

    public enum SolverStyle {MINISAT, GLUCOSE, MINICARD}

    protected final MiniSatConfig config;
    protected MiniSatStyleSolver solver;
    protected SolverStyle style;
    protected LNGIntVector validStates;
    protected final boolean initialPhase;
    protected final boolean incremental;
    protected int nextStateId;
    protected final PlaistedGreenbaumTransformationSolver pgTransformation;
    protected final PlaistedGreenbaumTransformationSolver fullPgTransformation;
    protected boolean lastComputationWithAssumptions;

    /**
     * Constructs a new SAT solver instance.
     * @param f             the formula factory
     * @param solverStyle   the solver style
     * @param miniSatConfig the MiniSat configuration, must not be {@code null}
     * @param glucoseConfig the Glucose configuration, must not be {@code null} for solver type {@link SolverStyle#GLUCOSE}
     * @throws IllegalArgumentException if the solver style is unknown
     */
    protected MiniSat(final FormulaFactory f, final SolverStyle solverStyle, final MiniSatConfig miniSatConfig,
                      final GlucoseConfig glucoseConfig) {
        super(f);
        config = miniSatConfig;
        style = solverStyle;
        initialPhase = miniSatConfig.initialPhase();
        switch (solverStyle) {
            case MINISAT:
                solver = new MiniSat2Solver(miniSatConfig);
                break;
            case GLUCOSE:
                solver = new GlucoseSyrup(miniSatConfig, glucoseConfig);
                break;
            case MINICARD:
                solver = new MiniCard(miniSatConfig);
                break;
            default:
                throw new IllegalArgumentException("Unknown solver style: " + solverStyle);
        }
        result = UNDEF;
        incremental = miniSatConfig.incremental();
        validStates = new LNGIntVector();
        nextStateId = 0;
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, underlyingSolver(), initialPhase);
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, underlyingSolver(), initialPhase);
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
        if (underlyingSolver instanceof MiniSat2Solver) {
            style = SolverStyle.MINISAT;
        } else if (underlyingSolver instanceof MiniCard) {
            style = SolverStyle.MINICARD;
        } else if (underlyingSolver instanceof GlucoseSyrup) {
            style = SolverStyle.GLUCOSE;
        } else {
            throw new IllegalArgumentException("Unknown solver type: " + underlyingSolver.getClass());
        }
        initialPhase = underlyingSolver.getConfig().initialPhase();
        solver = underlyingSolver;
        result = UNDEF;
        incremental = underlyingSolver.getConfig().incremental();
        validStates = new LNGIntVector();
        nextStateId = 0;
        pgTransformation = new PlaistedGreenbaumTransformationSolver(f, true, underlyingSolver, initialPhase);
        fullPgTransformation = new PlaistedGreenbaumTransformationSolver(f, false, underlyingSolver, initialPhase);
    }

    /**
     * Returns a new MiniSat solver with the MiniSat configuration from the formula factory.
     * @param f the formula factory
     * @return the solver
     */
    public static MiniSat miniSat(final FormulaFactory f) {
        return new MiniSat(f, SolverStyle.MINISAT, (MiniSatConfig) f.configurationFor(ConfigurationType.MINISAT), null);
    }

    /**
     * Returns a new MiniSat solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration, must not be {@code null}
     * @return the solver
     */
    public static MiniSat miniSat(final FormulaFactory f, final MiniSatConfig config) {
        return new MiniSat(f, SolverStyle.MINISAT, config, null);
    }

    /**
     * Returns a new Glucose solver with the MiniSat and Glucose configuration from the formula factory.
     * @param f the formula factory
     * @return the solver
     */
    public static MiniSat glucose(final FormulaFactory f) {
        return new MiniSat(f, SolverStyle.GLUCOSE, (MiniSatConfig) f.configurationFor(ConfigurationType.MINISAT),
                (GlucoseConfig) f.configurationFor(ConfigurationType.GLUCOSE));
    }

    /**
     * Returns a new Glucose solver with a given configuration.
     * @param f             the formula factory
     * @param miniSatConfig the MiniSat configuration, must not be {@code null}
     * @param glucoseConfig the Glucose configuration, must not be {@code null}
     * @return the solver
     */
    public static MiniSat glucose(final FormulaFactory f, final MiniSatConfig miniSatConfig, final GlucoseConfig glucoseConfig) {
        return new MiniSat(f, SolverStyle.GLUCOSE, miniSatConfig, glucoseConfig);
    }

    /**
     * Returns a new MiniCard solver with the MiniSat configuration from the formula factory.
     * @param f the formula factory
     * @return the solver
     */
    public static MiniSat miniCard(final FormulaFactory f) {
        return new MiniSat(f, SolverStyle.MINICARD, (MiniSatConfig) f.configurationFor(ConfigurationType.MINISAT), null);
    }

    /**
     * Returns a new MiniCard solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration, must not be {@code null}
     * @return the solver
     */
    public static MiniSat miniCard(final FormulaFactory f, final MiniSatConfig config) {
        return new MiniSat(f, SolverStyle.MINICARD, config, null);
    }

    /**
     * Returns a new solver depending on the given solver style with the configuration from the formula factory.
     * @param f     the formula factory
     * @param style the solver style, must not be {@code null}
     * @return the solver
     */
    public static MiniSat mk(final FormulaFactory f, final SolverStyle style) {
        final MiniSatConfig miniSatConfig = (MiniSatConfig) f.configurationFor(ConfigurationType.MINISAT);
        final GlucoseConfig glucoseConfig = style == SolverStyle.GLUCOSE ? (GlucoseConfig) f.configurationFor(ConfigurationType.GLUCOSE) : null;
        return mk(f, style, miniSatConfig, glucoseConfig);
    }

    /**
     * Returns a new solver depending on the given solver style with the given configuration.
     * @param f             the formula factory
     * @param solverStyle   the solver style, must not be {@code null}
     * @param miniSatConfig the configuration, must not be {@code null}
     * @param glucoseConfig the Glucose configuration, must not be {@code null} for solver type {@link SolverStyle#GLUCOSE}
     * @return the solver
     */
    public static MiniSat mk(final FormulaFactory f, final SolverStyle solverStyle, final MiniSatConfig miniSatConfig, final GlucoseConfig glucoseConfig) {
        switch (solverStyle) {
            case MINISAT:
                return miniSat(f, miniSatConfig);
            case GLUCOSE:
                return glucose(f, miniSatConfig, glucoseConfig);
            case MINICARD:
                return miniCard(f, miniSatConfig);
            default:
                throw new IllegalArgumentException("Unknown solver style: " + solverStyle);
        }
    }

    @Override
    public void add(final Formula formula, final Proposition proposition) {
        result = UNDEF;
        if (formula.type() == FType.PBC) {
            final PBConstraint constraint = (PBConstraint) formula;
            if (constraint.isCC()) {
                if (style == SolverStyle.MINICARD) {
                    if (constraint.comparator() == CType.LE) {
                        ((MiniCard) solver).addAtMost(generateClauseVector(constraint.operands()), constraint.rhs());
                    } else if (constraint.comparator() == CType.LT && constraint.rhs() > 3) {
                        ((MiniCard) solver).addAtMost(generateClauseVector(constraint.operands()), constraint.rhs() - 1);
                    } else if (constraint.comparator() == CType.EQ && constraint.rhs() == 1) {
                        ((MiniCard) solver).addAtMost(generateClauseVector(constraint.operands()), constraint.rhs());
                        solver.addClause(generateClauseVector(constraint.operands()), proposition);
                    } else {
                        addFormulaAsCNF(constraint, proposition);
                    }
                } else {
                    final EncodingResult result = EncodingResult.resultForMiniSat(f, this, proposition);
                    CCEncoder.encode((CardinalityConstraint) constraint, result);
                }
            } else {
                addFormulaAsCNF(constraint, proposition);
            }
        } else {
            addFormulaAsCNF(formula, proposition);
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
        final int id = nextStateId++;
        validStates.push(id);
        return new SolverState(id, solver.saveState());
    }

    @Override
    public void loadState(final SolverState state) {
        int index = -1;
        for (int i = validStates.size() - 1; i >= 0 && index == -1; i--) {
            if (validStates.get(i) == state.id()) {
                index = i;
            }
        }
        if (index == -1) {
            throw new IllegalArgumentException("The given solver state is not valid anymore.");
        }
        validStates.shrinkTo(index + 1);
        solver.loadState(state.state());
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
            index = solver.newVar(!initialPhase, true);
            solver.addName(lit.name(), index);
        }
        return index;
    }

    /**
     * Creates an assignment from a Boolean vector of the solver. The created assignment is not fast evaluable.
     * @param vec             the vector of the solver
     * @param relevantIndices the solver's indices of the relevant variables for the model.  If {@code null}, all
     *                        variables are relevant.
     * @return the assignment
     */
    public Assignment createAssignment(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
        return createAssignment(vec, relevantIndices, false);
    }

    /**
     * Creates an assignment from a Boolean vector of the solver. The flag {@code fastEvaluable} determines if the created
     * assignment is {@link Assignment#fastEvaluable() fast evaluable} assignment.
     * @param vec             the vector of the solver
     * @param relevantIndices the solver's indices of the relevant variables for the model.  If {@code null}, all
     *                        variables are relevant.
     * @param fastEvaluable   {@code true} if the created assignment should be fast evaluable, otherwise {@code false}
     * @return the assignment
     */
    public Assignment createAssignment(final LNGBooleanVector vec, final LNGIntVector relevantIndices, final boolean fastEvaluable) {
        final Assignment model = new Assignment(fastEvaluable);
        if (relevantIndices == null) {
            for (int i = 0; i < vec.size(); i++) {
                final String name = solver.nameForIdx(i);
                if (isRelevantVariable(name)) {
                    model.addLiteral(f.literal(name, vec.get(i)));
                }
            }
        } else {
            for (int i = 0; i < relevantIndices.size(); i++) {
                final int index = relevantIndices.get(i);
                if (index != -1) {
                    final String name = solver.nameForIdx(index);
                    if (isRelevantVariable(name)) {
                        model.addLiteral(f.literal(name, vec.get(index)));
                    }
                }
            }
        }
        return model;
    }

    /**
     * Returns whether a variable on the solver is relevant or an auxiliary variable.
     * @param name the name of the variable
     * @return true if it is relevant, false if it is an auxiliary variable
     */
    public boolean isRelevantVariable(final String name) {
        return config.isAuxiliaryVariablesInModels() || (!name.startsWith(FormulaFactory.CNF_PREFIX) &&
                !name.startsWith(FormulaFactory.CC_PREFIX) && !name.startsWith(FormulaFactory.PB_PREFIX));
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
        return initialPhase;
    }

    @Override
    public String toString() {
        return String.format("%s{result=%s, incremental=%s}", solver.getClass().getSimpleName(), result, incremental);
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
        return (style == SolverStyle.MINISAT || style == SolverStyle.MINICARD) && incremental;
    }

    @Override
    public boolean canGenerateProof() {
        return config.proofGeneration() && (style == SolverStyle.MINISAT || style == SolverStyle.GLUCOSE && !incremental);
    }

    /**
     * Returns this solver's style.
     * @return this solver's style
     */
    public SolverStyle getStyle() {
        return style;
    }

    /**
     * Returns whether this solver is incremental
     * @return {@code true} if this solver is incremental, {@code false} otherwise
     */
    public boolean isIncremental() {
        return incremental;
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
