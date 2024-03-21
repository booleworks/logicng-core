package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.formulas.Literal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntegerClause {
    private final List<Literal> boolLiterals;
    private final List<ArithmeticLiteral> arithLiterals;

    public IntegerClause(final List<Literal> boolLiterals, final List<ArithmeticLiteral> arithLiterals) {
        this.boolLiterals = boolLiterals;
        this.arithLiterals = arithLiterals;
    }

    public IntegerClause(final Literal booLiteral) {
        this(Collections.singletonList(booLiteral), Collections.emptyList());
    }

    public IntegerClause(final ArithmeticLiteral arithLiteral) {
        this(Collections.emptyList(), Collections.singletonList(arithLiteral));
    }

    public IntegerClause() {
        this.boolLiterals = Collections.emptyList();
        this.arithLiterals = Collections.emptyList();
    }

    public List<Literal> getBoolLiterals() {
        return this.boolLiterals;
    }

    public List<ArithmeticLiteral> getArithmeticLiterals() {
        return this.arithLiterals;
    }

    public int size() {
        return this.boolLiterals.size() + this.arithLiterals.size();
    }

    public boolean isValid() {
        for (final ArithmeticLiteral lit : this.arithLiterals) {
            if (lit.isValid()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUnsat() {
        for (final ArithmeticLiteral lit : this.arithLiterals) {
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
                this.boolLiterals.stream().map(Object::toString),
                this.arithLiterals.stream().map(Object::toString)
        ).collect(Collectors.joining(", "));
        builder.append(lits);
        builder.append(">");
        return builder.toString();
    }
}
