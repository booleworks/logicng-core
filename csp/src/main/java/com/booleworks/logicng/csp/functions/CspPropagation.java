package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A class grouping functions for propagating CSP problems.
 */
public class CspPropagation {
    /**
     * Prefix for auxiliary variables introduced by the propagation.
     */
    public static String BOUNDED_AUX_VAR = "PROPAGATION";

    private CspPropagation() {
    }

    /**
     * Propagates a CSP problem. This is an optimization that reduces the variable's domains if it recognizes that
     * the full domain is not necessary.
     * <p>
     * A problem can only be propagated once!
     * @param csp the un-propagated CSP problem
     * @param cf  the factory
     * @return the propagated CSP problem
     */
    public static Csp propagate(final Csp csp, final CspFactory cf) {
        if (!csp.getPropagateSubstitutions().isEmpty()) {
            throw new IllegalArgumentException("Propagating a CSP more than once is not supported");
        }
        final IntegerVariableSubstitution restrictions =
                new IntegerVariableSubstitution(csp.getPropagateSubstitutions());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final IntegerClause clause : csp.getClauses()) {
                if (calculateNewBounds(clause, restrictions, cf)) {
                    changed = true;
                }
            }
        }
        if (!restrictions.isEmpty()) {
            final Set<IntegerClause> newClauses =
                    csp.getClauses().stream().map(c -> rebuildClause(c, restrictions)).filter(c -> !c.isValid())
                            .collect(Collectors.toSet());
            return Csp.fromClauses(newClauses, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                    restrictions);
        } else {
            return csp;
        }
    }

    private static boolean calculateNewBounds(final IntegerClause clause,
                                              final IntegerVariableSubstitution restrictions, final CspFactory cf) {
        boolean changed = false;
        for (final IntegerVariable v : clause.getCommonVariables()) {
            assert clause.getBoolLiterals().isEmpty();
            final IntegerVariable currentV = restrictions.getOrSelf(v);
            if (currentV.isUnsatisfiable()) {
                continue;
            }
            int[] bound = null;
            for (final ArithmeticLiteral lit : clause.getArithmeticLiterals()) {
                final int[] b = getBound(lit, currentV, restrictions);
                if (b == null) {
                    bound = null;
                    break;
                } else {
                    if (bound == null) {
                        bound = b;
                    } else {
                        bound[0] = Math.min(bound[0], b[0]);
                        bound[1] = Math.max(bound[1], b[1]);
                    }
                }
            }
            if (bound != null && bound[0] <= bound[1]) {
                final IntegerVariable oldVar = restrictions.getOrSelf(v);
                final IntegerVariable newVar = boundVariable(oldVar, bound[0], bound[1], cf);
                if (newVar != oldVar) {
                    restrictions.add(v, newVar);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static IntegerClause rebuildClause(final IntegerClause clause,
                                               final IntegerVariableSubstitution assignment) {
        final TreeSet<ArithmeticLiteral> newLits = clause.getArithmeticLiterals().stream()
                .map(l -> l.substitute(assignment))
                .filter(Objects::nonNull)
                .filter(l -> !l.isUnsat())
                .collect(Collector.of(TreeSet::new, TreeSet::add, (left, right) -> {
                    left.addAll(right);
                    return left;
                }));
        if (newLits.equals(clause.getArithmeticLiterals())) {
            return clause;
        } else {
            return new IntegerClause(clause.getBoolLiterals(), newLits);
        }
    }

    private static IntegerVariable boundVariable(final IntegerVariable variable, final int lb, final int ub,
                                                 final CspFactory cf) {
        final IntegerDomain d = variable.getDomain().bound(lb, ub);
        if (d == variable.getDomain()) {
            return variable;
        }
        return cf.auxVariable(BOUNDED_AUX_VAR, variable.getName(), d);
    }

    private static int[] getBound(final ArithmeticLiteral literal, final IntegerVariable v,
                                  final IntegerVariableSubstitution restrictions) {
        if (literal instanceof LinearLiteral) {
            final LinearLiteral l = (LinearLiteral) literal;
            final int a = l.getSum().getA(v);
            final IntegerDomain d = l.getSum().getDomainExcept(v, restrictions);
            int lb = v.getDomain().lb();
            int ub = v.getDomain().ub();
            switch (l.getOperator()) {
                case LE:
                    if (a > 0) {
                        ub = divfloor(-d.lb(), a);
                    } else if (a < 0) {
                        lb = divceil(-d.lb(), a);
                    }
                    break;
                case EQ:
                    if (a > 0) {
                        lb = divceil(-d.ub(), a);
                        ub = divfloor(-d.lb(), a);
                    } else if (a < 0) {
                        lb = divceil(-d.lb(), a);
                        ub = divfloor(-d.ub(), a);
                    }
                    break;
                case NE:
                    return null;
            }
            if (lb > ub) {
                return null;
            }
            return new int[]{lb, ub};
        } else {
            throw new RuntimeException("Cannot calculate bound of " + literal.getClass());
        }
    }

    /**
     * ceil(b/a)
     */
    private static int divceil(final int b, final int a) {
        if ((a >= 0 && b >= 0) ||
                (a < 0 && b < 0)) {
            return b / a;
        } else if (a < 0) {
            return (-b + a + 1) / -a;
        } else {
            return (b - a + 1) / a;
        }
    }

    /**
     * floor(b/a)
     */
    private static int divfloor(final int b, final int a) {
        if (a >= 0 && b >= 0) {
            return b / a;
        } else if (a < 0 && b < 0) {
            return -b / -a;
        } else if (a >= 0) {
            return (b - a + 1) / a;
        } else {
            return (-b + a + 1) / -a;
        }
    }
}
