package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderDecoding;
import com.booleworks.logicng.csp.encodings.OrderEncoding;
import com.booleworks.logicng.csp.encodings.OrderReduction;
import com.booleworks.logicng.csp.predicates.AllDifferentPredicate;
import com.booleworks.logicng.csp.predicates.ComparisonPredicate;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.AdditionFunction;
import com.booleworks.logicng.csp.terms.IntegerConstant;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.MultiplicationFunction;
import com.booleworks.logicng.csp.terms.NegationFunction;
import com.booleworks.logicng.csp.terms.SubtractionFunction;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class CspFactory {
    private static final String AUX_PREFIX = "@AUX_";
    private static final String BOUND_PREFIX = "@BOUND_";
    private final IntegerConstant zero;
    private final IntegerConstant one;
    private final FormulaFactory formulaFactory;
    private final Map<Integer, IntegerConstant> integerConstants;
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
        this.zero = new IntegerConstant(this, 0);
        this.one = new IntegerConstant(this, 1);
        this.integerConstants.put(0, this.zero);
        this.integerConstants.put(1, this.one);
    }

    public CspFactory(final CspFactory other, final FormulaFactory formulaFactory) {
        this.formulaFactory = formulaFactory;
        this.integerConstants = new HashMap<>(other.integerConstants);
        this.integerVariables = new HashMap<>(other.integerVariables);
        this.unaryMinusTerms = new HashMap<>(other.unaryMinusTerms);
        this.addTerms = new HashMap<>(other.addTerms);
        this.subTerms = new HashMap<>(other.subTerms);
        this.mulTerms = new HashMap<>(other.mulTerms);
        this.eqPredicates = new HashMap<>(other.eqPredicates);
        this.nePredicates = new HashMap<>(other.nePredicates);
        this.lePredicates = new HashMap<>(other.lePredicates);
        this.ltPredicates = new HashMap<>(other.ltPredicates);
        this.gePredicates = new HashMap<>(other.gePredicates);
        this.gtPredicates = new HashMap<>(other.gtPredicates);
        this.allDifferentPredicates = new HashMap<>(other.allDifferentPredicates);
        this.auxCounter = other.auxCounter;
        this.zero = new IntegerConstant(this, 0);
        this.one = new IntegerConstant(this, 1);
        this.integerConstants.put(0, this.zero);
        this.integerConstants.put(1, this.one);
    }

    public IntegerConstant zero() {
        return zero;
    }

    public IntegerConstant one() {
        return one;
    }

    public IntegerConstant constant(final int value) {
        if (value == 0) {
            return zero();
        } else if (value == 1) {
            return one();
        } else {
            return integerConstants.computeIfAbsent(value, c -> new IntegerConstant(this, value));
        }
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
        final IntegerVariable existingVar = integerVariables.get(name);
        if (existingVar != null) {
            if (!domain.equals(existingVar.getDomain())) {
                throw new IllegalArgumentException("Variable " + name + " already exists in the CSP factory with another domain");
            }
            return existingVar;
        }
        final IntegerVariable newVariable = new IntegerVariable(this, name, domain);
        integerVariables.put(name, newVariable);
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
        return variable(AUX_PREFIX + auxCounter++, domain);
    }

    public Term minus(final Term term) {
        // contract double minus --x to x
        if (term instanceof NegationFunction) {
            return ((NegationFunction) term).getOperand();
        }
        return unaryMinusTerms.computeIfAbsent(term, t -> new NegationFunction(this, term));
    }

    public Term minus(final IntegerConstant constant) {
        return constant(-constant.getValue());
    }

    public Term add(final Term... terms) {
        return add(Arrays.asList(terms));
    }

    public Term add(final Collection<Term> terms) {
        final LinkedHashSet<Term> originalOperands = new LinkedHashSet<>(terms);
        final Term foundFunction = addTerms.get(originalOperands);
        if (foundFunction != null) {
            return foundFunction;
        }
        final LinkedHashSet<Term> compactedOperands = compactifyAddOperands(originalOperands);
        if (compactedOperands.size() == 1) {
            final Term term = compactedOperands.iterator().next();
            addTerms.put(originalOperands, term);
            addTerms.put(compactedOperands, term);
            return term;
        }
        final AdditionFunction addition = new AdditionFunction(this, compactedOperands);
        addTerms.put(originalOperands, addition);
        addTerms.put(compactedOperands, addition);
        return addition;
    }

    public IntegerConstant add(final IntegerConstant left, final IntegerConstant right) {
        return constant(left.getValue() + right.getValue());
    }

    private LinkedHashSet<Term> compactifyAddOperands(final LinkedHashSet<Term> originalOperands) {
        final LinkedHashSet<Term> compactifiedTerms = new LinkedHashSet<>();
        int constValue = 0;
        for (final Term op : originalOperands) {
            // gather all constant integers including 0
            if (op instanceof IntegerConstant) {
                constValue += ((IntegerConstant) op).getValue();
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
            return zero;
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
        if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
            return constant(((IntegerConstant) left).getValue() - ((IntegerConstant) right).getValue());
        }
        return subTerms.computeIfAbsent(new Pair<>(left, right), p -> new SubtractionFunction(this, left, right));
    }

    public IntegerConstant sub(final IntegerConstant left, final IntegerConstant right) {
        return constant(left.getValue() - right.getValue());
    }

    public Term mul(final int value, final Term term) {
        return mul(constant(value), term);
    }

    public Term mul(final IntegerConstant left, final Term right) {
        // a*0 or 0*a = 0
        if (left.getType() == Term.Type.ZERO || right.getType() == Term.Type.ZERO) {
            return zero;
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
        if (right instanceof IntegerConstant) {
            return constant(left.getValue() * ((IntegerConstant) right).getValue());
        }
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        return mulTerms.computeIfAbsent(operands, o -> new MultiplicationFunction(this, left, right));
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
        final ComparisonPredicate foundFormula = eqPredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final ComparisonPredicate predicate = new ComparisonPredicate(this, CspPredicate.Type.EQ, left, right);
        eqPredicates.put(operands, predicate);
        return predicate;
    }

    public ComparisonPredicate ne(final Term left, final Term right) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        final ComparisonPredicate foundFormula = nePredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final ComparisonPredicate predicate = new ComparisonPredicate(this, CspPredicate.Type.NE, left, right);
        nePredicates.put(operands, predicate);
        return predicate;
    }

    public ComparisonPredicate lt(final Term left, final Term right) {
        return processComparison(left, right, ltPredicates, (l, r) -> l.getValue() < r.getValue(), CspPredicate.Type.LT);
    }

    public ComparisonPredicate le(final Term left, final Term right) {
        return processComparison(left, right, lePredicates, (l, r) -> l.getValue() <= r.getValue(), CspPredicate.Type.LE);
    }

    public ComparisonPredicate gt(final Term left, final Term right) {
        return processComparison(left, right, gtPredicates, (l, r) -> l.getValue() > r.getValue(), CspPredicate.Type.GT);
    }

    public ComparisonPredicate ge(final Term left, final Term right) {
        return processComparison(left, right, gePredicates, (l, r) -> l.getValue() >= r.getValue(), CspPredicate.Type.GE);
    }

    private ComparisonPredicate processComparison(final Term left, final Term right, final Map<Pair<Term, Term>, ComparisonPredicate> cache,
                                                  final BiPredicate<IntegerConstant, IntegerConstant> test, final CspPredicate.Type type) {
        final Pair<Term, Term> operands = new Pair<>(left, right);
        final ComparisonPredicate foundFormula = cache.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final ComparisonPredicate predicate = new ComparisonPredicate(this, type, left, right);
        cache.put(operands, predicate);
        return predicate;
    }

    public AllDifferentPredicate allDifferent(final Collection<Term> terms) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(terms);
        final AllDifferentPredicate foundFormula = allDifferentPredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final AllDifferentPredicate predicate = new AllDifferentPredicate(this, operands);
        allDifferentPredicates.put(operands, predicate);
        return predicate;
    }

    public FormulaFactory formulaFactory() {
        return formulaFactory;
    }

    public Csp buildCsp(final Collection<CspPredicate> predicates) {
        final Set<IntegerClause> clauses = predicates.stream().flatMap(p -> p.decompose().stream()).collect(Collectors.toSet());
        return Csp.fromClauses(clauses);
    }

    public Csp buildCsp(final CspPredicate... predicates) {
        return buildCsp(Arrays.asList(predicates));
    }

    public List<Formula> encodeCsp(final Csp csp, final CspEncodingContext context) {
        final Csp newCsp;
        final EncodingResult result = EncodingResult.resultForFormula(formulaFactory);
        switch (context.getAlgorithm()) {
            case Order:
                newCsp = OrderReduction.reduce(csp, context, formulaFactory);
                OrderEncoding.encode(newCsp, context, result, this);
                return result.result();
            default:
                throw new UnsupportedOperationException("Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    public List<Formula> encodeVariable(final IntegerVariable variable, final CspEncodingContext context) {
        final EncodingResult result = EncodingResult.resultForFormula(formulaFactory);
        switch (context.getAlgorithm()) {
            case Order:
                OrderEncoding.encodeVariable(variable, context, result, this);
                return result.result();
            default:
                throw new UnsupportedOperationException("Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    public List<Formula> encodeConstraint(final CspPredicate predicate, final CspEncodingContext context) {
        Set<IntegerClause> clauses = predicate.decompose();
        final EncodingResult result = EncodingResult.resultForFormula(formulaFactory);
        switch (context.getAlgorithm()) {
            case Order:
                clauses = OrderReduction.reduce(clauses, context, formulaFactory);
                OrderEncoding.encodeClauses(clauses, context, result, this);
                return result.result();
            default:
                throw new UnsupportedOperationException("Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    public CspAssignment decode(final Assignment model, final Csp csp, final CspEncodingContext context) {
        return decode(model, csp.getIntegerVariables(), context);
    }

    public CspAssignment decode(final Assignment model, final Collection<IntegerVariable> variables, final CspEncodingContext context) {
        switch (context.getAlgorithm()) {
            case Order:
                return OrderDecoding.decode(model, variables, context, this);
            default:
                throw new UnsupportedOperationException("Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }
}


