// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.bdds;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.knowledgecompilation.bdds.datastructures.BDDNode;
import org.logicng.knowledgecompilation.bdds.functions.BDDCNFFunction;
import org.logicng.knowledgecompilation.bdds.functions.BDDDNFFunction;
import org.logicng.knowledgecompilation.bdds.functions.BDDFunction;
import org.logicng.knowledgecompilation.bdds.functions.BDDModelEnumerationFunction;
import org.logicng.knowledgecompilation.bdds.functions.LngBDDFunction;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDConstruction;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDOperations;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDReordering;

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
public class BDD {

    private final int index;
    protected final BDDKernel kernel;
    protected final BDDConstruction construction;
    protected final BDDOperations operations;

    /**
     * Constructs a new BDD with a given index.
     * @param index  the index
     * @param kernel the kernel of this BDD
     */
    public BDD(final int index, final BDDKernel kernel) {
        this.index = index;
        this.kernel = kernel;
        construction = new BDDConstruction(kernel);
        operations = new BDDOperations(kernel);
    }

    /**
     * Returns the index of this BDD.
     * <p>
     * The index marks the entry point of this BDD in the {@link #underlyingKernel() underlying kernel}.
     * @return the index of this BDD
     */
    public int index() {
        return index;
    }

    /**
     * Returns the BDD Kernel of this factory.  The Kernel should only be accessed when you know, what you are doing.
     * @return the BDD Kernel
     */
    public BDDKernel underlyingKernel() {
        return kernel;
    }

    /**
     * Applies a given function on this BDD and returns the result.
     * @param function the function
     * @param <T>      the result type of the function
     * @return the result of the function application
     */
    public <T> T apply(final BDDFunction<T> function) {
        return function.apply(this);
    }

    /**
     * Returns a formula representation of this BDD.  This is done by using the Shannon expansion.
     * @return the formula for this BDD
     */
    public Formula toFormula() {
        return toFormula(kernel.factory());
    }

    /**
     * Returns a formula representation of this BDD.  This is done by using the Shannon expansion.
     * @param f the formula factory to generate new formulas
     * @return the formula for this BDD
     */
    public Formula toFormula(final FormulaFactory f) {
        return operations.toFormula(f, index, true);
    }

    /**
     * Returns a formula representation of this BDD.  This is done by using the Shannon expansion.
     * If {@code followPathsToTrue} is activated, the paths leading to the {@code true} terminal are followed to generate the formula.
     * If {@code followPathsToTrue} is deactivated, the paths leading to the {@code false} terminal are followed to generate the formula and the resulting formula is negated.
     * Depending on the formula and the number of satisfying assignments, the generated formula can be more compact using the {@code true} paths
     * or {@code false} paths, respectively.
     * @param followPathsToTrue the extraction style
     * @return the formula for this BDD
     */
    public Formula toFormula(final boolean followPathsToTrue) {
        return toFormula(kernel.factory(), followPathsToTrue);
    }

    /**
     * Returns a formula representation of this BDD.  This is done by using the Shannon expansion.
     * If {@code followPathsToTrue} is activated, the paths leading to the {@code true} terminal are followed to generate the formula.
     * If {@code followPathsToTrue} is deactivated, the paths leading to the {@code false} terminal are followed to generate the formula and the resulting formula is negated.
     * Depending on the formula and the number of satisfying assignments, the generated formula can be more compact using the {@code true} paths
     * or {@code false} paths, respectively.
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
    public BDD negate() {
        return new BDD(kernel.addRef(construction.not(index), null), kernel);
    }

    /**
     * Returns a new BDD which is the implication of this BDD to the given other BDD.  Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the implication from this BDD to the other BDD
     * @throws IllegalArgumentException if the two BDDs don't have the same kernel
     */
    public BDD implies(final BDD other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new BDD(kernel.addRef(construction.implication(index, other.index), null), kernel);
    }

    /**
     * Returns a new BDD which is the implication of the other given BDD to this BDD.  Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the implication from the other BDD to this BDD
     * @throws IllegalArgumentException if the two BDDs don't have the same kernel
     */
    public BDD impliedBy(final BDD other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new BDD(kernel.addRef(construction.implication(other.index, index), null), kernel);
    }

    /**
     * Returns a new BDD which is the equivalence of this BDD and the other given BDD.  Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the equivalence of this and the other BDD
     * @throws IllegalArgumentException if the two BDDs don't have the same kernel
     */
    public BDD equivalence(final BDD other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new BDD(kernel.addRef(construction.equivalence(index, other.index), null), kernel);
    }

    /**
     * Returns a new BDD which is the conjunction of this BDD and the given other BDD.  Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the conjunction of the two BDDs
     * @throws IllegalArgumentException if the two BDDs don't have the same kernel
     */
    public BDD and(final BDD other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new BDD(kernel.addRef(construction.and(index, other.index), null), kernel);
    }

    /**
     * Returns a new BDD which is the disjunction of this BDD and the given other BDD.  Both BDDs must use the same kernel.
     * @param other the other BDD
     * @return the disjunction of the two BDDs
     * @throws IllegalArgumentException if the two BDDs don't have the same kernel
     */
    public BDD or(final BDD other) {
        if (other.kernel != kernel) {
            throw new IllegalArgumentException("Only BDDs with the same kernel can be processed");
        }
        return new BDD(kernel.addRef(construction.or(index, other.index), null), kernel);
    }

    /**
     * Returns {@code true} if this BDD is a tautology, {@code false} otherwise.
     * @return {@code true} if this BDD is a tautology, {@code false} otherwise
     */
    public boolean isTautology() {
        return index == BDDKernel.BDD_TRUE;
    }

    /**
     * Returns {@code true} if this BDD is a contradiction, {@code false} otherwise.
     * @return {@code true} if this BDD is a contradiction, {@code false} otherwise
     */
    public boolean isContradiction() {
        return index == BDDKernel.BDD_FALSE;
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
    public List<Assignment> enumerateAllModels() {
        return enumerateAllModels(kernel.factory(), (Collection<Variable>) null);
    }

    /**
     * Enumerates all models of this BDD.
     * @param f the formula factory to generate new formulas
     * @return the list of all models
     */
    public List<Assignment> enumerateAllModels(final FormulaFactory f) {
        return enumerateAllModels(f, (Collection<Variable>) null);
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param variables the variables
     * @return the list of all models
     */
    public List<Assignment> enumerateAllModels(final Variable... variables) {
        return enumerateAllModels(kernel.factory(), Arrays.asList(variables));
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables
     * @return the list of all models
     */
    public List<Assignment> enumerateAllModels(final FormulaFactory f, final Variable... variables) {
        return enumerateAllModels(f, Arrays.asList(variables));
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param variables the variables
     * @return the list of all models
     */
    public List<Assignment> enumerateAllModels(final Collection<Variable> variables) {
        return apply(new BDDModelEnumerationFunction(kernel.factory(), variables));
    }

    /**
     * Enumerates all models of this BDD wrt. a given set of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables
     * @return the list of all models
     */
    public List<Assignment> enumerateAllModels(final FormulaFactory f, final Collection<Variable> variables) {
        return apply(new BDDModelEnumerationFunction(f, variables));
    }

    /**
     * Returns a CNF formula for this BDD.
     * @return the CNF for the formula represented by this BDD
     */
    public Formula cnf() {
        return cnf(kernel.factory());
    }

    /**
     * Returns a CNF formula for this BDD.
     * @param f the formula factory to generate new formulas
     * @return the CNF for the formula represented by this BDD
     */
    public Formula cnf(final FormulaFactory f) {
        return apply(new BDDCNFFunction(f));
    }

    /**
     * Returns the number of clauses for the CNF formula of the BDD.
     * @return the number of clauses for the CNF formula of the BDD
     */
    public BigInteger numberOfClausesCNF() {
        return operations.pathCountZero(index);
    }

    /**
     * Returns a DNF formula for this BDD.
     * @return the DNF for the formula represented by this BDD
     */
    public Formula dnf() {
        return dnf(kernel.factory());
    }

    /**
     * Returns a DNF formula for this BDD.
     * @param f the formula factory to generate new formulas
     * @return the DNF for the formula represented by this BDD
     */
    public Formula dnf(final FormulaFactory f) {
        return apply(new BDDDNFFunction(f));
    }

    /**
     * Restricts the BDD.
     * @param restriction the restriction
     * @return the restricted BDD
     */
    public BDD restrict(final Collection<Literal> restriction) {
        final BDD resBDD = BDDFactory.build(restriction, kernel);
        return new BDD(construction.restrict(index, resBDD.index), kernel);
    }

    /**
     * Restricts the BDD.
     * @param restriction the restriction
     * @return the restricted BDD
     */
    public BDD restrict(final Literal... restriction) {
        return restrict(Arrays.asList(restriction));
    }

    /**
     * Existential quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public BDD exists(final Collection<Variable> variables) {
        final BDD resBDD = BDDFactory.build(variables, kernel);
        return new BDD(construction.exists(index, resBDD.index), kernel);
    }

    /**
     * Existential quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public BDD exists(final Variable... variables) {
        return exists(Arrays.asList(variables));
    }

    /**
     * Universal quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public BDD forall(final Collection<Variable> variables) {
        final BDD resBDD = BDDFactory.build(variables, kernel);
        return new BDD(construction.forAll(index, resBDD.index), kernel);
    }

    /**
     * Universal quantifier elimination for a given set of variables.
     * @param variables the variables to eliminate
     * @return the BDD with the eliminated variables
     */
    public BDD forall(final Variable... variables) {
        return forall(Arrays.asList(variables));
    }

    /**
     * Returns an arbitrary model of this BDD or {@code null} if there is none.
     * @return an arbitrary model of this BDD
     */
    public Assignment model() {
        return createAssignment(kernel.factory(), operations.satOne(index));
    }

    /**
     * Returns an arbitrary model of this BDD or {@code null} if there is none.
     * @param f the formula factory to generate new formulas
     * @return an arbitrary model of this BDD
     */
    public Assignment model(final FormulaFactory f) {
        return createAssignment(f, operations.satOne(index));
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given variables or {@code null} if there is
     * none.  If a variable is a don't care variable, it will be assigned with the given default value.
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the model
     * @return an arbitrary model of this BDD
     */
    public Assignment model(final boolean defaultValue, final Collection<Variable> variables) {
        return model(kernel.factory(), defaultValue, variables);
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given variables or {@code null} if there is
     * none.  If a variable is a don't care variable, it will be assigned with the given default value.
     * @param f            the formula factory to generate new formulas
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the model
     * @return an arbitrary model of this BDD
     */
    public Assignment model(final FormulaFactory f, final boolean defaultValue, final Collection<Variable> variables) {
        final int varBDD = BDDFactory.build(variables, kernel).index;
        final int pol = defaultValue ? BDDKernel.BDD_TRUE : BDDKernel.BDD_FALSE;
        final int modelBDD = operations.satOneSet(index, varBDD, pol);
        return createAssignment(f, modelBDD);
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given variables or {@code null} if there is
     * none.  If a variable is a don't care variable, it will be assigned with the given default value.
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the model
     * @return an arbitrary model of this BDD
     */
    public Assignment model(final boolean defaultValue, final Variable... variables) {
        return model(kernel.factory(), defaultValue, Arrays.asList(variables));
    }

    /**
     * Returns an arbitrary model of this BDD which contains at least the given variables or {@code null} if there is
     * none.  If a variable is a don't care variable, it will be assigned with the given default value.
     * @param f            the formula factory to generate new formulas
     * @param defaultValue the default value for don't care variables
     * @param variables    the set of variable which has to be contained in the model
     * @return an arbitrary model of this BDD
     */
    public Assignment model(final FormulaFactory f, final boolean defaultValue, final Variable... variables) {
        return model(f, defaultValue, Arrays.asList(variables));
    }

    /**
     * Returns a full model of this BDD or {@code null} if there is none.
     * @return a full model of this BDD
     */
    public Assignment fullModel() {
        return fullModel(kernel.factory());
    }

    /**
     * Returns a full model of this BDD or {@code null} if there is none.
     * @param f the formula factory to generate new formulas
     * @return a full model of this BDD
     */
    public Assignment fullModel(final FormulaFactory f) {
        return createAssignment(f, operations.fullSatOne(index));
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
        final int supportBDD = operations.support(index);
        final Assignment assignment = createAssignment(kernel.factory(), supportBDD); // only variables, cannot create new literals
        assert assignment == null || assignment.negativeLiterals().isEmpty();
        return assignment == null ? Collections.emptySortedSet() : new TreeSet<>(assignment.positiveVariables());
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
     * Swaps two variables in a BDD.
     * Beware that if the {@link #kernel BDDKernel} of this BDD was used for multiple
     * BDDs, the variables are swapped in <b>all</b> of these BDDs.
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
    public BDDReordering getReordering() {
        return kernel.getReordering();
    }

    /**
     * Returns a LogicNG internal BDD data structure of this BDD.
     * @return the BDD as LogicNG data structure
     */
    public BDDNode toLngBdd() {
        return apply(new LngBDDFunction(kernel.factory()));
    }

    /**
     * Creates an assignment from a BDD.
     * @param f        the formula factory to generate new formulas
     * @param modelBDD the BDD
     * @return the assignment
     * @throws IllegalStateException if the BDD does not represent a unique model
     */
    private Assignment createAssignment(final FormulaFactory f, final int modelBDD) {
        if (modelBDD == BDDKernel.BDD_FALSE) {
            return null;
        }
        if (modelBDD == BDDKernel.BDD_TRUE) {
            return new Assignment();
        }
        final List<int[]> nodes = operations.allNodes(modelBDD);
        final Assignment assignment = new Assignment();
        for (final int[] node : nodes) {
            final Variable variable = kernel.getVariableForIndex(node[1]);
            if (node[2] == BDDKernel.BDD_FALSE) {
                assignment.addLiteral(variable);
            } else if (node[3] == BDDKernel.BDD_FALSE) {
                assignment.addLiteral(variable.negate(f));
            } else {
                throw new IllegalStateException("Expected that the model BDD has one unique path through the BDD.");
            }
        }
        return assignment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, kernel);
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof BDD
                && index == ((BDD) other).index
                && Objects.equals(kernel, ((BDD) other).kernel);
    }

    @Override
    public String toString() {
        return "BDD{" + index + "}";
    }
}
