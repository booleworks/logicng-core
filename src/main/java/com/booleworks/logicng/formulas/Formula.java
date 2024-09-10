// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;
import com.booleworks.logicng.functions.LiteralsFunction;
import com.booleworks.logicng.functions.NumberOfAtomsFunction;
import com.booleworks.logicng.functions.NumberOfNodesFunction;
import com.booleworks.logicng.functions.VariablesFunction;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.BDDFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.VariableOrderingProvider;
import com.booleworks.logicng.predicates.CNFPredicate;
import com.booleworks.logicng.predicates.DNFPredicate;
import com.booleworks.logicng.predicates.NNFPredicate;
import com.booleworks.logicng.predicates.satisfiability.ContradictionPredicate;
import com.booleworks.logicng.predicates.satisfiability.SATPredicate;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.transformations.NNFTransformation;
import com.booleworks.logicng.transformations.cnf.CNFConfig;
import com.booleworks.logicng.transformations.cnf.CNFEncoder;

import java.util.SortedSet;
import java.util.stream.Stream;

/**
 * Interface for formulas.
 * @version 3.0.0
 * @since 1.0
 */
public interface Formula extends Iterable<Formula> {

    /**
     * Returns the type of this formula.
     * @return the type of this formula
     */
    FType type();

    /**
     * Returns the factory of this formula.
     * @return the factory of this formula
     */
    FormulaFactory factory();

    /**
     * Returns the number of atomic formulas of this formula. An atomic formula
     * is a predicate (constants and literals) or a pseudo-Boolean constraint.
     * @param f the formula factory to use for caching
     * @return the number of atomic formulas of this formula.
     */
    default long numberOfAtoms(final FormulaFactory f) {
        return new NumberOfAtomsFunction(f).apply(this);
    }

    /**
     * Returns the number of nodes of this formula.
     * @param f the formula factory to use for caching
     * @return the number of nodes of this formula.
     */
    default long numberOfNodes(final FormulaFactory f) {
        return new NumberOfNodesFunction(f).apply(this);
    }

    /**
     * Returns the number of operands of this formula.
     * @return the number of operands of this formula
     */
    int numberOfOperands();

    /**
     * Returns the number of internal nodes of this formula.
     * @return the number of internal nodes of this formula.
     */
    default long numberOfInternalNodes() {
        return factory().numberOfNodes(this);
    }

    /**
     * Returns whether this formula is a constant formula ("True" or "False").
     * @return {@code true} if this formula is a constant formula, {@code false}
     *         otherwise
     */
    boolean isConstantFormula();

    /**
     * Returns whether this formula is an atomic formula (constant, literal,
     * pseudo Boolean constraint), or not.
     * @return {@code true} if this formula is an atomic formula, {@code false}
     *         otherwise
     */
    boolean isAtomicFormula();

    /**
     * Returns all variables occurring in this formula. Returns an unmodifiable
     * set, so do not try to change the variable set manually.
     * @param f the formula factory to use for caching
     * @return all variables occurring in this formula
     */
    default SortedSet<Variable> variables(final FormulaFactory f) {
        return new VariablesFunction(f).apply(this);
    }

    /**
     * Returns all literals occurring in this formula. Returns an unmodifiable
     * set, so do not try to change the literal set manually.
     * @param f the formula factory to use for caching
     * @return all literals occurring in this formula
     */
    default SortedSet<Literal> literals(final FormulaFactory f) {
        return new LiteralsFunction(f).apply(this);
    }

    /**
     * Returns {@code true} if a given variable name is found in this formula,
     * {@code false} otherwise.
     * @param variable the variable to search for
     * @return {@code true} if a given variable is found in this formula
     */
    boolean containsVariable(final Variable variable);

    /**
     * Evaluates this formula with a given assignment. A literal not covered by
     * the assignment evaluates to {@code false} if it is positive, otherwise it
     * evaluates to {@code true}.
     * @param assignment the given assignment
     * @return the result of the evaluation, {@code true} or {@code false}
     */
    boolean evaluate(final Assignment assignment);

    /**
     * Restricts this formula with a given assignment and a formula factory that
     * generates new formulas.
     * @param f          the formula factory to generate new formulas
     * @param assignment the given assignment
     * @return a new restricted formula
     */
    Formula restrict(final FormulaFactory f, final Assignment assignment);

    /**
     * Returns {@code true} if this formula contains a given node, {@code false}
     * otherwise.
     * <p>
     * In particular, a {@code Literal} node {@code ~a} does NOT contain the
     * node {@code a}.
     * @param formula the node
     * @return {@code true} if this formula contains a given node
     */
    boolean containsNode(final Formula formula);

    /**
     * Returns {@code true} if this formula is in NNF, otherwise {@code false}
     * @param f the formula factory to use for caching
     * @return {@code true} if this formula is in NNF, otherwise {@code false}
     * @see NNFPredicate the NNF predicate
     */
    default boolean isNNF(final FormulaFactory f) {
        return holds(new NNFPredicate(f));
    }

    /**
     * Returns {@code true} if this formula is in DNF, otherwise {@code false}
     * @param f the formula factory to use for caching
     * @return {@code true} if this formula is in DNF, otherwise {@code false}
     * @see DNFPredicate the DNF predicate
     */
    default boolean isDNF(final FormulaFactory f) {
        return holds(new DNFPredicate(f));
    }

    /**
     * Returns {@code true} if this formula is in CNF, otherwise {@code false}
     * @param f the formula factory to use for caching
     * @return {@code true} if this formula is in CNF, otherwise {@code false}
     * @see CNFPredicate the CNF predicate
     */
    default boolean isCNF(final FormulaFactory f) {
        return holds(new CNFPredicate(f));
    }

    /**
     * Performs a simultaneous substitution on this formula given a single
     * mapping from variable to formula.
     * @param f        the formula factory to generate new formulas
     * @param variable the variable
     * @param formula  the formula
     * @return a new substituted formula
     */
    default Formula substitute(final FormulaFactory f, final Variable variable, final Formula formula) {
        final Substitution subst = new Substitution();
        subst.addMapping(variable, formula);
        return substitute(f, subst);
    }

    /**
     * Performs a given substitution on this formula.
     * @param f            the formula factory to generate new formulas
     * @param substitution the substitution
     * @return a new substituted formula
     */
    Formula substitute(final FormulaFactory f, final Substitution substitution);

    /**
     * Returns a negated copy of this formula.
     * @param f the formula factory to generate new formulas
     * @return a negated copy of this formula
     */
    Formula negate(final FormulaFactory f);

    /**
     * Returns a copy of this formula which is in NNF.
     * @param f the formula factory to generate new formulas
     * @return a copy of this formula which is in NNF
     */
    default Formula nnf(final FormulaFactory f) {
        return transform(new NNFTransformation(f));
    }

    /**
     * Returns a copy of this formula which is in CNF. The algorithm which is
     * used for the default CNF transformation can be configured in the
     * {@link FormulaFactory}.
     * <p>
     * Be aware that the default algorithm for the CNF transformation may result
     * in a CNF containing additional auxiliary variables with the prefix of
     * {@link InternalAuxVarType#CNF}. Also, the result may not be a
     * semantically equivalent CNF but an equisatisfiable CNF.
     * <p>
     * If the introduction of auxiliary variables is unwanted, you can choose
     * one of the algorithms {@link CNFConfig.Algorithm#FACTORIZATION} and
     * {@link CNFConfig.Algorithm#BDD}. Both algorithms provide CNF conversions
     * without the introduction of auxiliary variables and the result is a
     * semantically equivalent CNF.
     * <p>
     * Since CNF is the input for the SAT or MaxSAT solvers, it has a special
     * treatment here. For other conversions, use the according formula
     * functions.
     * @param f the formula factory to generate new formulas
     * @return a copy of this formula which is in CNF
     */
    default Formula cnf(final FormulaFactory f) {
        return CNFEncoder.encode(f, this, null);
    }

    /**
     * Returns whether this formula is satisfiable. A new SAT solver is used to
     * check the satisfiability. This is a convenience method for the predicate
     * {@link SATPredicate}. If you want to have more influence on the solver
     * (e.g. which solver type or configuration) you must create and use a
     * {@link SATSolver} on your own.
     * @param f the formula factory to use for caching
     * @return {@code true} when this formula is satisfiable, {@code false}
     *         otherwise
     */
    default boolean isSatisfiable(final FormulaFactory f) {
        return holds(new SATPredicate(f));
    }

    /**
     * Returns whether this formula is a tautology, hence always true. A new SAT
     * solver is used to check the tautology. This is a convenience method for
     * the predicate {@link TautologyPredicate}. If you want to have more
     * influence on the solver (e.g. which solver type or configuration) you
     * must create and use a {@link SATSolver} on your own.
     * @param f the formula factory to use for caching
     * @return {@code true} when this formula is a tautology, {@code false}
     *         otherwise
     */
    default boolean isTautology(final FormulaFactory f) {
        return holds(new TautologyPredicate(f));
    }

    /**
     * Returns whether this formula is a contradiction, hence always false. A
     * new SAT solver is used to check the contradiction. This is a convenience
     * method for the predicate {@link ContradictionPredicate}. If you want to
     * have more influence on the solver (e.g. which solver type or
     * configuration) you must create and use a {@link SATSolver} on your own.
     * @param f the formula factory to use for caching
     * @return {@code true} when this formula is a contradiction, {@code false}
     *         otherwise
     */
    default boolean isContradiction(final FormulaFactory f) {
        return holds(new ContradictionPredicate(f));
    }

    /**
     * Returns whether this formula implies the given other formula, i.e. `this
     * =&gt; other` is a tautology. A new SAT solver is used to check this
     * tautology. This is a convenience method. If you want to have more
     * influence on the solver (e.g. which solver type or configuration) you
     * must create and use a {@link SATSolver} on your own.
     * @param f     the formula factory to use for caching
     * @param other the formula which should be checked if it is implied by this
     *              formula
     * @return {@code true} when this formula implies the given other formula,
     *         {@code false} otherwise
     */
    default boolean implies(final FormulaFactory f, final Formula other) {
        return f.implication(this, other).holds(new TautologyPredicate(f));
    }

    /**
     * Returns whether this formula is implied by the given other formula, i.e.
     * `other =&gt; this` is a tautology. A new SAT solver is used to check this
     * tautology. This is a convenience method. If you want to have more
     * influence on the solver (e.g. which solver type or configuration) you
     * must create and use a {@link SATSolver} on your own.
     * @param f     the formula factory to use for caching
     * @param other the formula which should be checked if it implies this
     *              formula
     * @return {@code true} when this formula is implied by the given other
     *         formula, {@code false} otherwise
     */
    default boolean isImpliedBy(final FormulaFactory f, final Formula other) {
        return f.implication(other, this).holds(new TautologyPredicate(f));
    }

    /**
     * Returns whether this formula is equivalent to the given other formula,
     * i.e. `other &lt;=&gt; this` is a tautology. A new SAT solver is used to
     * check this tautology. This is a convenience method. If you want to have
     * more influence on the solver (e.g. which solver type or configuration)
     * you must create and use a {@link SATSolver} on your own.
     * @param f     the formula factory to use for caching
     * @param other the formula which should be checked if it is equivalent with
     *              this formula
     * @return {@code true} when this formula is equivalent to the given other
     *         formula, {@code false} otherwise
     */
    default boolean isEquivalentTo(final FormulaFactory f, final Formula other) {
        return f.equivalence(this, other).holds(new TautologyPredicate(f));
    }

    /**
     * Generates a BDD from this formula with a given variable ordering. This is
     * done by generating a new BDD factory, generating the variable order for
     * this formula, and building a new BDD. If more sophisticated operations
     * should be performed on the BDD or more than one formula should be
     * constructed on the BDD, an own instance of {@link BDDFactory} should be
     * created and used.
     * @param f        the formula factory to generate new formulas
     * @param provider the variable ordering provider
     * @return the BDD for this formula with the given ordering
     */
    default BDD bdd(final FormulaFactory f, final VariableOrderingProvider provider) {
        final Formula formula = nnf(f);
        final int varNum = formula.variables(f).size();
        final BDDKernel kernel;
        if (provider == null) {
            kernel = new BDDKernel(f, varNum, varNum * 30, varNum * 20);
        } else {
            kernel = new BDDKernel(f, provider.getOrder(f, formula), varNum * 30, varNum * 20);
        }
        return BDDFactory.build(f, formula, kernel);
    }

    /**
     * Generates a BDD from this formula with no given variable ordering. This
     * is done by generating a new BDD factory and building a new BDD. If more
     * sophisticated operations should be performed on the BDD or more than one
     * formula should be constructed on the BDD, an own instance of *
     * {@link BDDFactory} should be created and used.
     * @param f the formula factory to generate new formulas
     * @return the BDD for this formula
     */
    default BDD bdd(final FormulaFactory f) {
        return bdd(f, null);
    }

    /**
     * Transforms this formula with a given formula transformation.
     * @param transformation the formula transformation
     * @return the transformed formula
     */
    default Formula transform(final FormulaTransformation transformation) {
        return transformation.apply(this);
    }

    /**
     * Transforms this formula with a given formula transformation.
     * @param transformation the formula transformation
     * @param handler        the computation handler
     * @return the result of the transformation which may have been canceled by
     *         the computation handler
     */
    default LNGResult<Formula> transform(final FormulaTransformation transformation, final ComputationHandler handler) {
        return transformation.apply(this, handler);
    }

    /**
     * Evaluates a given predicate on this formula, caches the result, and
     * returns {@code true} if the predicate holds, {@code false} otherwise.
     * @param predicate the predicate
     * @return {@code true} if the predicate holds, {@code false} otherwise
     */
    default boolean holds(final FormulaPredicate predicate) {
        return predicate.test(this);
    }

    /**
     * Applies a given function on this formula and returns the result.
     * @param function the function
     * @param <T>      the result type of the function
     * @return the result of the function application
     */
    default <T> T apply(final FormulaFunction<T> function) {
        return function.apply(this);
    }

    /**
     * Returns a stream of this formula's operands.
     * <p>
     * Most times streams have worse performance then iterating over the formula
     * per iterator. Since internally formulas store their operands, a costly
     * call to {@code Arrays.stream()} is necessary. So if performance matters -
     * avoid using streams.
     * @return the stream
     */
    Stream<Formula> stream();

    @Override
    String toString();
}
