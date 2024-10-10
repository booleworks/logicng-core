package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Set;

/**
 * Interface for encoding contexts.
 * <p>
 * An encoding context is used to store information produced by the encoding process. This mainly includes auxiliary
 * variables and performed substitutions. This is needed for the decoding algorithms to know how to interpret the
 * results and is also useful for performing multiple independent encoding calls that result in one big coherent
 * encoding.
 */
public interface CspEncodingContext {
    /**
     * Prefix for LogicNG auxiliary variables.
     */
    String CSP_AUX_LNG_VARIABLE = "CSP";

    /**
     * Returns the encoding algorithm of the context
     * @return the encoding algorithm of the context
     */
    CspEncodingAlgorithm getAlgorithm();

    /**
     * Resolves a list of integer variables to the boolean variables that are assigned to the integer variables.
     * @param variables list of integer variables
     * @return related boolean variables
     */
    Set<Variable> getSatVariables(final Collection<IntegerVariable> variables);

    /**
     * Returns whether an integer variable has an encoding in this context.
     * @param v the integer variable
     * @return {@code true} if the variable has an encoding in this context, {@code false} otherwise.
     */
    boolean isEncoded(final IntegerVariable v);

    /**
     * Returns a new context for order encoding.
     * @return a new context for order encoding
     */
    static OrderEncodingContext order() {
        return new OrderEncodingContext();
    }

    /**
     * Returns a new context for compact order encoding with the given base.
     * @param base the base of the encoding
     * @return a new context for compact order encoding
     */
    static CompactOrderEncodingContext compactOrder(final int base) {
        return new CompactOrderEncodingContext(base);
    }
}
