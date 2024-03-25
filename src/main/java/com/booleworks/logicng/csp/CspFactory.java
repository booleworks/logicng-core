package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.predicates.AllDifferentPredicate;
import com.booleworks.logicng.csp.predicates.ComparisonPredicate;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.AdditionFunction;
import com.booleworks.logicng.csp.terms.Constant;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.MultiplicationFunction;
import com.booleworks.logicng.csp.terms.NegationFunction;
import com.booleworks.logicng.csp.terms.SubtractionFunction;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class CspFactory {
    private static final String AUX_PREFIX = "@AUX_";
    private static final String BOUND_PREFIX = "@BOUND_";
    private final Constant zero;
    private final Constant one;
    private final FormulaFactory formulaFactory;
    private final Map<Integer, Constant> integerConstants;
    private final Map<String, IntegerVariable> integerVariables;
    private final Map<Term, NegationFunction> unaryMinusTerms;
    private final Map<LinkedHashSet<Term>, Term> addTerms;
    private final Map<Pair<Term, Term>, SubtractionFunction> subTerms;
    private final Map<LinkedHashSet<Term>, MultiplicationFunction> mulTerms;
    private final Map<LinkedHashSet<Term>, ComparisonPredicate> eqPredicates;
    private final Map<LinkedHashSet<Term>, ComparisonPredicate> nePredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> lePredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> ltPredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> gePredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> gtPredicates;
    private final Map<LinkedHashSet<Term>, AllDifferentPredicate> allDifferentPredicates;
    private int auxCounter;

    public CspFactory(final FormulaFactory formulaFactory) {
        this.formulaFactory = formulaFactory;
        this.integerConstants = new HashMap<>();
        this.integerVariables = new HashMap<>();
        this.unaryMinusTerms = new HashMap<>();
        this.addTerms = new HashMap<>();
        this.subTerms = new HashMap<>();
        this.mulTerms = new HashMap<>();
        this.eqPredicates = new HashMap<>();
        this.nePredicates = new HashMap<>();
        this.lePredicates = new HashMap<>();
        this.ltPredicates = new HashMap<>();
        this.gePredicates = new HashMap<>();
        this.gtPredicates = new HashMap<>();
        this.allDifferentPredicates = new HashMap<>();
        this.auxCounter = 0;
        this.zero = new Constant(this, 0);
        this.one = new Constant(this, 1);
        this.integerConstants.put(0, this.zero);
        this.integerConstants.put(1, this.one);
    }

    public Constant zero() {
        return this.zero;
    }

    public Constant one() {
        return this.one;
    }

    public Constant constant(final int value) {
        return this.integerConstants.computeIfAbsent(value, c -> new Constant(this, value));
    }

    public IntegerVariable variable(final String name, final int lowerBound, final int upperBound) {
        return variable(name, new IntegerRangeDomain(lowerBound, upperBound));
    }

    public IntegerVariable variable(final String name, final Collection<Integer> values) {
        return variable(name, new IntegerSetDomain(new TreeSet<>(values)));
    }

    public IntegerVariable variable(final String name, final IntegerDomain domain) {
        if (domain.isEmpty()) {
            throw new IllegalArgumentException("Empty domain for variable " + name);
        }
        final IntegerVariable existingVar = this.integerVariables.get(name);
        if (existingVar != null) {
            if (!domain.equals(existingVar.getDomain())) {
                throw new IllegalArgumentException("Variable " + name + " already exists in the CSP factory with another domain");
            }
            return existingVar;
        }
        final IntegerVariable newVariable = new IntegerVariable(this, name, domain);
        this.integerVariables.put(name, newVariable);
        return newVariable;
    }

    public IntegerVariable boundVariable(final IntegerVariable variable, final int lb, final int ub) {
        final IntegerDomain d = variable.getDomain().bound(lb, ub);
        if (d == variable.getDomain()) {
            return variable;
        }
        final String name = getUnboundedVariableOf(variable).getName();
        return variable(BOUND_PREFIX + "[" + d.lb() + "," + d.ub() + "]_" + name, d);
    }

    public IntegerVariable getUnboundedVariableOf(final IntegerVariable variable) {
        if (variable.getName().startsWith(BOUND_PREFIX)) {
            final String[] parts = variable.getName().split("_");
            final String name = Arrays.stream(parts).skip(2).collect(Collectors.joining("_"));
            return integerVariables.get(name);
        } else {
            return variable;
        }
    }

    public IntegerVariable auxVariable(final IntegerDomain domain) {
        return variable(AUX_PREFIX + this.auxCounter++, domain);
    }

    public Term minus(final Term term) {
        // contract double minus --x to x
        if (term instanceof NegationFunction) {
            return ((NegationFunction) term).getOperand();
        }
        // inline - in constants
        if (term instanceof Constant) {
            return constant(-((Constant) term).getValue());
        }
        return this.unaryMinusTerms.computeIfAbsent(term, t -> new NegationFunction(this, term));
    }

    public Term add(final Term... terms) {
        return add(Arrays.asList(terms));
    }

    public Term add(final Collection<Term> terms) {
        final LinkedHashSet<Term> originalOperands = new LinkedHashSet<>(terms);
        final Term foundFunction = this.addTerms.get(originalOperands);
        if (foundFunction != null) {
            return foundFunction;
        }
        final LinkedHashSet<Term> compactedOperands = compactifyAddOperands(originalOperands);
        if (compactedOperands.size() == 1) {
            final Term term = compactedOperands.iterator().next();
            this.addTerms.put(originalOperands, term);
            this.addTerms.put(compactedOperands, term);
            return term;
        }
        final AdditionFunction addition = new AdditionFunction(this, compactedOperands);
        this.addTerms.put(originalOperands, addition);
        this.addTerms.put(compactedOperands, addition);
        return addition;
    }

    private LinkedHashSet<Term> compactifyAddOperands(final LinkedHashSet<Term> originalOperands) {
        final LinkedHashSet<Term> compactifiedTerms = new LinkedHashSet<>();
        int constValue = 0;
        for (final Term op : originalOperands) {
            // gather all constant integers including 0
            if (op instanceof Constant) {
                constValue += ((Constant) op).getValue();
            }
            // flatten nested additions
            else if (op instanceof AdditionFunction) {
                compactifiedTerms.addAll(((AdditionFunction) op).getOperands());
            } else {
                compactifiedTerms.add(op);
            }
        }
        if (constValue != 0) {
            compactifiedTerms.add(constant(constValue));
        }
        return compactifiedTerms;
    }

    public Term sub(final Term left, final Term right) {
        // x-x = 0
        if (left.equals(right)) {
            return this.zero;
        }
        // 0 - x = -x
        if (left.getType() == Term.Type.ZERO) {
            return minus(right);
        }
        // x - 0 = x
        if (right.getType() == Term.Type.ZERO) {
            return left;
        }
        // inline - in constants
        if (left instanceof Constant && right instanceof Constant) {
            return constant(((Constant) left).getValue() - ((Constant) right).getValue());
        }
        return this.subTerms.computeIfAbsent(new Pair<>(left, right), p -> new SubtractionFunction(this, left, right));
    }

    public Term mul(final int value, final Term term) {
        return mul(constant(value), term);
    }

    public Term mul(final Term left, final Term right) {
        // a*0 or 0*a = 0
        if (left.getType() == Term.Type.ZERO || right.getType() == Term.Type.ZERO) {
            return this.zero;
        }
        // 1*a = a
        if (left.getType() == Term.Type.ONE) {
            return right;
        }
        // a*1 = a
        if (right.getType() == Term.Type.ONE) {
            return left;
        }
        // inline * in constants
        if (left instanceof Constant && right instanceof Constant) {
            return constant(((Constant) left).getValue() * ((Constant) right).getValue());
        }
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        return this.mulTerms.computeIfAbsent(operands, o -> new MultiplicationFunction(this, left, right));
    }

    public ComparisonPredicate comparison(final Term left, final Term right, final CspPredicate.Type type) {
        switch (type) {
            case EQ:
                return eq(left, right);
            case NE:
                return ne(left, right);
            case LT:
                return lt(left, right);
            case LE:
                return le(left, right);
            case GT:
                return gt(left, right);
            case GE:
                return ge(left, right);
            default:
                throw new IllegalArgumentException("Invalid type for comparison predicates:" + type);
        }
    }

    public ComparisonPredicate eq(final Term left, final Term right) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        final ComparisonPredicate foundFormula = this.eqPredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        //if (left.equals(right)) {
        //    this.eqPredicates.put(operands, this.formulaFactory.verum());
        //    return this.formulaFactory.verum();
        //}
        //if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
        //    final Constant constant = this.formulaFactory.constant(((IntegerConstant) left).getValue() == ((IntegerConstant) right).getValue());
        //    this.eqPredicates.put(operands, constant);
        //    return constant;
        //}
        final ComparisonPredicate predicate = new ComparisonPredicate(this, CspPredicate.Type.EQ, left, right);
        this.eqPredicates.put(operands, predicate);
        return predicate;
    }

    public ComparisonPredicate ne(final Term left, final Term right) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        final ComparisonPredicate foundFormula = this.nePredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        //if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
        //    final Constant constant = this.formulaFactory.constant(((IntegerConstant) left).getValue() != ((IntegerConstant) right).getValue());
        //    this.nePredicates.put(operands, constant);
        //    return constant;
        //}
        final ComparisonPredicate predicate = new ComparisonPredicate(this, CspPredicate.Type.NE, left, right);
        this.nePredicates.put(operands, predicate);
        return predicate;
    }

    public ComparisonPredicate lt(final Term left, final Term right) {
        return processComparison(left, right, this.ltPredicates, (l, r) -> l.getValue() < r.getValue(), CspPredicate.Type.LT);
    }

    public ComparisonPredicate le(final Term left, final Term right) {
        return processComparison(left, right, this.lePredicates, (l, r) -> l.getValue() <= r.getValue(), CspPredicate.Type.LE);
    }

    public ComparisonPredicate gt(final Term left, final Term right) {
        return processComparison(left, right, this.gtPredicates, (l, r) -> l.getValue() > r.getValue(), CspPredicate.Type.GT);
    }

    public ComparisonPredicate ge(final Term left, final Term right) {
        return processComparison(left, right, this.gePredicates, (l, r) -> l.getValue() >= r.getValue(), CspPredicate.Type.GE);
    }

    private ComparisonPredicate processComparison(final Term left, final Term right, final Map<Pair<Term, Term>, ComparisonPredicate> cache,
                                                  final BiPredicate<Constant, Constant> test, final CspPredicate.Type type) {
        final Pair<Term, Term> operands = new Pair<>(left, right);
        final ComparisonPredicate foundFormula = cache.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        //if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
        //    final Constant constant = this.formulaFactory.constant(test.test((IntegerConstant) left, (IntegerConstant) right));
        //    cache.put(operands, constant);
        //    return constant;
        //}
        final ComparisonPredicate predicate = new ComparisonPredicate(this, type, left, right);
        cache.put(operands, predicate);
        return predicate;
    }

    public AllDifferentPredicate allDifferent(final Collection<Term> terms) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(terms);
        final AllDifferentPredicate foundFormula = this.allDifferentPredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        // zero or one terms always have different values
        //if (terms.size() <= 1) {
        //    this.allDifferentPredicates.put(operands, this.formulaFactory.verum());
        //    return this.formulaFactory.verum();
        //}
        final AllDifferentPredicate predicate = new AllDifferentPredicate(this, operands);
        this.allDifferentPredicates.put(operands, predicate);
        return predicate;
    }

    public FormulaFactory getFormulaFactory() {
        return this.formulaFactory;
    }

    public Csp buildCsp(final Collection<CspPredicate> predicates) {
        final Set<IntegerClause> clauses = predicates.stream().flatMap(p -> p.decompose().stream()).collect(Collectors.toSet());
        return Csp.fromClauses(this, clauses);
    }

    public Csp buildCsp(final CspPredicate... predicates) {
        return buildCsp(Arrays.asList(predicates));
    }
}


