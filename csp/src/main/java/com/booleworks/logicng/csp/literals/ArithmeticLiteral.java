package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Set;

/**
 * This class is an interface for all arithmetic literals. Arithmetic literals are datastructures storing a certain
 * type of arithmetic condition/relation.
 * <p>
 * Right now, they are created and used by the different encoding steps to gradually transform the input constraints
 * into smaller sub-problems. These sub-problems are stored in arithmetic literals.
 */
public interface ArithmeticLiteral {
    /**
     * Returns whether a literal is trivially valid. This function returning {@code false} does not mean the literal
     * is not valid.
     * @return {@code true} if this literal is detected to be valid. {@code false} if it is not valid or the function
     * fails to detect it
     */
    boolean isValid();

    /**
     * Returns whether a literal is trivially unsatisfiable. This function returning {@code false} does not mean the
     * literal is satisfiable.
     * @return {@code true} if this literal is detected to be unsatisfiable. {@code false} if it is satisfiable or the
     * function fails to detect it
     */
    boolean isUnsat();

    /**
     * Returns a set of all integer variables stored in this literal.
     * @return a set of all integer variables stored in this literal.
     */
    Set<IntegerVariable> getVariables();

    /**
     * Applies all the substitutions contained in {@code assignment} to the variables stored in this literal.
     * Variables with no substitution in {@code assignment} stay unchanged.
     * @param assignment the substitutions to apply to this literal
     * @return A new literal with the substituted variables or the same literal if no variables change
     */
    ArithmeticLiteral substitute(IntegerVariableSubstitution assignment);
}
