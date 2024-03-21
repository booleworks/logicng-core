package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.formulas.Formula;

import java.util.Collections;
import java.util.List;

public abstract class Term {

    protected final CspFactory cspFactory;
    protected final Type type;
    protected Decomposition decompositionResult;

    protected Term(final CspFactory cspFactory, final Type type) {
        this.cspFactory = cspFactory;
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    public abstract boolean isAtom();

    protected abstract Decomposition calculateDecomposition();

    public final Decomposition decompose() {
        if (this.decompositionResult == null) {
            this.decompositionResult = calculateDecomposition();
        }
        return this.decompositionResult;
    }

    public enum Type {
        ZERO, ONE, CONST, VAR, NEG, ADD, SUB, MUL
    }

    public static final class Decomposition {
        private final LinearExpression linearExpression;
        private final List<Formula> additionalConstraints;

        public Decomposition(final LinearExpression linearExpression, final List<Formula> additionalConstrains) {
            this.linearExpression = linearExpression;
            this.additionalConstraints = additionalConstrains;
        }

        public Decomposition(final LinearExpression linearExpression) {
            this.linearExpression = linearExpression;
            this.additionalConstraints = Collections.emptyList();
        }

        public LinearExpression getLinearExpression() {
            return this.linearExpression;
        }

        public List<Formula> getAdditionalConstraints() {
            return this.additionalConstraints;
        }

        @Override
        public String toString() {
            return "IntegerTerm.Decomposition{" +
                    "linearExpression=" + this.linearExpression.toString() +
                    ", additionalConstraints=" + this.additionalConstraints.toString() +
                    '}';
        }
    }
}
