package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntegerClause implements Comparable<IntegerClause> {
    private final SortedSet<Literal> boolLiterals;
    private final SortedSet<ArithmeticLiteral> arithLiterals;

    public IntegerClause(final SortedSet<Literal> boolLiterals, final SortedSet<ArithmeticLiteral> arithLiterals) {
        this.boolLiterals = boolLiterals;
        this.arithLiterals = arithLiterals;
    }

    public IntegerClause(final Literal booLiteral) {
        this(new TreeSet<>(), Collections.emptySortedSet());
        boolLiterals.add(booLiteral);
    }

    public IntegerClause(final ArithmeticLiteral arithLiteral) {
        this(Collections.emptySortedSet(), new TreeSet<>());
        arithLiterals.add(arithLiteral);
    }

    public IntegerClause() {
        this.boolLiterals = Collections.emptySortedSet();
        this.arithLiterals = Collections.emptySortedSet();
    }

    public SortedSet<Literal> getBoolLiterals() {
        return boolLiterals;
    }

    public SortedSet<ArithmeticLiteral> getArithmeticLiterals() {
        return arithLiterals;
    }

    public Set<IntegerVariable> getCommonVariables() {
        if (!boolLiterals.isEmpty()) {
            return Collections.emptySet();
        }
        Set<IntegerVariable> commonVars = null;
        for (final ArithmeticLiteral lit : arithLiterals) {
            final Set<IntegerVariable> vs = lit.getVariables();
            if (commonVars == null) {
                commonVars = vs;
            } else {
                commonVars = commonVars.stream().filter(vs::contains).collect(Collectors.toSet());
            }
        }
        return Objects.requireNonNullElse(commonVars, Collections.emptySet());
    }

    public int size() {
        return boolLiterals.size() + arithLiterals.size();
    }

    public boolean isValid() {
        for (final ArithmeticLiteral lit : arithLiterals) {
            if (lit.isValid()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUnsat() {
        for (final ArithmeticLiteral lit : arithLiterals) {
            if (!lit.isUnsat()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CLAUSE<");
        final String lits = Stream.concat(
                boolLiterals.stream().map(Object::toString),
                arithLiterals.stream().map(Object::toString)
        ).collect(Collectors.joining(", "));
        builder.append(lits);
        builder.append(">");
        return builder.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        final IntegerClause that = (IntegerClause) o;

        if (!boolLiterals.equals(that.boolLiterals)) {return false;}
        return arithLiterals.equals(that.arithLiterals);
    }

    @Override
    public int hashCode() {
        int result = boolLiterals.hashCode();
        result = 31 * result + arithLiterals.hashCode();
        return result;
    }

    @Override
    public int compareTo(final IntegerClause other) {
        final Iterator<ArithmeticLiteral> leftArithIter = arithLiterals.iterator();
        final Iterator<ArithmeticLiteral> rightArithIter = other.arithLiterals.iterator();
        while (leftArithIter.hasNext() && rightArithIter.hasNext()) {
            final int c = leftArithIter.next().compareTo(rightArithIter.next());
            if (c != 0) {
                return c;
            }
        }
        if (arithLiterals.size() < other.arithLiterals.size()) {
            return -1;
        } else if (other.arithLiterals.size() < arithLiterals.size()) {
            return 1;
        }
        final Iterator<Literal> leftBoolIter = boolLiterals.iterator();
        final Iterator<Literal> rightBoolIter = other.boolLiterals.iterator();
        while (leftBoolIter.hasNext() && rightBoolIter.hasNext()) {
            final int c = leftBoolIter.next().compareTo(rightBoolIter.next());
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(boolLiterals.size(), other.boolLiterals.size());
    }

    public static Set<IntegerClause> factorize(final Set<IntegerClause> left, final Set<IntegerClause> right) {
        final Set<IntegerClause> clauses = new TreeSet<>();
        for (final IntegerClause l : left) {
            for (final IntegerClause r : right) {
                final SortedSet<Literal> newBools = new TreeSet<>(l.boolLiterals);
                final SortedSet<ArithmeticLiteral> newAriths = new TreeSet<>(l.arithLiterals);
                newBools.addAll(r.boolLiterals);
                newAriths.addAll(r.arithLiterals);
                clauses.add(new IntegerClause(newBools, newAriths));
            }
        }
        return clauses;
    }
}
