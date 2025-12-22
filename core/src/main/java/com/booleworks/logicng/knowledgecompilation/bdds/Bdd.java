// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BddNode;
import com.booleworks.logicng.knowledgecompilation.bdds.functions.BddCnfFunction;
import com.booleworks.logicng.knowledgecompilation.bdds.functions.BddDnfFunction;
import com.booleworks.logicng.knowledgecompilation.bdds.functions.BddFunction;
import com.booleworks.logicng.knowledgecompilation.bdds.functions.BddModelEnumerationFunction;
import com.booleworks.logicng.knowledgecompilation.bdds.functions.LngBddFunction;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddConstruction;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddOperations;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddReordering;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The internal representation of a BDD.
 * @version 3.0.0
 * @since 1.4.0
 */
public class Bdd {

    private final int index;
    protected final BddKernel kernel;
    protected final BddConstruction construction;
    protected final BddOperations operations;

    /**
     * Constructs a new BDD with a given index.
     * @param index  the index
     * @param kernel the kernel of this BDD
     */
    public Bdd(final int index, final BddKernel kernel) {
        this.index = index;
        this.kernel = kernel;
        construction = new BddConstruction(kernel);
        operations = new BddOperations(kernel);
    }

    /**
     * Returns the index of this BDD.
     * <p>
     * The index marks the entry point of this BDD in the
     * {@link #getUnderlyingKernel() underlying kernel}.
     * @return the index of this BDD
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the BDD Kernel of this factory. The Kernel should only be
     * accessed when you know, what you are doing.
     * @return the BDD Kernel
     */
    public BddKernel getUnderlyingKernel() {
        return kernel;
    }

    /**
     * Applies a given function on this BDD and returns the result.
     * @param function the function
     * @param <T>      the result type of the function
     * @return the result of the function application
     */
    public <T> T apply(final BddFunction<T> function) {
        return function.apply(this);
    }

    /**
     * Returns a formula representation of this BDD. This is done by using the
     * Shannon expansion.
     * @return the formula for this BDD
     */
    public Formula toFormula() {
        return toFormula(kernel.getFactory());
    }

    /**
     * Returns a formula representation of this BDD. This is done by using the
     * Shannon expansion.
     * @param f the formula factory to generate new formulas
     * @return the formula for this BDD
     */
    public Formula toFormula(final FormulaFactory f) {
        return operations.toFormula(f, index, true);
    }

    /**
     * Returns a formula representation of this BDD. This is done by using the
     * Shannon expansion. If {@code followPathsToTrue} is activated, the paths
     * leading to the {@code true} terminal are followed to generate the
     * formula. If {@code followPathsToTrue} is deactivated, the paths leading
     * to the {@code false} terminal are followed to generate the formula and
     * the resulting formula is negated. Depending on the formula and the number
     * of satisfying models, the generated formula can be more compact
     * using the {@code true} paths or {@code false} paths, respectively.
     * @param followPathsToTrue the extraction style
     * @return the formula for this BDD
     */
    public Formula toFormula(final boolean followPathsToTrue) {
        return toFormula(kernel.getFactory(), followPathsToTrue);
    }

    /**
     * Returns a formula representation of this BDD. This is done by using the
     * Shannon expansion. If {@code followPathsToTrue} is activated, the paths
     * leading to the {@code true} terminal are followed to generate the
     * formula. If {@code followPathsToTrue} is deactivated, the paths leading
     * to the {@code false} terminal are followed to generate the formula and
     * the resulting formula is negated. Depending on the formula and the number
     * of satisfying models, the generated formula can be more compact
     * using the {@code true} paths or {@code false} paths, respectively.
     * @param f                 the formula factory to generate new formulas
     * @param followPathsToTrue the extraction style
     * @return the formula for this BDD
     */
    public Formula toFormula(final FormulaFactory f, final boolean followPathsToTrue) {
        return operations.toFormula(f, index, followPathsToTrue);
    }

    /**
     * Returns a new BDD which is the negation of this BDD.
     * @return the negation of this BDD
     */
    public Bdd negate() {
        return new Bdd(kernel.addRef(construction.not(index), NopHandler.get()), kernel);
    }

    /**
     * Returns a new BDD which is the implication of this BDD to the given other
     * BDD. Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the implication from this BDD to the other BDD
     * @throws IllegalArgumentException if the two BDDs don't have the same
     *                                  kernel
     */
    public Bdd implies(final Bdd other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new Bdd(kernel.addRef(construction.implication(index, other.index), NopHandler.get()), kernel);
    }

    /**
     * Returns a new BDD which is the implication of the other given BDD to this
     * BDD. Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the implication from the other BDD to this BDD
     * @throws IllegalArgumentException if the two BDDs don't have the same
     *                                  kernel
     */
    public Bdd impliedBy(final Bdd other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new Bdd(kernel.addRef(construction.implication(other.index, index), NopHandler.get()), kernel);
    }

    /**
     * Returns a new BDD which is the equivalence of this BDD and the other
     * given BDD. Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the equivalence of this and the other BDD
     * @throws IllegalArgumentException if the two BDDs don't have the same
     *                                  kernel
     */
    public Bdd equivalence(final Bdd other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new Bdd(kernel.addRef(construction.equivalence(index, other.index), NopHandler.get()), kernel);
    }

    /**
     * Returns a new BDD which is the conjunction of this BDD and the given
     * other BDD. Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the conjunction of the two BDDs
     * @throws IllegalArgumentException if the two BDDs don't have the same
     *                                  kernel
     */
    public Bdd and(final Bdd other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new Bdd(kernel.addRef(construction.and(index, other.index), NopHandler.get()), kernel);
    }

    /**
     * Returns a new BDD which is the disjunction of this BDD and the given
     * other BDD. Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the disjunction of the two BDDs
     * @throws IllegalArgumentException if the two BDDs don't have the same
     *                                  kernel
     */
    public Bdd or(final Bdd other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new Bdd(kernel.addRef(construction.or(index, other.index), NopHandler.get()), kernel);
    }

    /**
     * Returns {@code true} if this BDD is a tautology, {@code false} otherwise.
     * @return {@code true} if this BDD is a tautology, {@code false} otherwise
     */
    public boolean isTautology() {
        return index == BddKernel.BDD_TRUE;
    }

    /**
     * Returns {@code true} if this BDD is a contradiction, {@code false}
     * otherwise.
     * @return {@code true} if this BDD is a contradiction, {@code false}
     * otherwise
     */
    public boolean isContradiction() {
        return index == BddKernel.BDD_FALSE;
    }

    /**
     * Returns the model count of this BDD.
     * @return the model count
     */
    public BigInteger modelCount() {
        return operations.satCount(index);
    }

    /**
     * Enumerates all models of this BDD.
     * @return the list of all models
     */
    public List<Model> enumerateAllModels() {
        return enumerateAllModels(kernel.getFactory(), (Collection<Variable>) null);
    }

    /**
     * Enumerates all models of this BDD.
     * @param f the formula factory to generate new formulas
     * @return the list of all models
     */
    public List<Model> enumerateAllModels(final FormulaFactory f) {
        return enumerateAllModels(f, (Collection<Variable>) null);
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param variables the variables
     * @return the list of all models
     */
    public List<Model> enumerateAllModels(final Variable... variables) {
        return enumerateAllModels(kernel.getFactory(), Arrays.asList(variables));
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables
     * @return the list of all models
     */
    public List<Model> enumerateAllModels(final FormulaFactory f, final Variable... variables) {
        return enumerateAllModels(f, Arrays.asList(variables));
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param variables the variables
     * @return the list of all models
     */
    public List<Model> enumerateAllModels(final Collection<Variable> variables) {
        return apply(new BddModelEnumerationFunction(kernel.getFactory(), variables));
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables
     * @return the list of all models
     */
    public List<Model> enumerateAllModels(final FormulaFactory f, final Collection<Variable> variables) {
        return apply(new BddModelEnumerationFunction(f, variables));
    }

    /**
     * Returns a CNF formula for this BDD.
     * @return the CNF for the formula represented by this BDD
     */
    public Formula cnf() {
        return cnf(kernel.getFactory());
    }

    /**
     * Returns a CNF formula for this BDD.
     * @param f the formula factory to generate new formulas
     * @return the CNF for the formula represented by this BDD
     */
    public Formula cnf(final FormulaFactory f) {
        return apply(new BddCnfFunction(f));
    }

    /**
     * Returns the number of clauses for the CNF formula of the BDD.
     * @return the number of clauses for the CNF formula of the BDD
     */
    public BigInteger numberOfClausesCnf() {
        return operations.pathCountZero(index);
    }

    /**
     * Returns a DNF formula for this BDD.
     * @return the DNF for the formula represented by this BDD
     */
    public Formula dnf() {
        return dnf(kernel.getFactory());
    }

    /**
     * Returns a DNF formula for this BDD.
     * @param f the formula factory to generate new formulas
     * @return the DNF for the formula represented by this BDD
     */
    public Formula dnf(final FormulaFactory f) {
        return apply(new BddDnfFunction(f));
    }

    /**
     * Restricts the BDD.
     * @param restriction the restriction
     * @return the restricted BDD
     */
    public Bdd restrict(final Collection<Literal> restriction) {
        final Bdd resBdd = BddFactory.build(restriction, kernel);
        return new Bdd(construction.restrict(index, resBdd.index), kernel);
    }

    /**
     * Restricts the BDD.
     * @param restriction the restriction
     * @return the restricted BDD
     */
    public Bdd restrict(final Literal... restriction) {
        return restrict(Arrays.asList(restriction));
    }

    /**
     * Existential quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public Bdd exists(final Collection<Variable> variables) {
        final Bdd resBdd = BddFactory.build(variables, kernel);
        return new Bdd(construction.exists(index, resBdd.index), kernel);
    }

    /**
     * Existential quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public Bdd exists(final Variable... variables) {
        return exists(Arrays.asList(variables));
    }

    /**
     * Universal quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public Bdd forall(final Collection<Variable> variables) {
        final Bdd resBdd = BddFactory.build(variables, kernel);
        return new Bdd(construction.forAll(index, resBdd.index), kernel);
    }

    /**
     * Universal quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public Bdd forall(final Variable... variables) {
        return forall(Arrays.asList(variables));
    }

    /**
     * Returns an arbitrary model of this BDD or {@code null} if there is none.
     * @return an arbitrary model of this BDD
     */
    public Model model() {
        return createModel(kernel.getFactory(), operations.satOne(index));
    }

    /**
     * Returns an arbitrary model of this BDD or {@code null} if there is none.
     * @param f the formula factory to generate new formulas
     * @return an arbitrary model of this BDD
     */
    public Model model(final FormulaFactory f) {
        return createModel(f, operations.satOne(index));
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given
     * variables or {@code null} if there is none. If a variable is a don't care
     * variable, it will be assigned with the given default value.
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the
     *                     model
     * @return an arbitrary model of this BDD
     */
    public Model model(final boolean defaultValue, final Collection<Variable> variables) {
        return model(kernel.getFactory(), defaultValue, variables);
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given
     * variables or {@code null} if there is none. If a variable is a don't care
     * variable, it will be assigned with the given default value.
     * @param f            the formula factory to generate new formulas
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the
     *                     model
     * @return an arbitrary model of this BDD
     */
    public Model model(final FormulaFactory f, final boolean defaultValue, final Collection<Variable> variables) {
        final int varBdd = BddFactory.build(variables, kernel).index;
        final int pol = defaultValue ? BddKernel.BDD_TRUE : BddKernel.BDD_FALSE;
        final int modelBdd = operations.satOneSet(index, varBdd, pol);
        return createModel(f, modelBdd);
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given
     * variables or {@code null} if there is none. If a variable is a don't care
     * variable, it will be assigned with the given default value.
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the
     *                     model
     * @return an arbitrary model of this BDD
     */
    public Model model(final boolean defaultValue, final Variable... variables) {
        return model(kernel.getFactory(), defaultValue, Arrays.asList(variables));
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given
     * variables or {@code null} if there is none. If a variable is a don't care
     * variable, it will be assigned with the given default value.
     * @param f            the formula factory to generate new formulas
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the
     *                     model
     * @return an arbitrary model of this BDD
     */
    public Model model(final FormulaFactory f, final boolean defaultValue, final Variable... variables) {
        return model(f, defaultValue, Arrays.asList(variables));
    }

    /**
     * Returns a full model of this BDD or {@code null} if there is none.
     * @return a full model of this BDD
     */
    public Model fullModel() {
        return fullModel(kernel.getFactory());
    }

    /**
     * Returns a full model of this BDD or {@code null} if there is none.
     * @param f the formula factory to generate new formulas
     * @return a full model of this BDD
     */
    public Model fullModel(final FormulaFactory f) {
        return createModel(f, operations.fullSatOne(index));
    }

    /**
     * Returns the number of paths leading to the terminal 'one' node.
     * @return the number of paths leading to the terminal 'one' node
     */
    public BigInteger pathCountOne() {
        return operations.pathCountOne(index);
    }

    /**
     * Returns the number of paths leading to the terminal 'zero' node.
     * @return the number of paths leading to the terminal 'zero' node
     */
    public BigInteger pathCountZero() {
        return operations.pathCountZero(index);
    }

    /**
     * Returns all the variables this BDD depends on.
     * @return all the variables that this BDD depends on
     */
    public SortedSet<Variable> support() {
        final int supportBdd = operations.support(index);
        // only variables, cannot create new literals
        final Model model = createModel(kernel.getFactory(), supportBdd);
        assert model == null || model.negativeLiterals().isEmpty();
        return model == null ? Collections.emptySortedSet() : new TreeSet<>(model.positiveVariables());
    }

    /**
     * Returns the number of distinct nodes for this BDD.
     * @return the number of distinct nodes
     */
    public int nodeCount() {
        return operations.nodeCount(index);
    }

    /**
     * Returns how often each variable occurs in this BDD.
     * @return how often each variable occurs in the BDD
     */
    public SortedMap<Variable, Integer> variableProfile() {
        final int[] varProfile = operations.varProfile(index);
        final SortedMap<Variable, Integer> profile = new TreeMap<>();
        for (int i = 0; i < varProfile.length; i++) {
            profile.put(kernel.getVariableForIndex(i), varProfile[i]);
        }
        return profile;
    }

    /**
     * Returns the variable order of this BDD.
     * @return the variable order
     */
    public List<Variable> getVariableOrder() {
        final List<Variable> order = new ArrayList<>();
        for (final int i : kernel.getCurrentVarOrder()) {
            order.add(kernel.getVariableForIndex(i));
        }
        return order;
    }

    /**
     * Swaps two variables in a BDD. Beware that if the {@link #kernel
     * BDDKernel} of this BDD was used for multiple BDDs, the variables are
     * swapped in <b>all</b> of these BDDs.
     * @param first  the first variable to swap
     * @param second the second variable to swap
     */
    public void swapVariables(final Variable first, final Variable second) {
        final int firstVar = kernel.getIndexForVariable(first);
        final int secondVar = kernel.getIndexForVariable(second);
        if (firstVar < 0) {
            throw new IllegalArgumentException("Unknown variable: " + first);
        } else if (secondVar < 0) {
            throw new IllegalArgumentException("Unknown variable: " + second);
        }
        kernel.getReordering().swapVariables(firstVar, secondVar);
    }

    /**
     * Returns the reordering object for the BDD kernel.
     * @return the reordering object
     */
    public BddReordering getReordering() {
        return kernel.getReordering();
    }

    /**
     * Returns a LogicNG internal BDD data structure of this BDD.
     * @return the BDD as LogicNG data structure
     */
    public BddNode toLngBdd() {
        return apply(new LngBddFunction(kernel.getFactory()));
    }

    /**
     * Creates a model from a BDD.
     * @param f        the formula factory to generate new formulas
     * @param modelBdd the BDD
     * @return the model
     * @throws IllegalStateException if the BDD does not represent a unique
     *                               model
     */
    private Model createModel(final FormulaFactory f, final int modelBdd) {
        if (modelBdd == BddKernel.BDD_FALSE) {
            return null;
        }
        if (modelBdd == BddKernel.BDD_TRUE) {
            return new Model();
        }
        final List<int[]> nodes = operations.allNodes(modelBdd);
        final List<Literal> model = new ArrayList<>();
        for (final int[] node : nodes) {
            final Variable variable = kernel.getVariableForIndex(node[1]);
            if (node[2] == BddKernel.BDD_FALSE) {
                model.add(variable);
            } else if (node[3] == BddKernel.BDD_FALSE) {
                model.add(variable.negate(f));
            } else {
                throw new IllegalStateException("Expected that the model BDD has one unique path through the BDD.");
            }
        }
        return new Model(model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, kernel);
    }

    @Override
    public boolean equals(final Object other) {
        return this == other ||
                other instanceof Bdd && index == ((Bdd) other).index && Objects.equals(kernel, ((Bdd) other).kernel);
    }

    @Override
    public String toString() {
        return "BDD{" + index + "}";
    }
}
