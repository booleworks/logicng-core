package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Common {
    @SafeVarargs
    public static <G> Set<G> setFrom(final G... elms) {
        return new LinkedHashSet<>(List.of(elms));
    }

    public static IntegerClause integerClauseFrom(final Literal v, final ArithmeticLiteral a) {
        return new IntegerClause(setFrom(v), setFrom(a));
    }

    public static CspAssignment assignmentFrom(final IntegerVariable v1, final int value1) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addIntAssignment(v1, value1);
        return assignment;
    }

    public static CspAssignment assignmentFrom(final IntegerVariable v1, final int value1, final IntegerVariable v2,
                                               final int value2) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addIntAssignment(v1, value1);
        assignment.addIntAssignment(v2, value2);
        return assignment;
    }

    public static CspAssignment assignmentFrom(final IntegerVariable v1, final int value1, final IntegerVariable v2,
                                               final int value2, final IntegerVariable v3, final int value3) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addIntAssignment(v1, value1);
        assignment.addIntAssignment(v2, value2);
        assignment.addIntAssignment(v3, value3);
        return assignment;
    }

    public static CspAssignment assignmentFrom(final Literal l1) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addLiteral(l1);
        return assignment;
    }

    public static CspAssignment assignmentFrom(final Literal l1, final Literal l2) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addLiteral(l1);
        assignment.addLiteral(l2);
        return assignment;
    }

    public static CspAssignment assignmentFrom(final Literal l1, final Literal l2, final Literal l3) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addLiteral(l1);
        assignment.addLiteral(l2);
        assignment.addLiteral(l3);
        return assignment;
    }
}
