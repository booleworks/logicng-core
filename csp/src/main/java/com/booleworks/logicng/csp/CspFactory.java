package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.encodings.CompactOrderDecoding;
import com.booleworks.logicng.csp.encodings.CompactOrderEncoding;
import com.booleworks.logicng.csp.encodings.CompactOrderEncodingContext;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderDecoding;
import com.booleworks.logicng.csp.encodings.OrderEncoding;
import com.booleworks.logicng.csp.encodings.OrderEncodingContext;
import com.booleworks.logicng.csp.functions.CspDecomposition;
import com.booleworks.logicng.csp.functions.IntegerVariablesFunction;
import com.booleworks.logicng.csp.predicates.AllDifferentPredicate;
import com.booleworks.logicng.csp.predicates.ComparisonPredicate;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.AbsoluteFunction;
import com.booleworks.logicng.csp.terms.AdditionFunction;
import com.booleworks.logicng.csp.terms.DivisionFunction;
import com.booleworks.logicng.csp.terms.IntegerConstant;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.MaxFunction;
import com.booleworks.logicng.csp.terms.MinFunction;
import com.booleworks.logicng.csp.terms.ModuloFunction;
import com.booleworks.logicng.csp.terms.MultiplicationFunction;
import com.booleworks.logicng.csp.terms.NegationFunction;
import com.booleworks.logicng.csp.terms.SubtractionFunction;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The central data structure for creating and managing CSP objects (terms, predicate, integer variables).
 */
public class CspFactory {
    /**
     * Prefix for all auxiliary variables
     */
    public static final String AUX_PREFIX = "@AUX_";
    private final IntegerConstant zero;
    private final IntegerConstant one;
    private final FormulaFactory formulaFactory;
    private final Map<Integer, IntegerConstant> integerConstants;
    private final Map<String, IntegerVariable> integerVariables;
    private final Map<Term, NegationFunction> unaryMinusTerms;
    private final Map<LinkedHashSet<Term>, Term> addTerms;
    private final Map<Pair<Term, Term>, SubtractionFunction> subTerms;
    private final Map<LinkedHashSet<Term>, MultiplicationFunction> mulTerms;
    private final Map<Term, AbsoluteFunction> absTerms;
    private final Map<LinkedHashSet<Term>, MaxFunction> maxTerms;
    private final Map<LinkedHashSet<Term>, MinFunction> minTerms;
    private final Map<Pair<Term, Integer>, ModuloFunction> modTerms;
    private final Map<Pair<Term, Integer>, DivisionFunction> divTerms;
    private final Map<LinkedHashSet<Term>, ComparisonPredicate> eqPredicates;
    private final Map<LinkedHashSet<Term>, ComparisonPredicate> nePredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> lePredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> ltPredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> gePredicates;
    private final Map<Pair<Term, Term>, ComparisonPredicate> gtPredicates;
    private final Map<LinkedHashSet<Term>, AllDifferentPredicate> allDifferentPredicates;
    private final Map<String, Integer> auxVarCounters;

    /**
     * Constructs a new factory for CSP related constructs. It uses a {@link FormulaFactory} as basis.
     * @param formulaFactory the underlying formula factory
     */
    public CspFactory(final FormulaFactory formulaFactory) {
        this.formulaFactory = formulaFactory;
        this.integerConstants = new HashMap<>();
        this.integerVariables = new HashMap<>();
        this.unaryMinusTerms = new HashMap<>();
        this.addTerms = new HashMap<>();
        this.subTerms = new HashMap<>();
        this.mulTerms = new HashMap<>();
        this.absTerms = new HashMap<>();
        this.maxTerms = new HashMap<>();
        this.minTerms = new HashMap<>();
        this.modTerms = new HashMap<>();
        this.divTerms = new HashMap<>();
        this.eqPredicates = new HashMap<>();
        this.nePredicates = new HashMap<>();
        this.lePredicates = new HashMap<>();
        this.ltPredicates = new HashMap<>();
        this.gePredicates = new HashMap<>();
        this.gtPredicates = new HashMap<>();
        this.allDifferentPredicates = new HashMap<>();
        this.auxVarCounters = new HashMap<>();
        this.zero = new IntegerConstant(0);
        this.one = new IntegerConstant(1);
        this.integerConstants.put(0, this.zero);
        this.integerConstants.put(1, this.one);
    }

    /**
     * Copies an existing CSP factory and replaces the underlying formula factory.
     * @param other          the existing CSP factory
     * @param formulaFactory the underlying formula factory
     */
    public CspFactory(final CspFactory other, final FormulaFactory formulaFactory) {
        this.formulaFactory = formulaFactory;
        this.integerConstants = new HashMap<>(other.integerConstants);
        this.integerVariables = new HashMap<>(other.integerVariables);
        this.unaryMinusTerms = new HashMap<>(other.unaryMinusTerms);
        this.addTerms = new HashMap<>(other.addTerms);
        this.subTerms = new HashMap<>(other.subTerms);
        this.mulTerms = new HashMap<>(other.mulTerms);
        this.absTerms = new HashMap<>(other.absTerms);
        this.maxTerms = new HashMap<>(other.maxTerms);
        this.minTerms = new HashMap<>(other.minTerms);
        this.modTerms = new HashMap<>(other.modTerms);
        this.divTerms = new HashMap<>(other.divTerms);
        this.eqPredicates = new HashMap<>(other.eqPredicates);
        this.nePredicates = new HashMap<>(other.nePredicates);
        this.lePredicates = new HashMap<>(other.lePredicates);
        this.ltPredicates = new HashMap<>(other.ltPredicates);
        this.gePredicates = new HashMap<>(other.gePredicates);
        this.gtPredicates = new HashMap<>(other.gtPredicates);
        this.allDifferentPredicates = new HashMap<>(other.allDifferentPredicates);
        this.auxVarCounters = new HashMap<>(other.auxVarCounters);
        this.zero = new IntegerConstant(0);
        this.one = new IntegerConstant(1);
        this.integerConstants.put(0, this.zero);
        this.integerConstants.put(1, this.one);
    }

    /**
     * Creates the integer constant for zero.
     * @return integer constant for zero
     */
    public IntegerConstant zero() {
        return zero;
    }

    /**
     * Creates the integer constant for one.
     * @return integer constant for one
     */
    public IntegerConstant one() {
        return one;
    }

    /**
     * Creates the integer constant for the given value.
     * @param value the constant integer value
     * @return the integer constant
     */
    public IntegerConstant constant(final int value) {
        if (value == 0) {
            return zero();
        } else if (value == 1) {
            return one();
        } else {
            return integerConstants.computeIfAbsent(value, c -> new IntegerConstant(value));
        }
    }

    /**
     * Creates a new integer variable with a ranged domain.
     * <p>
     * The domain must not be empty and there must not be another variable with the same name.
     * @param name       the name of the variable
     * @param lowerBound the lower bound of the variable's domain
     * @param upperBound the upper bound of the variable's domain
     * @return the new variable
     */
    public IntegerVariable variable(final String name, final int lowerBound, final int upperBound) {
        return variable(name, IntegerDomain.of(lowerBound, upperBound));
    }

    /**
     * Creates a new integer variable from individual values.
     * <p>
     * The domain must not be empty and there must not be another variable with the same name.
     * @param name   the name of the variable
     * @param values the integer domain
     * @return the new variable
     */
    public IntegerVariable variable(final String name, final Collection<Integer> values) {
        return variable(name, IntegerDomain.of(new TreeSet<>(values)));
    }

    /**
     * Creates a new integer variable from an integer domain.
     * <p>
     * The domain must not be empty and there must not be another variable with the same name.
     * @param name   the name of the variable
     * @param domain the integer domain
     * @return the new variable
     */
    public IntegerVariable variable(final String name, final IntegerDomain domain) {
        return variableIntern(name, domain, false);
    }

    private IntegerVariable variableIntern(final String name, final IntegerDomain domain, final boolean aux) {
        if (domain.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a variable with an empty domain");
        }
        final IntegerVariable existingVar = integerVariables.get(name);
        if (existingVar != null) {
            throw new IllegalArgumentException("Variable \"" + name + "\" already exists in this CSP factory");
        }
        final IntegerVariable newVariable = new IntegerVariable(name, domain, aux);
        integerVariables.put(name, newVariable);
        return newVariable;
    }

    /**
     * Creates a new auxiliary variable of type {@code type} and an integer domain.
     * @param type   the auxiliary class of the variable
     * @param domain the integer domain
     * @return a new auxiliary variable
     */
    public IntegerVariable auxVariable(final String type, final IntegerDomain domain) {
        final int counter = auxVarCounters.compute(type, (key, value) -> value == null ? 0 : value + 1);
        return variableIntern(AUX_PREFIX + type + "_" + counter, domain, true);
    }

    /**
     * Creates a new auxiliary variable of type {@code type} and an additional postfix and an integer domain.
     * <p>
     * The postfix can be used encode some addition information into the variable that helps to associate the
     * variable with its original purpose.
     * @param type    the auxiliary class of the variable
     * @param postfix the postfix of the variable
     * @param domain  the integer domain
     * @return a new auxiliary variable
     */
    public IntegerVariable auxVariable(final String type, final String postfix, final IntegerDomain domain) {
        final int counter = auxVarCounters.compute(type, (key, value) -> value == null ? 0 : value + 1);
        return variableIntern(AUX_PREFIX + type + "_" + counter + "_" + postfix, domain, true);
    }

    /**
     * Creates the negation of {@code term}
     * @param term the term
     * @return the negation of {@code term}
     */
    public Term minus(final Term term) {
        // contract double minus --x to x
        if (term instanceof NegationFunction) {
            return ((NegationFunction) term).getOperand();
        }
        if (term instanceof IntegerConstant) {
            return constant(((IntegerConstant) term).getValue() * -1);
        }
        return unaryMinusTerms.computeIfAbsent(term, NegationFunction::new);
    }

    /**
     * Create the negation of an integer constant.
     * @param constant the integer constant
     * @return the negated constant
     */
    public Term minus(final IntegerConstant constant) {
        return constant(-constant.getValue());
    }

    /**
     * Creates the addition of a list of terms.
     * @param terms the operands
     * @return the addition
     */
    public Term add(final Term... terms) {
        return add(Arrays.asList(terms));
    }

    /**
     * Creates the addition of a list of terms.
     * @param terms the operands
     * @return the addition
     */
    public Term add(final Collection<Term> terms) {
        final Collection<Term> originalOperands = terms;
        final Term foundFunction = addTerms.get(originalOperands);
        if (foundFunction != null) {
            return foundFunction;
        }
        final LinkedHashSet<Term> compactedOperands = compactifyAddOperands(originalOperands);
        final Term foundFunctionCompact = this.addTerms.get(compactedOperands);
        if (foundFunctionCompact != null) {
            return foundFunctionCompact;
        }
        if (compactedOperands.size() == 1) {
            final Term term = compactedOperands.iterator().next();
            addTerms.put(compactedOperands, term);
            return term;
        }
        final AdditionFunction addition = new AdditionFunction(compactedOperands);
        addTerms.put(compactedOperands, addition);
        return addition;
    }

    /**
     * Creates the addition of two integer constants.
     * @param left  the first integer constant
     * @param right the second integer constant
     * @return the sum as integer constant
     */
    public IntegerConstant add(final IntegerConstant left, final IntegerConstant right) {
        return constant(left.getValue() + right.getValue());
    }

    private LinkedHashSet<Term> compactifyAddOperands(final Collection<Term> originalOperands) {
        final LinkedHashMap<Term, Integer> mergedTerms = new LinkedHashMap<>();
        final LinkedHashSet<Term> compactifiedTerms = new LinkedHashSet<>();
        int constValue = 0;
        for (final Term op : originalOperands) {
            // gather all constant integers including 0
            if (op instanceof IntegerConstant) {
                constValue += ((IntegerConstant) op).getValue();
            }
            // flatten nested additions
            else if (op instanceof AdditionFunction) {
                for (final Term t : ((AdditionFunction) op).getOperands()) {
                    addOperand(t, mergedTerms);
                }
            } else {
                addOperand(op, mergedTerms);
            }
        }
        for (final Map.Entry<Term, Integer> entry : mergedTerms.entrySet()) {
            if (entry.getValue() == 1) {
                compactifiedTerms.add(entry.getKey());
            } else {
                compactifiedTerms.add(mul(entry.getValue(), entry.getKey()));
            }
        }
        if (constValue != 0) {
            compactifiedTerms.add(constant(constValue));
        }
        return compactifiedTerms;
    }

    private void addOperand(final Term op, final LinkedHashMap<Term, Integer> compactifiedTerms) {
        if (op instanceof MultiplicationFunction
                && ((MultiplicationFunction) op).getLeft() instanceof IntegerConstant) {
            final MultiplicationFunction mop = (MultiplicationFunction) op;
            if (mop.getLeft() instanceof IntegerConstant) {
                final int newValue = ((IntegerConstant) mop.getLeft()).getValue();
                compactifiedTerms.compute(mop.getRight(), (k, v) -> v == null ? newValue : newValue + v);
            }
        } else {
            compactifiedTerms.compute(op, (k, v) -> v == null ? 1 : v + 1);
        }
    }

    /**
     * Creates the subtraction of two terms.
     * @param left  the minuend
     * @param right the subtrahend
     * @return the subtraction
     */
    public Term sub(final Term left, final Term right) {
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
        return subTerms.computeIfAbsent(new Pair<>(left, right), p -> new SubtractionFunction(left, right));
    }

    /**
     * Creates the subtraction of two integer constant.
     * @param left  the minuend
     * @param right the subtrahend
     * @return the subtracted values as integer constant
     */
    public IntegerConstant sub(final IntegerConstant left, final IntegerConstant right) {
        return constant(left.getValue() - right.getValue());
    }

    /**
     * Creates the multiplication of a term with an integer.
     * @param value the constant integer value
     * @param term  the term
     * @return the multiplication
     */
    public Term mul(final int value, final Term term) {
        return mul(constant(value), term);
    }

    /**
     * Creates the multiplication of a term with an integer constant.
     * @param left  the integer constant
     * @param right the term
     * @return the multiplication
     */
    public Term mul(final Term left, final Term right) {
        // 1*a = a
        if (left.getType() == Term.Type.ONE) {
            return right;
        }
        // a*1 = a
        if (right.getType() == Term.Type.ONE) {
            return left;
        }
        // inline * in constants
        if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
            return constant(((IntegerConstant) left).getValue() * ((IntegerConstant) right).getValue());
        } else if (right instanceof IntegerConstant) {
            return mul(right, left);
        }
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        return mulTerms.computeIfAbsent(operands, o -> new MultiplicationFunction(left, right));
    }

    /**
     * Creates the absolute function of a term.
     * @param operand the operand
     * @return the absolute function
     */
    public Term abs(final Term operand) {
        // constant
        if (operand instanceof IntegerConstant) {
            return constant(Math.abs(((IntegerConstant) operand).getValue()));
        }
        return absTerms.computeIfAbsent(operand, o -> new AbsoluteFunction(operand));
    }

    /**
     * Creates the division of a term with an integer constant
     * @param left  the dividend
     * @param right the divisor
     * @return the division
     */
    public Term div(final Term left, final int right) {
        return div(left, constant(right));
    }

    /**
     * Creates the division of a term with an integer constant
     * @param left  the dividend
     * @param right the divisor
     * @return the division
     */
    public Term div(final Term left, final IntegerConstant right) {
        // no division by 0
        if (right.getValue() == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        // Decomposition only supports division by positive integers
        if (right.getValue() < 0) {
            return div(minus(left), constant(-right.getValue()));
        }
        // a/1 = a
        if (right.getValue() == 1) {
            return left;
        }
        // constant
        if (left instanceof IntegerConstant) {
            return constant(((IntegerConstant) left).getValue() / right.getValue());
        }
        return this.divTerms.computeIfAbsent(new Pair<>(left, right.getValue()),
                p -> new DivisionFunction(left, right));
    }

    /**
     * Creates the modulo function of a term with an integer constant
     * @param left  the dividend
     * @param right the divisor
     * @return the modulo function
     */
    public Term mod(final Term left, final int right) {
        return mod(left, constant(right));
    }

    /**
     * Creates the modulo function of a term with an integer constant
     * @param left  the dividend
     * @param right the divisor
     * @return the modulo function
     */
    public Term mod(final Term left, final IntegerConstant right) {
        // no division by 0 or by negative integers
        if (right.getValue() == 0) {
            throw new IllegalArgumentException("Cannot modulo by zero");
        }
        if (right.getValue() < 0) {
            throw new IllegalArgumentException("Modulo a negative number is not allowed");
        }
        // inline % in constants
        if (left instanceof IntegerConstant) {
            return constant(((IntegerConstant) left).getValue() % right.getValue());
        }
        return this.modTerms.computeIfAbsent(new Pair<>(left, right.getValue()), p -> new ModuloFunction(left, right));
    }

    /**
     * Creates the minimum function between two terms.
     * @param left  the first term
     * @param right the second term
     * @return the minimum function
     */
    public Term min(final Term left, final Term right) {
        // min(x, x) = x
        if (left.equals(right)) {
            return left;
        }
        // inline % in constants
        if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
            return constant(Math.min(((IntegerConstant) left).getValue(), ((IntegerConstant) right).getValue()));
        }
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        return minTerms.computeIfAbsent(operands, p -> new MinFunction(left, right));
    }

    /**
     * Creates the maximum function between two terms.
     * @param left  the first term
     * @param right the second term
     * @return the maximum function
     */
    public Term max(final Term left, final Term right) {
        // max(x, x) = x
        if (left.equals(right)) {
            return left;
        }
        // inline % in constants
        if (left instanceof IntegerConstant && right instanceof IntegerConstant) {
            return constant(Math.max(((IntegerConstant) left).getValue(), ((IntegerConstant) right).getValue()));
        }
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        return maxTerms.computeIfAbsent(operands, p -> new MaxFunction(left, right));
    }

    /**
     * Create a new comparison predicate
     * @param left  the left side of the comparison
     * @param right the right side of the comparison
     * @param type  the operator
     * @return the comparison predicate
     */
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

    /**
     * Creates an equality predicate.
     * @param left  the left side of the equality
     * @param right the right side of the equality
     * @return the equality predicate
     */
    public ComparisonPredicate eq(final Term left, final Term right) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        final ComparisonPredicate foundFormula = eqPredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final ComparisonPredicate predicate =
                new ComparisonPredicate(CspPredicate.Type.EQ, left, right, formulaFactory);
        eqPredicates.put(operands, predicate);
        return predicate;
    }

    /**
     * Creates an inequality predicate.
     * @param left  the left side of the inequality
     * @param right the right side of the inequality
     * @return the inequality predicate
     */
    public ComparisonPredicate ne(final Term left, final Term right) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(Arrays.asList(left, right));
        final ComparisonPredicate foundFormula = nePredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final ComparisonPredicate predicate =
                new ComparisonPredicate(CspPredicate.Type.NE, left, right, formulaFactory);
        nePredicates.put(operands, predicate);
        return predicate;
    }

    /**
     * Creates a less-than predicate
     * @param left  the left side
     * @param right the right side
     * @return the less-than predicate
     */
    public ComparisonPredicate lt(final Term left, final Term right) {
        return processComparison(left, right, ltPredicates, CspPredicate.Type.LT);
    }

    /**
     * Creates a less-than-equals predicate
     * @param left  the left side
     * @param right the right side
     * @return the less-than predicate
     */
    public ComparisonPredicate le(final Term left, final Term right) {
        return processComparison(left, right, lePredicates, CspPredicate.Type.LE);
    }

    /**
     * Creates a greater-than predicate
     * @param left  the left side
     * @param right the right side
     * @return the less-than predicate
     */
    public ComparisonPredicate gt(final Term left, final Term right) {
        return processComparison(left, right, gtPredicates, CspPredicate.Type.GT);
    }

    /**
     * Creates a greater-than-equals predicate
     * @param left  the left side
     * @param right the right side
     * @return the less-than predicate
     */
    public ComparisonPredicate ge(final Term left, final Term right) {
        return processComparison(left, right, gePredicates, CspPredicate.Type.GE);
    }

    private ComparisonPredicate processComparison(final Term left, final Term right,
                                                  final Map<Pair<Term, Term>, ComparisonPredicate> cache,
                                                  final CspPredicate.Type type) {
        final Pair<Term, Term> operands = new Pair<>(left, right);
        final ComparisonPredicate foundFormula = cache.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final ComparisonPredicate predicate = new ComparisonPredicate(type, left, right, formulaFactory);
        cache.put(operands, predicate);
        return predicate;
    }

    /**
     * Creates an all-different predicate that test whether all operands have different values.
     * @param terms the operands
     * @return the less-than predicate
     */
    public AllDifferentPredicate allDifferent(final Collection<Term> terms) {
        final LinkedHashSet<Term> operands = new LinkedHashSet<>(terms);
        final AllDifferentPredicate foundFormula = allDifferentPredicates.get(operands);
        if (foundFormula != null) {
            return foundFormula;
        }
        final AllDifferentPredicate predicate = new AllDifferentPredicate(operands, formulaFactory);
        allDifferentPredicates.put(operands, predicate);
        return predicate;
    }

    /**
     * Returns the underlying formula factory.
     * @return the underlying formula factory
     */
    public FormulaFactory getFormulaFactory() {
        return formulaFactory;
    }

    /**
     * Decomposes a formula into arithmetic clauses.
     * @param formula the formula
     * @return the decomposition result
     */
    public CspPredicate.Decomposition decompose(final Formula formula) {
        return CspDecomposition.decompose(formula, this);
    }

    /**
     * Builds a CSP problem from a formula.
     * @param formula the formula
     * @return the CSP problem
     */
    public Csp buildCsp(final Formula formula) {
        final SortedSet<Variable> variables = formula.variables(formulaFactory);
        final SortedSet<IntegerVariable> integerVariables = IntegerVariablesFunction.integerVariables(formula);
        final Set<IntegerClause> clauses = decompose(formula).getClauses();
        return Csp.fromClauses(clauses, integerVariables, variables);
    }

    /**
     * Build a CSP problem from a conjunction of CSP predicates.
     * @param predicates the predicates
     * @return the CSP problem
     */
    public Csp buildCsp(final Collection<CspPredicate> predicates) {
        final SortedSet<IntegerVariable> vars = new TreeSet<>();
        for (final CspPredicate predicate : predicates) {
            predicate.variablesInplace(vars);
        }
        final Set<IntegerClause> clauses = predicates.stream()
                .flatMap(p -> p.decompose(this).getClauses().stream())
                .collect(Collectors.toSet());
        return Csp.fromClauses(clauses, vars);
    }

    /**
     * Build a CSP problem from a conjunction of CSP predicates.
     * @param predicates the predicates
     * @return the CSP problem
     */
    public Csp buildCsp(final CspPredicate... predicates) {
        return buildCsp(Arrays.asList(predicates));
    }

    /**
     * Encodes a CSP problem as a CNF.
     * @param csp     the CSP problem
     * @param context the encoding context
     * @return the encoded CNF
     */
    public List<Formula> encodeCsp(final Csp csp, final CspEncodingContext context) {
        final EncodingResult result = EncodingResult.resultForFormula(formulaFactory);
        encodeCsp(csp, context, result);
        return result.getResult();
    }

    /**
     * Encodes a CSP problem as a CNF.
     * @param csp     the CSP problem
     * @param context the encoding context
     * @param result  the destination for the encoding
     */
    public void encodeCsp(final Csp csp, final CspEncodingContext context, final EncodingResult result) {
        switch (context.getAlgorithm()) {
            case Order:
                OrderEncoding.encode(csp, (OrderEncodingContext) context, result, this);
                break;
            case CompactOrder:
                CompactOrderEncoding.encode(csp, (CompactOrderEncodingContext) context, result, this);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    /**
     * Encodes a integer variable as a CNF.
     * @param variable the integer variable
     * @param context  the encoding context
     * @return the encoded CNF
     */
    public List<Formula> encodeVariable(final IntegerVariable variable, final CspEncodingContext context) {
        final EncodingResult result = EncodingResult.resultForFormula(formulaFactory);
        encodeVariable(variable, context, result);
        return result.getResult();
    }

    /**
     * Encodes a integer variable as a CNF.
     * @param variable the integer variable
     * @param context  the encoding context
     * @param result   the destination for the encoding
     */
    public void encodeVariable(final IntegerVariable variable, final CspEncodingContext context,
                               final EncodingResult result) {
        switch (context.getAlgorithm()) {
            case Order:
                OrderEncoding.encodeVariable(variable, (OrderEncodingContext) context, result, this);
                break;
            case CompactOrder:
                CompactOrderEncoding.encodeVariable(variable, (CompactOrderEncodingContext) context, result, this);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    /**
     * Encodes a CSP predicate as a CNF.
     * @param predicate the predicate
     * @param context   the encoding context
     * @return the encoded CNF
     */
    public List<Formula> encodeConstraint(final CspPredicate predicate, final CspEncodingContext context) {
        final EncodingResult result = EncodingResult.resultForFormula(formulaFactory);
        encodeConstraint(predicate, context, result);
        return result.getResult();
    }

    /**
     * Encodes a CSP predicate as a CNF.
     * @param predicate the predicate
     * @param context   the encoding context
     * @param result    the destination for the encoding
     */
    public void encodeConstraint(final CspPredicate predicate, final CspEncodingContext context,
                                 final EncodingResult result) {
        final CspPredicate.Decomposition decomp = predicate.decompose(this);
        switch (context.getAlgorithm()) {
            case Order:
                for (final IntegerVariable auxVar : decomp.getAuxiliaryIntegerVariables()) {
                    OrderEncoding.encodeVariable(auxVar, (OrderEncodingContext) context, result, this);
                }
                OrderEncoding.encodeClauses(decomp.getClauses(), (OrderEncodingContext) context, result, this);
                break;
            case CompactOrder:
                CompactOrderEncoding.encodeVariables(decomp.getAuxiliaryIntegerVariables(),
                        (CompactOrderEncodingContext) context, result, this);
                CompactOrderEncoding.encodeClauses(decomp.getClauses(), (CompactOrderEncodingContext) context, result,
                        this);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    /**
     * Decodes a propositional model for a CSP problem to an {@link CspAssignment}.
     * @param model   the propositional model
     * @param csp     the CSP problem
     * @param context the encoding context
     * @return the decoded model
     */
    public CspAssignment decode(final Assignment model, final Csp csp, final CspEncodingContext context) {
        switch (context.getAlgorithm()) {
            case Order:
                return OrderDecoding.decode(model, csp, (OrderEncodingContext) context, this);
            case CompactOrder:
                return CompactOrderDecoding.decode(model, csp, (CompactOrderEncodingContext) context, this);
            default:
                throw new UnsupportedOperationException(
                        "Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    /**
     * Decodes a propositional model to an {@link CspAssignment}.
     * @param model            the propositional model
     * @param integerVariables the relevant integer variables
     * @param booleanVariables the relevant boolean variables
     * @param context          the encoding context
     * @return the decoded model
     */
    public CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                final Collection<Variable> booleanVariables, final CspEncodingContext context) {
        switch (context.getAlgorithm()) {
            case Order:
                return OrderDecoding.decode(model, integerVariables, booleanVariables, (OrderEncodingContext) context,
                        this);
            case CompactOrder:
                return CompactOrderDecoding.decode(model, integerVariables, booleanVariables,
                        (CompactOrderEncodingContext) context, this);
            default:
                throw new UnsupportedOperationException(
                        "Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }

    /**
     * Decodes a propositional model to an {@link CspAssignment}.
     * @param model            the propositional model
     * @param integerVariables the relevant integer variables
     * @param context          the encoding context
     * @return the decoded model
     */
    public CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                final CspEncodingContext context) {
        switch (context.getAlgorithm()) {
            case Order:
                return OrderDecoding.decode(model, integerVariables, (OrderEncodingContext) context, this);
            case CompactOrder:
                return CompactOrderDecoding.decode(model, integerVariables, (CompactOrderEncodingContext) context,
                        this);
            default:
                throw new UnsupportedOperationException(
                        "Unsupported csp encoding algorithm: " + context.getAlgorithm());
        }
    }
}


