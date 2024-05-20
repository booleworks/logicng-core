package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.LinearExpression;

import java.util.Collections;
import java.util.Set;

public abstract class Term {

    protected final Type type;
    protected Decomposition decompositionResult;

    protected Term(final Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public abstract boolean isAtom();

    protected abstract Decomposition calculateDecomposition();

    public final Decomposition decompose() {
        if (decompositionResult == null) {
            decompositionResult = calculateDecomposition();
        }
        return decompositionResult;
    }

    public enum Type {
        ZERO, ONE, CONST, VAR, NEG, ADD, SUB, MUL
    }

    public static final class Decomposition {
        private final LinearExpression linearExpression;
        private final Set<IntegerClause> additionalConstraints;

        public Decomposition(final LinearExpression linearExpression, final Set<IntegerClause> additionalConstrains) {
            this.linearExpression = linearExpression;
            this.additionalConstraints = additionalConstrains;
        }

        public Decomposition(final LinearExpression linearExpression) {
            this.linearExpression = linearExpression;
            this.additionalConstraints = Collections.emptySet();
        }

        public LinearExpression getLinearExpression() {
            return linearExpression;
        }

        public Set<IntegerClause> getAdditionalConstraints() {
            return additionalConstraints;
        }

        @Override
        public String toString() {
            return "IntegerTerm.Decomposition{" +
                    "linearExpression=" + linearExpression.toString() +
                    ", additionalConstraints=" + additionalConstraints.toString() +
                    '}';
        }
    }
}
