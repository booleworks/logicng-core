package org.logicng.formulas;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public abstract class Predicate extends Formula {
    private static final Iterator<Formula> ITERATOR = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Formula next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Constructs a new formula.
     * @param f    the factory which created this formula
     */
    protected Predicate(FormulaFactory f) {
        super(FType.PREDICATE, f);
    }

    @Override
    public abstract long numberOfNodes();

    @Override
    public abstract long numberOfInternalNodes();

    @Override
    public int numberOfOperands() {
        return 0;
    }

    @Override
    public boolean isConstantFormula() {
        return false;
    }

    @Override
    public boolean isAtomicFormula() {
        return true;
    }

    @Override
    public boolean containsVariable(Variable variable) {
        return false;
    }

    @Override
    public boolean evaluate(Assignment assignment) {
        throw new UnsupportedOperationException("Cannot evaluate a formula with predicates with a boolean assignment");
    }

    @Override
    public Formula restrict(Assignment assignment) {
        return this;
    }

    @Override
    public boolean containsNode(Formula formula) {
        return this.equals(formula);
    }

    @Override
    public Formula substitute(Substitution substitution) {
        return this;
    }

    @Override
    public Formula negate() {
        return this.factory().not(this);
    }

    @Override
    public Stream<Formula> stream() {
        return Stream.empty();
    }

    @Override
    public Iterator<Formula> iterator() {
        return ITERATOR;
    }
}
