// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.explanations.mus.MusConfig;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.formulas.implementation.noncaching.NonCachingFormulaFactory;
import com.booleworks.logicng.functions.SubNodeFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import com.booleworks.logicng.transformations.FormulaFactoryImporter;
import com.booleworks.logicng.transformations.cnf.CnfConfig;
import com.booleworks.logicng.transformations.simplification.AdvancedSimplifierConfig;
import com.booleworks.logicng.util.FormulaRandomizerConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The formula factory for LogicNG. Formulas in LogicNG can only be generated
 * and managed by a formula factory.
 *
 * <h2>Formula Factory Implementations</h2>
 * <p>
 * There are two types of formula factories: {@link CachingFormulaFactory}
 * and {@link NonCachingFormulaFactory}.
 * <p>
 * The caching formula factory keeps track of all formulas it creates, and it
 * guarantees that equivalent formulas (in terms of associativity and
 * commutativity) are hold exactly once in memory.
 * <p>
 * The non-caching formula factory does not keep track of all formulas (it only
 * keeps track of variables), and it can, therefore, not avoid creating multiple
 * instances of the same formulas. The non-caching formula factory can be
 * beneficial in some use-cases that create many unique or short-lived formulas,
 * or in some multithreaded use cases.
 *
 * <h2>Modes</h2>
 * <p>
 * A formula factory can be in one of two modes: WRITE or READ-ONLY.
 * <p>
 * The WRITE mode is the default mode and allows all read and write operations.
 * <p>
 * The READ-ONLY mode only allows a limited subset of operations that do not
 * modify shared data, like creating formulas and variables. The READ-ONLY mode
 * is always thread-safe.
 *
 * <h2>Thread-Safety</h2>
 * <table>
 *   <tr>
 *     <th></th>
 *     <th>WRITE</th>
 *     <th>READ-ONLY</th>
 *   </tr>
 *   <tr>
 *     <td><strong>Caching</strong></td>
 *     <td>No*</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td><strong>Non-Caching</strong></td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *   </tr>
 *   <caption>Thread-safety by mode and type</caption>
 * </table>
 * (Yes: Factory is thread-safe), (No: Factory is not thread-safe)
 * <p>
 * (*): Can be configured to be thread-safe by setting the `threadSafe` flag in
 * {@link FormulaFactoryConfig}.
 */
public abstract class FormulaFactory {

    protected final String name;
    protected final FormulaFactoryConfig.FormulaMergeStrategy formulaMergeStrategy;
    protected final boolean simplifyComplementaryOperands;
    protected final Map<ConfigurationType, Configuration> configurations;
    protected final ThreadLocal<SubNodeFunction> subformulaFunction;
    protected Map<String, AtomicInteger> auxVarCounters;
    protected final String auxVarPrefix;
    protected CFalse cFalse;
    protected CTrue cTrue;
    protected final ThreadLocal<FormulaFactoryImporter> importer;
    protected final boolean threadSafe;
    volatile protected boolean readOnly;

    /**
     * Constructs an empty {@link CachingFormulaFactory} with the passed
     * configuration.
     * @param config configuration for the factory
     * @return an empty caching formula factory
     */
    public static CachingFormulaFactory caching(final FormulaFactoryConfig config) {
        return new CachingFormulaFactory(config);
    }

    /**
     * Constructs an empty {@link CachingFormulaFactory} with the default
     * configuration.
     * @return an empty caching formula factory
     */
    public static CachingFormulaFactory caching() {
        return new CachingFormulaFactory();
    }

    /**
     * Constructs an empty {@link NonCachingFormulaFactory} with the passed
     * configuration.
     * @param config configuration for the factory
     * @return an empty non-caching formula factory
     */
    public static NonCachingFormulaFactory nonCaching(final FormulaFactoryConfig config) {
        return new NonCachingFormulaFactory(config);
    }

    /**
     * Constructs an empty {@link NonCachingFormulaFactory} with the default
     * configuration.
     * @return an empty non-caching formula factory
     */
    public static NonCachingFormulaFactory nonCaching() {
        return new NonCachingFormulaFactory();
    }

    protected FormulaFactory(final FormulaFactoryConfig config) {
        name = config.name;
        formulaMergeStrategy = config.formulaMergeStrategy;
        threadSafe = config.threadSafe;
        if (config.formulaMergeStrategy == FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT &&
                this instanceof CachingFormulaFactory) {
            throw new IllegalArgumentException(
                    "The USE_BUT_NO_IMPORT merge strategy can only be used for non-caching formula factories.");
        }
        simplifyComplementaryOperands = config.simplifyComplementaryOperands;
        configurations = initDefaultConfigs();
        auxVarPrefix = "@AUX_" + name + "_";
        initCaches();
        readOnly = false;
        importer = ThreadLocal.withInitial(() -> new FormulaFactoryImporter(this));
        subformulaFunction = ThreadLocal.withInitial(() -> new SubNodeFunction(this));
    }

    /**
     * Init all configurations with the default configurations.
     */
    private static Map<ConfigurationType, Configuration> initDefaultConfigs() {
        final Map<ConfigurationType, Configuration> configMap = new ConcurrentHashMap<>();
        configMap.put(ConfigurationType.CNF, CnfConfig.builder().build());
        configMap.put(ConfigurationType.ENCODER, EncoderConfig.builder().build());
        configMap.put(ConfigurationType.SAT, SatSolverConfig.builder().build());
        configMap.put(ConfigurationType.MAXSAT, MaxSatConfig.builder().build());
        configMap.put(ConfigurationType.MUS, MusConfig.builder().build());
        configMap.put(ConfigurationType.ADVANCED_SIMPLIFIER, AdvancedSimplifierConfig.builder().build());
        configMap.put(ConfigurationType.MODEL_ENUMERATION, ModelEnumerationConfig.builder().build());
        configMap.put(ConfigurationType.FORMULA_RANDOMIZER, FormulaRandomizerConfig.builder().build());
        return configMap;
    }

    /**
     * Removes all formulas from the factory cache.
     */
    protected void initCaches() {
        if (readOnly) {
            throwReadOnlyException();
        }
        auxVarCounters = new ConcurrentHashMap<>();
        for (final InternalAuxVarType auxType : InternalAuxVarType.values()) {
            auxVarCounters.put(auxType.getPrefix(), new AtomicInteger(0));
        }
    }

    /**
     * Sets the formula factory in a read-only mode. In this mode, no new
     * formulas or variables can be generated. Trying to do so will throw an
     * {@link FactoryReadOnlyException}. A formula factory read-only mode is
     * thread-save - even if it is a caching formula factory.
     */
    public void readOnlyMode() {
        readOnly = true;
    }

    /**
     * Activates the write mode on this formula factory. A caching formula
     * factory in write mode is NOT thread-safe.
     */
    public void writeMode() {
        readOnly = false;
    }

    /**
     * Returns whether this factory is in read-only mode.
     * @return whether this factory is in read-only mode
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns the name of this formula factory.
     * @return the name of this formula factory
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the configuration for a given configuration type or {@code null}
     * if there isn't any.
     * @param cType the configuration type
     * @return the configuration for a given configuration type
     */
    public Configuration configurationFor(final ConfigurationType cType) {
        return configurations.get(cType);
    }

    /**
     * Puts a new configuration into the configuration database. If there is
     * already a configuration present for this type, it will be overwritten.
     * <p>
     * Note that is not allowed to pass configurations of type
     * {@link ConfigurationType#FORMULA_FACTORY}. Such configurations can only
     * be passed to the constructor and never be changed thereafter.
     * @param configuration the configuration
     * @throws IllegalArgumentException if a configuration of type
     *                                  {@link ConfigurationType#FORMULA_FACTORY}
     *                                  was passed
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public void putConfiguration(final Configuration configuration) {
        if (readOnly) {
            throwReadOnlyException();
        }
        if (configuration.getType() == ConfigurationType.FORMULA_FACTORY) {
            throw new IllegalArgumentException(
                    "Configurations for the formula factory itself can only be passed in the constructor.");
        }
        configurations.put(configuration.getType(), configuration);
    }

    /**
     * Returns a function to compute the sub-formulas.
     * @return a function to compute the sub-formulas
     */
    public SubNodeFunction getSubformulaFunction() {
        return subformulaFunction.get();
    }

    /**
     * Returns the constant "True" or "False" depending on the given value.
     * @param value the given value
     * @return the constant
     */
    public Constant constant(final boolean value) {
        return value ? cTrue : cFalse;
    }

    /**
     * Returns a (singleton) object for the constant "True".
     * @return an object for the constant "True"
     */
    public CTrue verum() {
        return cTrue;
    }

    /**
     * Returns a (singleton) object for the constant "False".
     * @return an object for the constant "False"
     */
    public CFalse falsum() {
        return cFalse;
    }

    /**
     * Creates a new binary operator with a given type and two operands.
     * @param type  the type of the formula
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @return the newly generated formula
     * @throws IllegalArgumentException      if a wrong formula type is passed
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula binaryOperator(final FType type, final Formula left, final Formula right) {
        if (readOnly) {
            throwReadOnlyException();
        }
        switch (type) {
            case IMPL:
                return implication(left, right);
            case EQUIV:
                return equivalence(left, right);
            default:
                throw new IllegalArgumentException("Cannot create a binary formula with operator: " + type);
        }
    }

    /**
     * Creates a new implication.
     * @param leftIn  the left-hand side operand
     * @param rightIn the right-hand side operand
     * @return a new implication
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula implication(final Formula leftIn, final Formula rightIn) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final Formula left = importOrPanic(leftIn);
        final Formula right = importOrPanic(rightIn);
        if (left.getType() == FType.FALSE || right.getType() == FType.TRUE) {
            return verum();
        }
        if (left.getType() == FType.TRUE) {
            return right;
        }
        if (right.getType() == FType.FALSE) {
            return not(left);
        }
        if (left.equals(right)) {
            return verum();
        }
        if (left.equals(negateOrNull(right))) {
            return right;
        }
        return internalImplication(left, right);
    }

    protected abstract Formula internalImplication(final Formula left, final Formula right);

    /**
     * Creates a new equivalence.
     * @param leftIn  the left-hand side operand
     * @param rightIn the right-hand side operand
     * @return a new equivalence
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula equivalence(final Formula leftIn, final Formula rightIn) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final Formula left = importOrPanic(leftIn);
        final Formula right = importOrPanic(rightIn);
        if (left.getType() == FType.TRUE) {
            return right;
        }
        if (right.getType() == FType.TRUE) {
            return left;
        }
        if (left.getType() == FType.FALSE) {
            return not(right);
        }
        if (right.getType() == FType.FALSE) {
            return not(left);
        }
        if (left.equals(right)) {
            return verum();
        }
        if (left.equals(negateOrNull(right))) {
            return falsum();
        }
        return internalEquivalence(left, right);
    }

    protected abstract Formula internalEquivalence(final Formula left, final Formula right);

    /**
     * Creates the negation of a given formula.
     * <p>
     * Constants, literals and negations are negated directly and returned. For
     * all other formulas a new {@code Not} object is returned.
     * @param formula the given formula
     * @return the negated formula
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula not(final Formula formula) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final Formula operand = importOrPanic(formula);
        if (operand.getType() == FType.LITERAL || operand.getType() == FType.FALSE || operand.getType() == FType.TRUE ||
                operand.getType() == FType.NOT) {
            return operand.negate(this);
        }
        return internalNot(operand);
    }

    protected abstract Formula internalNot(final Formula operand);

    /**
     * Creates a new n-ary operator with a given type and a list of operands.
     * @param type     the type of the formula
     * @param operands the list of operands
     * @return the newly generated formula
     * @throws IllegalArgumentException      if a wrong formula type is passed
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula naryOperator(final FType type, final Collection<? extends Formula> operands) {
        if (readOnly) {
            throwReadOnlyException();
        }
        return naryOperator(type, operands.toArray(new Formula[0]));
    }

    /**
     * Creates a new n-ary operator with a given type and a list of operands.
     * @param type     the type of the formula
     * @param operands the list of operands
     * @return the newly generated formula
     * @throws IllegalArgumentException      if a wrong formula type is passed
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula naryOperator(final FType type, final Formula... operands) {
        if (readOnly) {
            throwReadOnlyException();
        }
        switch (type) {
            case OR:
                return or(operands);
            case AND:
                return and(operands);
            default:
                throw new IllegalArgumentException("Cannot create an n-ary formula with operator: " + type);
        }
    }

    /**
     * Creates a new conjunction from an array of formulas.
     * @param operands the vector of formulas
     * @return a new conjunction
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula and(final Formula... operands) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>(operands.length);
        Collections.addAll(ops, operands);
        return internalAnd(ops);
    }

    /**
     * Creates a new conjunction from a collection of formulas.
     * <p>
     * Note: The LinkedHashSet is used to eliminate duplicate sub-formulas and
     * to respect the commutativity of operands.
     * @param operands the array of formulas
     * @return a new conjunction
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula and(final Collection<? extends Formula> operands) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>(operands);
        return internalAnd(ops);
    }

    protected abstract Formula internalAnd(LinkedHashSet<? extends Formula> operands);

    /**
     * Creates a new CNF from an array of clauses.
     * <p>
     * ATTENTION: it is assumed that the operands are really clauses - this is
     * not checked for performance reasons. Also, no reduction of operands is
     * performed - this method should only be used if you are sure that the CNF
     * is free of redundant clauses.
     * @param clauses the array of clauses
     * @return a new CNF
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula cnf(final Formula... clauses) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>(clauses.length);
        Collections.addAll(ops, clauses);
        return internalCnf(ops);
    }

    /**
     * Creates a new CNF from a collection of clauses.
     * <p>
     * ATTENTION: it is assumed that the operands are really clauses - this is
     * not checked for performance reasons. Also, no reduction of operands is
     * performed - this method should only be used if you are sure that the CNF
     * is free of redundant clauses.
     * @param clauses the collection of clauses
     * @return a new CNF
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula cnf(final Collection<? extends Formula> clauses) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<? extends Formula> ops = new LinkedHashSet<>(clauses);
        return internalCnf(ops);
    }

    protected abstract Formula internalCnf(final LinkedHashSet<? extends Formula> clauses);

    /**
     * Creates a new disjunction from an array of formulas.
     * @param operands the list of formulas
     * @return a new disjunction
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula or(final Formula... operands) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>(operands.length);
        Collections.addAll(ops, operands);
        return internalOr(ops);
    }

    /**
     * Creates a new disjunction from a collection of formulas.
     * <p>
     * Note: The LinkedHashSet is used to eliminate duplicate sub-formulas and
     * to respect the commutativity of operands.
     * @param operands the collection of formulas
     * @return a new disjunction
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula or(final Collection<? extends Formula> operands) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>(operands);
        return internalOr(ops);
    }

    protected abstract Formula internalOr(final LinkedHashSet<? extends Formula> operandsIn);

    /**
     * Creates a new clause from an array of literals.
     * <p>
     * ATTENTION: No reduction of operands is performed - this method should
     * only be used if you are sure that the clause is free of redundant or
     * contradicting literals.
     * @param literals the collection of literals
     * @return a new clause
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula clause(final Literal... literals) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Literal> ops = new LinkedHashSet<>(literals.length);
        Collections.addAll(ops, literals);
        return internalClause(ops);
    }

    /**
     * Creates a new clause from a collection of literals.
     * <p>
     * ATTENTION: No reduction of operands is performed - this method should
     * only be used if you are sure that the clause is free of contradicting
     * literals.
     * @param literals the collection of literals
     * @return a new clause
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula clause(final Collection<? extends Literal> literals) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final LinkedHashSet<Literal> ops = new LinkedHashSet<>(literals);
        return internalClause(ops);
    }

    protected abstract Formula internalClause(final LinkedHashSet<Literal> literalsIn);

    /**
     * Creates a new literal instance with a given name and positive phase.
     * @param name the variable name
     * @return a new literal with the given name and positive phase
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public abstract Variable variable(final String name);

    /**
     * Creates a new literal instance with a given name and phase.
     * <p>
     * Literal names should not start with {@code @AUX} - these are
     * reserved for internal literals.
     * @param name  the literal name
     * @param phase the literal phase
     * @return a new literal with the given name and phase
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public Literal literal(final String name, final boolean phase) {
        if (readOnly) {
            throwReadOnlyException();
        }
        if (phase) {
            return variable(name);
        } else {
            return internalNegativeLiteral(name);
        }
    }

    protected abstract Literal internalNegativeLiteral(final String name);

    /**
     * Creates a list of literals with the given names and positive phase.
     * @param names the variable names
     * @return a new list of literals with the given names and positive phase
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public SortedSet<Variable> variables(final Collection<String> names) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final SortedSet<Variable> variables = new TreeSet<>();
        for (final String name : names) {
            variables.add(variable(name));
        }
        return variables;
    }

    /**
     * Creates a list of literals with the given names and positive phase.
     * @param names the variable names
     * @return a new list of literals with the given names and positive phase
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public SortedSet<Variable> variables(final String... names) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final SortedSet<Variable> variables = new TreeSet<>();
        for (final String name : names) {
            variables.add(variable(name));
        }
        return variables;
    }

    /**
     * Creates a new pseudo-Boolean constraint.
     * @param comparator   the comparator of the constraint
     * @param rhs          the right-hand side of the constraint
     * @param literals     the literals of the constraint
     * @param coefficients the coefficients of the constraint
     * @return the pseudo-Boolean constraint
     * @throws IllegalArgumentException      if the number of literals and
     *                                       coefficients do not correspond
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula pbc(final CType comparator, final int rhs, final List<? extends Literal> literals,
                       final List<Integer> coefficients) {
        if (readOnly) {
            throwReadOnlyException();
        }
        return constructPbc(comparator, rhs, literals, coefficients);
    }

    /**
     * Creates a new pseudo-Boolean constraint.
     * @param comparator   the comparator of the constraint
     * @param rhs          the right-hand side of the constraint
     * @param literals     the literals of the constraint
     * @param coefficients the coefficients of the constraint
     * @return the pseudo-Boolean constraint
     * @throws IllegalArgumentException      if the number of literals and
     *                                       coefficients do not correspond
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula pbc(final CType comparator, final int rhs, final Literal[] literals, final int[] coefficients) {
        if (readOnly) {
            throwReadOnlyException();
        }
        return constructPbc(comparator, rhs, Arrays.asList(literals),
                Arrays.stream(coefficients).boxed().collect(Collectors.toList()));
    }

    private Formula constructPbc(final CType comparator, final int rhs, final List<? extends Literal> literalsIn,
                                 final List<Integer> coefficients) {
        final List<? extends Literal> literals = importOrPanic(literalsIn);
        if (literals.isEmpty()) {
            return constant(evaluateTrivialPBConstraint(comparator, rhs));
        }
        if (isCc(comparator, rhs, literals, coefficients)) {
            return constructCcUnsafe(comparator, rhs, literals);
        }
        return internalPbc(literals, coefficients, comparator, rhs);
    }

    protected abstract Formula internalPbc(final List<? extends Literal> literals, final List<Integer> coefficients,
                                           final CType comparator, int rhs);

    /**
     * Creates a new cardinality constraint.
     * @param variables  the variables of the constraint
     * @param comparator the comparator of the constraint
     * @param rhs        the right-hand side of the constraint
     * @return the cardinality constraint
     * @throws IllegalArgumentException      if there are negative variables
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula cc(final CType comparator, final int rhs, final Collection<Variable> variables) {
        return constructCc(comparator, rhs, variables);
    }

    /**
     * Creates a new cardinality constraint.
     * @param variables  the variables of the constraint
     * @param comparator the comparator of the constraint
     * @param rhs        the right-hand side of the constraint
     * @return the cardinality constraint
     * @throws IllegalArgumentException      if there are negative variables
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula cc(final CType comparator, final int rhs, final Variable... variables) {
        return constructCc(comparator, rhs, Arrays.asList(variables));
    }

    /**
     * Creates a new at-most-one cardinality constraint.
     * @param variables the variables of the constraint
     * @return the at-most-one constraint
     * @throws IllegalArgumentException      if there are negative variables
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     */
    public Formula amo(final Collection<Variable> variables) {
        return constructCcUnsafe(CType.LE, 1, variables);
    }

    /**
     * Creates a new at-most-one cardinality constraint.
     * @param variables the variables of the constraint
     * @return the at-most-one constraint
     * @throws IllegalArgumentException      if there are negative variables
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     */
    public Formula amo(final Variable... variables) {
        return constructCcUnsafe(CType.LE, 1, Arrays.asList(variables));
    }

    /**
     * Creates a new exactly-one cardinality constraint.
     * @param variables the variables of the constraint
     * @return the exactly-one constraint
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws IllegalArgumentException      if there are negative variables
     */
    public Formula exo(final Collection<Variable> variables) {
        return constructCcUnsafe(CType.EQ, 1, variables);
    }

    /**
     * Creates a new exactly-one cardinality constraint.
     * @param variables the variables of the constraint
     * @return the exactly-one constraint
     * @throws IllegalArgumentException      if there are negative variables
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    public Formula exo(final Variable... variables) {
        return constructCcUnsafe(CType.EQ, 1, Arrays.asList(variables));
    }

    private Formula constructCc(final CType comparator, final int rhs, final Collection<Variable> literals) {
        if (readOnly) {
            throwReadOnlyException();
        }
        if (!isCc(comparator, rhs, literals, null)) {
            throw new IllegalArgumentException("Given values do not represent a cardinality constraint.");
        }
        return constructCcUnsafe(comparator, rhs, literals);
    }

    private Formula constructCcUnsafe(final CType comparator, final int rhs,
                                      final Collection<? extends Literal> literalsIn) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final List<? extends Literal> literals = importOrPanic(literalsIn);
        if (literals.isEmpty()) {
            return constant(evaluateTrivialPBConstraint(comparator, rhs));
        }
        return internalCc(literals, comparator, rhs);
    }

    protected abstract Formula internalCc(List<? extends Literal> literals, CType comparator, int rhs);

    /**
     * Tests if the given pseudo-Boolean parameters (comparator, right-hand
     * side, literals and coefficients) represent a cardinality constraint. See
     * {@link CardinalityConstraint} for the definition of a cardinality
     * constraint.
     * @param comparator   the comparator
     * @param rhs          the right-hand side
     * @param literals     the literals
     * @param coefficients the coefficients or {@code null} if there are no
     *                     coefficients to test
     * @return {@code true} if the given pseudo-Boolean constraint is a
     * cardinality constraint, otherwise {@code false}
     */
    private static boolean isCc(final CType comparator, final int rhs, final Collection<? extends Literal> literals,
                                final List<Integer> coefficients) {
        for (final Literal lit : literals) {
            if (!lit.getPhase()) {
                return false;
            }
        }
        if (coefficients != null) {
            for (final int c : coefficients) {
                if (c != 1) {
                    return false;
                }
            }
        }
        return comparator == CType.LE && rhs >= 0 ||
                comparator == CType.LT && rhs >= 1 ||
                comparator == CType.GE && rhs >= 0 ||
                comparator == CType.GT && rhs >= -1 ||
                comparator == CType.EQ && rhs >= 0;
    }

    /**
     * Evaluates the trivial case for a pseudo-Boolean constraint where no
     * literals are given.
     * @param comparator the comparator
     * @param rhs        the right-hand side
     * @return {@code true} if the trivial case is a tautology, {@code false} if
     * the trivial case is a contradiction
     */
    private static boolean evaluateTrivialPBConstraint(final CType comparator, final int rhs) {
        switch (comparator) {
            case EQ:
                return rhs == 0;
            case LE:
                return rhs >= 0;
            case LT:
                return rhs > 0;
            case GE:
                return rhs <= 0;
            case GT:
                return rhs < 0;
            default:
                throw new IllegalArgumentException("Unknown comparator: " + comparator);
        }
    }

    /**
     * Returns a new auxiliary literal of the given type.
     * <p>
     * Remark: currently only the counter is increased - there is no check if
     * the literal is already present.
     * @param type the category of the variable
     * @return the new auxiliary literal
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public Variable newAuxVariable(final String type) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final AtomicInteger auxVarCounter = auxVarCounters.computeIfAbsent(type, t -> {
            if (t.contains("_") || t.contains("@")) {
                throw new IllegalArgumentException("Auxiliary variable types must not contain '_' or '@' characters");
            }
            for (final String existingType : auxVarCounters.keySet()) {
                if ((t.length() <= existingType.length() && existingType.startsWith(t))
                        || (t.length() > existingType.length() && t.startsWith(existingType))
                ) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Can not add new auxiliary variable type \"%s\" collides with existing type \"%s\"",
                                    type, existingType));
                }
            }
            return new AtomicInteger(0);
        });
        final String name = auxVarPrefix + type + '_' + auxVarCounter.getAndIncrement();
        return variable(name);
    }

    public Variable newAuxVariable(final InternalAuxVarType type) {
        return newAuxVariable(type.getPrefix());
    }

    /**
     * Returns a new cardinality constraint auxiliary literal.
     * <p>
     * Remark: currently only the counter is increased - there is no check if
     * the literal is already present.
     * @return the new cardinality constraint auxiliary literal
     */
    public Variable newCcVariable() {
        return newAuxVariable(InternalAuxVarType.CC);
    }

    /**
     * Returns a new pseudo Boolean auxiliary literal.
     * <p>
     * Remark: currently only the counter is increased - there is no check if
     * the literal is already present.
     * @return the new pseudo Boolean auxiliary literal
     */
    public Variable newPbVariable() {
        return newAuxVariable(InternalAuxVarType.PBC);
    }

    /**
     * Returns a new CNF auxiliary literal.
     * <p>
     * Remark: currently only the counter is increased - there is no check if
     * the literal is already present.
     * @return the new CNF auxiliary literal
     */
    public Variable newCnfVariable() {
        return newAuxVariable(InternalAuxVarType.CNF);
    }

    /**
     * Returns a condensed array of operands for a given n-ary disjunction.
     * @param operands the formulas
     * @return a condensed array of operands
     */
    protected CondensedOperands condenseOperandsOr(final Collection<? extends Formula> operands) {
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>();
        boolean cnfCheck = true;
        for (final Formula form : operands) {
            if (form.getType() == FType.OR) {
                for (final Formula op : ((NAryOperator) form).getOperands()) {
                    final byte ret = addFormulaOr(ops, op);
                    if (ret == 0x00) {
                        return new CondensedOperands(null, cnfCheck);
                    }
                    if (ret == 0x02) {
                        cnfCheck = false;
                    }
                }
            } else {
                final byte ret = addFormulaOr(ops, form);
                if (ret == 0x00) {
                    return new CondensedOperands(null, cnfCheck);
                }
                if (ret == 0x02) {
                    cnfCheck = false;
                }
            }
        }
        return new CondensedOperands(ops, cnfCheck);
    }

    /**
     * Returns a condensed array of operands for a given n-ary conjunction.
     * @param operands the formulas
     * @return a condensed array of operands
     */
    protected CondensedOperands condenseOperandsAnd(final Collection<? extends Formula> operands) {
        final LinkedHashSet<Formula> ops = new LinkedHashSet<>();
        boolean cnfCheck = true;
        for (final Formula form : operands) {
            if (form.getType() == FType.AND) {
                for (final Formula op : ((NAryOperator) form).getOperands()) {
                    final byte ret = addFormulaAnd(ops, op);
                    if (ret == 0x00) {
                        return new CondensedOperands(null, cnfCheck);
                    }
                    if (ret == 0x02) {
                        cnfCheck = false;
                    }
                }
            } else {
                final byte ret = addFormulaAnd(ops, form);
                if (ret == 0x00) {
                    return new CondensedOperands(null, cnfCheck);
                }
                if (ret == 0x02) {
                    cnfCheck = false;
                }
            }
        }
        return new CondensedOperands(ops, cnfCheck);
    }

    /**
     * Returns the number of internal nodes of a given formula.
     * @param formula the formula
     * @return the number of internal nodes
     */
    public long numberOfNodes(final Formula formula) {
        return formula.apply(subformulaFunction.get()).size();
    }

    /**
     * Adds a given formula to a list of operands. If the formula is the neutral
     * element for the respective n-ary operation it will be skipped. If a
     * complementary formula is already present in the list of operands or the
     * formula is the dual element, 0x00 is returned. If the added formula was a
     * literal 0x01 is returned, otherwise 0x02 is returned.
     * @param ops     the list of operands
     * @param formula the formula
     */
    private byte addFormulaOr(final LinkedHashSet<Formula> ops, final Formula formula) {
        if (formula.getType() == FType.FALSE) {
            return 0x01;
        } else if (formula.getType() == FType.TRUE || containsComplement(ops, formula)) {
            return 0x00;
        } else {
            ops.add(formula);
            return (byte) (formula.getType() == FType.LITERAL ? 0x01 : 0x02);
        }
    }

    /**
     * Adds a given formula to a list of operands. If the formula is the neutral
     * element for the respective n-ary operation it will be skipped. If a
     * complementary formula is already present in the list of operands or the
     * formula is the dual element, 0x00 is returned. If the added formula was a
     * clause, 0x01 is returned, otherwise 0x02 is returned.
     * @param ops     the list of operands
     * @param formula the formula
     */
    private byte addFormulaAnd(final LinkedHashSet<Formula> ops, final Formula formula) {
        if (formula.getType() == FType.TRUE) {
            return 0x01;
        } else if (formula.getType() == FType.FALSE || containsComplement(ops, formula)) {
            return 0x00;
        } else {
            ops.add(formula);
            return (byte) (formula.getType() == FType.LITERAL ||
                    formula.getType() == FType.OR && ((Or) formula).isCnfClause() ? 0x01 : 0x02);
        }
    }

    /**
     * Returns {@code true} if a given list of formulas contains the negation of
     * a given formula, {@code false} otherwise. Always returns {@code false} if
     * the formula factory is configured to allow contradictions and
     * tautologies.
     * @param formulas the list of formulas
     * @param formula  the formula
     * @return {@code true} if a given list of formulas contains a given
     * formula, {@code false} otherwise
     */
    private boolean containsComplement(final LinkedHashSet<Formula> formulas, final Formula formula) {
        if (!simplifyComplementaryOperands) {
            return false;
        }
        final Formula negatedFormula = negateOrNull(formula);
        return negatedFormula != null && formulas.contains(negatedFormula);
    }

    /**
     * Returns the negated formula if the negation exists in the cache,
     * otherwise {@code null} is returned.
     * @param formula the formula
     * @return the negated formula if the negation exists in the cache,
     * otherwise {@code null}
     */
    protected abstract Formula negateOrNull(final Formula formula);

    /**
     * Imports a formula from another formula factory into this factory and
     * returns it. If the current factory of the formula is already this formula
     * factory, the same instance will be returned.
     * @param formula the formula to import
     * @return the imported formula on this factory
     * @throws FactoryReadOnlyException if the factory is in read-only mode
     */
    public Formula importFormula(final Formula formula) {
        if (readOnly) {
            throwReadOnlyException();
        }
        return formula.transform(importer.get());
    }

    /**
     * Checks if the given formula was created by this formula factory. If this
     * is the case, the formula is returned. Otherwise, depending on the
     * {@link #formulaMergeStrategy} the formula is either imported or an
     * exception is thrown.
     * @param formula the formula to check
     * @return the (possibly imported) formula
     * @throws UnsupportedOperationException if the formula was created by
     *                                       another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    private Formula importOrPanic(final Formula formula) {
        if (formula.getFactory() == this) {
            return formula;
        }
        switch (formulaMergeStrategy) {
            case IMPORT:
                return importFormula(formula);
            case USE_BUT_NO_IMPORT:
                return formula;
            case PANIC:
                throw new UnsupportedOperationException("Found an operand with a different formula factory.");
            default:
                throw new IllegalStateException("Unknown formula merge strategy: " + formulaMergeStrategy);
        }
    }

    /**
     * Checks if the given formulas were created by this formula factory. If
     * this is the case, the same list is returned. Otherwise, depending on the
     * {@link #formulaMergeStrategy} the formulas are either imported or an
     * exception is thrown.
     * @param formulas the formulas to check
     * @return the (possibly imported) formulas
     * @throws UnsupportedOperationException if one of the formulas was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     * @throws FactoryReadOnlyException      if the factory is in read-only mode
     */
    protected LinkedHashSet<? extends Formula> importOrPanicLhs(final LinkedHashSet<? extends Formula> formulas) {
        if (formulaMergeStrategy == FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT) {
            return formulas;
        }
        boolean foundAnotherFormulaFactory = false;
        for (final Formula formula : formulas) {
            if (formula.getFactory() != this) {
                foundAnotherFormulaFactory = true;
                break;
            }
        }
        if (!foundAnotherFormulaFactory) {
            return formulas;
        }
        switch (formulaMergeStrategy) {
            case IMPORT:
                final LinkedHashSet<Formula> result = new LinkedHashSet<>();
                for (final Formula formula : formulas) {
                    result.add(formula.getFactory() != this ? importFormula(formula) : formula);
                }
                return result;
            case PANIC:
                throw new UnsupportedOperationException("Found an operand with a different formula factory.");
            default:
                throw new IllegalStateException("Unknown formula merge strategy: " + formulaMergeStrategy);
        }

    }

    /**
     * Checks if the given literals were created by this formula factory. If
     * this is the case, the same array is returned. Otherwise, depending on the
     * {@link #formulaMergeStrategy} the formulas are either imported or an
     * exception is thrown.
     * @param literals the literals to check
     * @return the (possibly imported) literals
     * @throws UnsupportedOperationException if one of the literals was created
     *                                       by another factory and the formula
     *                                       merge strategy is
     *                                       {@link FormulaFactoryConfig.FormulaMergeStrategy#PANIC}.
     */
    protected List<? extends Literal> importOrPanic(final Collection<? extends Literal> literals) {
        if (formulaMergeStrategy == FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT) {
            return new ArrayList<>(literals);
        }
        boolean foundAnotherFormulaFactory = false;
        for (final Literal lit : literals) {
            if (lit.getFactory() != this) {
                foundAnotherFormulaFactory = true;
                break;
            }
        }
        if (!foundAnotherFormulaFactory) {
            return new ArrayList<>(literals);
        }
        switch (formulaMergeStrategy) {
            case IMPORT:
                final List<Literal> result = new ArrayList<>(literals.size());
                for (final Literal lit : literals) {
                    result.add(lit.getFactory() != this ? literal(lit.getName(), lit.getPhase()) : lit);
                }
                return result;
            case PANIC:
                throw new UnsupportedOperationException("Found an operand with a different formula factory.");
            default:
                throw new IllegalStateException("Unknown formula merge strategy: " + formulaMergeStrategy);
        }
    }

    protected void throwReadOnlyException() {
        throw new FactoryReadOnlyException();
    }

    protected final static class CondensedOperands {
        final LinkedHashSet<Formula> operands;
        final boolean isCnf;

        public CondensedOperands(final LinkedHashSet<Formula> operands, final boolean isCnf) {
            this.operands = operands;
            this.isCnf = isCnf;
        }

        public LinkedHashSet<Formula> getOperands() {
            return operands;
        }

        public boolean isCnf() {
            return isCnf;
        }
    }
}
