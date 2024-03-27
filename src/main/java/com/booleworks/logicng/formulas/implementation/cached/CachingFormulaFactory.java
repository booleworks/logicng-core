// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import static com.booleworks.logicng.formulas.cache.PredicateCacheEntry.IS_CNF;
import static com.booleworks.logicng.formulas.cache.TransformationCacheEntry.FACTORIZED_CNF;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.AuxVarType;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.cache.CacheEntry;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CachingFormulaFactory extends FormulaFactory {

    private final PropositionalParser parser;
    Map<String, Variable> posLiterals;
    Map<String, Literal> negLiterals;
    Set<Variable> generatedVariables;
    Map<Formula, Not> nots;
    Map<Pair<Formula, Formula>, Implication> implications;
    Map<LinkedHashSet<? extends Formula>, Equivalence> equivalences;
    Map<LinkedHashSet<? extends Formula>, And> ands2;
    Map<LinkedHashSet<? extends Formula>, And> ands3;
    Map<LinkedHashSet<? extends Formula>, And> ands4;
    Map<LinkedHashSet<? extends Formula>, And> andsN;
    Map<LinkedHashSet<? extends Formula>, Or> ors2;
    Map<LinkedHashSet<? extends Formula>, Or> ors3;
    Map<LinkedHashSet<? extends Formula>, Or> ors4;
    Map<LinkedHashSet<? extends Formula>, Or> orsN;
    Map<PBOperands, PBConstraint> pbConstraints;
    Map<CCOperands, CardinalityConstraint> cardinalityConstraints;
    Map<CacheEntry, Map<Formula, Formula>> transformationCache;
    Map<CacheEntry, Map<Formula, Boolean>> predicateCache;
    Map<CacheEntry, Map<Formula, Object>> functionCache;
    Map<PBConstraint, List<Formula>> pbEncodingCache;

    /**
     * Constructor for a new formula factory with the default configuration.
     */
    public CachingFormulaFactory() {
        this(FormulaFactoryConfig.builder().build());
    }

    /**
     * Constructor for a new formula factory.
     * @param config the configuration for this formula factory
     */
    public CachingFormulaFactory(final FormulaFactoryConfig config) {
        super(config);
        cFalse = new LngCachedFalse(this);
        cTrue = new LngCachedTrue(this);
        parser = new PropositionalParser(this);
        clear();
    }

    @Override
    protected Formula internalImplication(final Formula left, final Formula right) {
        final Pair<Formula, Formula> key = new Pair<>(left, right);
        Implication implication = implications.get(key);
        if (implication == null) {
            implication = new LngCachedImplication(left, right, this);
            implications.put(key, implication);
        }
        return implication;
    }

    @Override
    protected Formula internalEquivalence(final Formula left, final Formula right) {
        final LinkedHashSet<Formula> key = new LinkedHashSet<>(List.of(left, right));
        Equivalence equivalence = equivalences.get(key);
        if (equivalence == null) {
            equivalence = new LngCachedEquivalence(left, right, this);
            equivalences.put(key, equivalence);
        }
        return equivalence;
    }

    @Override
    protected Formula internalNot(final Formula operand) {
        Not not = nots.get(operand);
        if (not == null) {
            not = new LngCachedNot(operand, this);
            nots.put(operand, not);
        }
        return not;
    }

    /**
     * Creates a new conjunction.
     * @param operandsIn the formulas
     * @return a new conjunction
     */
    @Override
    protected Formula internalAnd(final LinkedHashSet<? extends Formula> operandsIn) {
        final LinkedHashSet<? extends Formula> operands = importOrPanicLHS(operandsIn);
        And tempAnd = null;
        Map<LinkedHashSet<? extends Formula>, And> opAndMap = andsN;
        if (operands.size() > 1) {
            switch (operands.size()) {
                case 2:
                    opAndMap = ands2;
                    break;
                case 3:
                    opAndMap = ands3;
                    break;
                case 4:
                    opAndMap = ands4;
                    break;
                default:
                    break;
            }
            tempAnd = opAndMap.get(operands);
        }
        if (tempAnd != null) {
            return tempAnd;
        }
        final LinkedHashSet<? extends Formula> condensedOperands =
                operands.size() < 2 ? operands : condenseOperandsAnd(operands);
        if (condensedOperands == null) {
            return falsum();
        }
        if (condensedOperands.isEmpty()) {
            return verum();
        }
        if (condensedOperands.size() == 1) {
            return condensedOperands.iterator().next();
        }
        final And and;
        Map<LinkedHashSet<? extends Formula>, And> condAndMap = andsN;
        switch (condensedOperands.size()) {
            case 2:
                condAndMap = ands2;
                break;
            case 3:
                condAndMap = ands3;
                break;
            case 4:
                condAndMap = ands4;
                break;
            default:
                break;
        }
        and = condAndMap.get(condensedOperands);
        if (and == null) {
            tempAnd = new LngCachedAnd(condensedOperands, this);
            setCnfCaches(tempAnd, cnfCheck);
            opAndMap.put(operands, tempAnd);
            condAndMap.put(condensedOperands, tempAnd);
            return tempAnd;
        }
        opAndMap.put(operands, and);
        return and;
    }

    @Override
    protected Formula internalCnf(final LinkedHashSet<? extends Formula> clausesIn) {
        final LinkedHashSet<? extends Formula> clauses = importOrPanicLHS(clausesIn);
        if (clauses.isEmpty()) {
            return verum();
        }
        if (clauses.size() == 1) {
            return clauses.iterator().next();
        }
        Map<LinkedHashSet<? extends Formula>, And> opAndMap = andsN;
        switch (clauses.size()) {
            case 2:
                opAndMap = ands2;
                break;
            case 3:
                opAndMap = ands3;
                break;
            case 4:
                opAndMap = ands4;
                break;
            default:
                break;
        }
        And tempAnd = opAndMap.get(clauses);
        if (tempAnd != null) {
            return tempAnd;
        }
        tempAnd = new LngCachedAnd(clauses, this);
        setCnfCaches(tempAnd, true);
        opAndMap.put(clauses, tempAnd);
        return tempAnd;
    }

    @Override
    protected Formula internalOr(final LinkedHashSet<? extends Formula> operandsIn) {
        final LinkedHashSet<? extends Formula> operands = importOrPanicLHS(operandsIn);
        Or tempOr = null;
        Map<LinkedHashSet<? extends Formula>, Or> opOrMap = orsN;
        if (operands.size() > 1) {
            switch (operands.size()) {
                case 2:
                    opOrMap = ors2;
                    break;
                case 3:
                    opOrMap = ors3;
                    break;
                case 4:
                    opOrMap = ors4;
                    break;
                default:
                    break;
            }
            tempOr = opOrMap.get(operands);
        }
        if (tempOr != null) {
            return tempOr;
        }
        final LinkedHashSet<? extends Formula> condensedOperands =
                operands.size() < 2 ? operands : condenseOperandsOr(operands);
        if (condensedOperands == null) {
            return verum();
        }
        if (condensedOperands.isEmpty()) {
            return falsum();
        }
        if (condensedOperands.size() == 1) {
            return condensedOperands.iterator().next();
        }
        final Or or;
        Map<LinkedHashSet<? extends Formula>, Or> condOrMap = orsN;
        switch (condensedOperands.size()) {
            case 2:
                condOrMap = ors2;
                break;
            case 3:
                condOrMap = ors3;
                break;
            case 4:
                condOrMap = ors4;
                break;
            default:
                break;
        }
        or = condOrMap.get(condensedOperands);
        if (or == null) {
            tempOr = new LngCachedOr(condensedOperands, this);
            setCnfCaches(tempOr, cnfCheck);
            opOrMap.put(operands, tempOr);
            condOrMap.put(condensedOperands, tempOr);
            return tempOr;
        }
        opOrMap.put(operands, or);
        return or;
    }

    @Override
    protected Formula internalClause(final LinkedHashSet<Literal> literalsIn) {
        final LinkedHashSet<? extends Formula> literals = importOrPanicLHS(literalsIn);
        if (literals.isEmpty()) {
            return falsum();
        }
        if (literals.size() == 1) {
            return literals.iterator().next();
        }
        Map<LinkedHashSet<? extends Formula>, Or> opOrMap = orsN;
        switch (literals.size()) {
            case 2:
                opOrMap = ors2;
                break;
            case 3:
                opOrMap = ors3;
                break;
            case 4:
                opOrMap = ors4;
                break;
            default:
                break;
        }
        Or tempOr = opOrMap.get(literals);
        if (tempOr != null) {
            return tempOr;
        }
        tempOr = new LngCachedOr(literals, this);
        setCnfCaches(tempOr, true);
        opOrMap.put(literals, tempOr);
        return tempOr;
    }

    @Override
    public Variable variable(final String name) {
        if (readOnly) {
            throwReadOnlyException();
        }
        Variable var = posLiterals.get(name);
        if (var == null) {
            var = new LngCachedVariable(name, this);
            posLiterals.put(name, var);
        }
        return var;
    }

    @Override
    protected Literal internalNegativeLiteral(final String name) {
        Literal lit = negLiterals.get(name);
        if (lit == null) {
            lit = new LngCachedLiteral(name, false, this);
            negLiterals.put(name, lit);
        }
        return lit;
    }

    @Override
    protected Formula internalPbc(final List<? extends Literal> literals, final List<Integer> coefficients,
                                  final CType comparator, final int rhs) {
        final PBOperands operands = new PBOperands(literals, coefficients, comparator, rhs);
        PBConstraint constraint = pbConstraints.get(operands);
        if (constraint == null) {
            constraint = new LngCachedPBConstraint(literals, coefficients, comparator, rhs, this);
            pbConstraints.put(operands, constraint);
        }
        return constraint;
    }

    @Override
    protected Formula internalCc(final List<? extends Literal> literals, final CType comparator, final int rhs) {
        final CCOperands operands = new CCOperands(literals, comparator, rhs);
        CardinalityConstraint constraint = cardinalityConstraints.get(operands);
        if (constraint == null) {
            constraint = new LngCachedCardinalityConstraint(importOrPanic(literals), comparator, rhs, this);
            cardinalityConstraints.put(operands, constraint);
        }
        return constraint;
    }

    @Override
    public Variable newAuxVariable(final AuxVarType type) {
        return newAuxVariable(type, null);
    }

    @Override
    public Variable newAuxVariable(final AuxVarType type, final String prefix) {
        if (readOnly) {
            throwReadOnlyException();
        }
        final String name = auxVarPrefixes.get(type) + Objects.requireNonNullElse(prefix, "") + auxVarCounters.get(type).getAndIncrement();
        final Variable var = variable(name);
        generatedVariables.add(var);
        return var;
    }

    @Override
    public boolean isGeneratedVariable(final Variable var) {
        return generatedVariables.contains(var);
    }

    private void setCnfCaches(final Formula formula, final boolean isCNF) {
        if (isCNF) {
            predicateCache.computeIfAbsent(IS_CNF, k -> new HashMap<>()).put(formula, true);
            transformationCache.computeIfAbsent(FACTORIZED_CNF, k -> new HashMap<>()).put(formula, formula);
        } else {
            predicateCache.computeIfAbsent(IS_CNF, k -> new HashMap<>()).put(formula, false);
        }
    }

    /**
     * Returns the complete transformation cache for a given cache entry type.
     * <p>
     * Attention: this cache should only be modified by formula transformations
     * and not be altered in any other way. Manipulating this cache manually can
     * lead to a serious malfunction of algorithms.
     * @param key the cache entry type
     * @return the cache (mapping from formula to formula)
     */
    public Map<Formula, Formula> getTransformationCacheForType(final CacheEntry key) {
        return transformationCache.computeIfAbsent(key, m -> new HashMap<>());
    }

    /**
     * Returns the complete function cache for a given cache entry type.
     * <p>
     * Attention: this cache should only be modified by formula functions and
     * not be altered in any other way. Manipulating this cache manually can
     * lead to a serious malfunction of algorithms.
     * @param key the cache entry type
     * @param <T> the type of the cache result
     * @return the cache (mapping from formula to formula)
     */
    @SuppressWarnings("unchecked")
    public <T> Map<Formula, T> getFunctionCacheForType(final CacheEntry key) {
        return (Map<Formula, T>) functionCache.computeIfAbsent(key, m -> new HashMap<>());
    }

    /**
     * Returns the complete predicate cache for a given cache entry type.
     * <p>
     * Attention: this cache should only be modified by formula predicate and
     * not be altered in any other way. Manipulating this cache manually can
     * lead to a serious malfunction of algorithms.
     * @param key the cache entry type
     * @return the cache (mapping from formula to formula)
     */
    public Map<Formula, Boolean> getPredicateCacheForType(final CacheEntry key) {
        return predicateCache.computeIfAbsent(key, m -> new HashMap<>());
    }

    @Override
    protected Formula negateOrNull(final Formula formula) {
        if (formula.type() == FType.FALSE || formula.type() == FType.TRUE || formula.type() == FType.NOT) {
            return formula.negate(this);
        } else if (formula.type() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            final String name = lit.name();
            return lit.phase() ? negLiterals.get(name) : posLiterals.get(name);
        } else {
            return nots.get(formula);
        }
    }

    /**
     * Removes all formulas from the factory cache.
     */
    @Override
    public void clear() {
        super.clear();
        posLiterals = new HashMap<>();
        negLiterals = new HashMap<>();
        generatedVariables = new HashSet<>();
        nots = new HashMap<>();
        implications = new HashMap<>();
        equivalences = new HashMap<>();
        ands2 = new HashMap<>();
        ands3 = new HashMap<>();
        ands4 = new HashMap<>();
        andsN = new HashMap<>();
        ors2 = new HashMap<>();
        ors3 = new HashMap<>();
        ors4 = new HashMap<>();
        orsN = new HashMap<>();
        pbConstraints = new HashMap<>();
        cardinalityConstraints = new HashMap<>();
        transformationCache = new HashMap<>();
        predicateCache = new HashMap<>();
        functionCache = new HashMap<>();
        pbEncodingCache = new HashMap<>();
    }

    @Override
    public Formula parse(final String string) throws ParserException {
        if (readOnly) {
            throwReadOnlyException();
        }
        return parser.parse(string);
    }

    /**
     * Returns the statistics for this formula factory.
     * @return the statistics for this formula factory
     */
    public Statistics statistics() {
        final Statistics statistics = new Statistics();
        statistics.name = name;
        statistics.positiveLiterals = posLiterals.size();
        statistics.negativeLiterals = negLiterals.size();
        statistics.negations = nots.size();
        statistics.implications = implications.size();
        statistics.equivalences = equivalences.size();
        statistics.conjunctions2 = ands2.size();
        statistics.conjunctions3 = ands3.size();
        statistics.conjunctions4 = ands4.size();
        statistics.conjunctionsN = andsN.size();
        statistics.disjunctions2 = ors2.size();
        statistics.disjunctions3 = ors3.size();
        statistics.disjunctions4 = ors4.size();
        statistics.disjunctionsN = orsN.size();
        statistics.pbcs = pbConstraints.size();
        statistics.ccs = cardinalityConstraints.size();
        statistics.ccCounter = auxVarCounters.get(AuxVarType.CC).get();
        statistics.pbCounter = auxVarCounters.get(AuxVarType.PBC).get();
        statistics.cnfCounter = auxVarCounters.get(AuxVarType.CNF).get();
        return statistics;
    }

    @Override
    public String toString() {
        return "Name:              " + name + System.lineSeparator() +
                "Positive Literals: " + posLiterals.size() + System.lineSeparator() +
                "Negative Literals: " + negLiterals.size() + System.lineSeparator() +
                "Negations:         " + nots.size() + System.lineSeparator() +
                "Implications:      " + implications.size() + System.lineSeparator() +
                "Equivalences:      " + equivalences.size() + System.lineSeparator() +
                "Conjunctions (2):  " + ands2.size() + System.lineSeparator() +
                "Conjunctions (3):  " + ands3.size() + System.lineSeparator() +
                "Conjunctions (4):  " + ands4.size() + System.lineSeparator() +
                "Conjunctions (>4): " + andsN.size() + System.lineSeparator() +
                "Disjunctions (2):  " + ors2.size() + System.lineSeparator() +
                "Disjunctions (3):  " + ors3.size() + System.lineSeparator() +
                "Disjunctions (4):  " + ors4.size() + System.lineSeparator() +
                "Disjunctions (>4): " + orsN.size() + System.lineSeparator() +
                "Pseudo Booleans:   " + pbConstraints.size() + System.lineSeparator() +
                "CCs:               " + cardinalityConstraints.size() + System.lineSeparator();
    }

    /**
     * Helper class for the operands of a pseudo-Boolean constraint.
     */
    private final static class PBOperands {
        private final List<? extends Literal> literals;
        private final List<Integer> coefficients;
        private final CType comparator;
        private final int rhs;

        /**
         * Constructs a new instance.
         * @param literals     the literals of the constraint
         * @param coefficients the coefficients of the constraint
         * @param comparator   the comparator of the constraint
         * @param rhs          the right-hand side of the constraint
         */
        public PBOperands(final List<? extends Literal> literals, final List<Integer> coefficients,
                          final CType comparator, final int rhs) {
            this.literals = literals;
            this.coefficients = coefficients;
            this.comparator = comparator;
            this.rhs = rhs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rhs, comparator, coefficients, literals);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other instanceof PBOperands) {
                final PBOperands o = (PBOperands) other;
                return rhs == o.rhs && comparator == o.comparator && coefficients.equals(o.coefficients) &&
                        literals.equals(o.literals);
            }
            return false;
        }
    }

    /**
     * Helper class for the operands of a cardinality constraint.
     */
    private final static class CCOperands {
        private final List<? extends Literal> literals;
        private final CType comparator;
        private final int rhs;

        /**
         * Constructs a new instance.
         * @param literals   the literals of the constraint
         * @param comparator the comparator of the constraint
         * @param rhs        the right-hand side of the constraint
         */
        public CCOperands(final List<? extends Literal> literals, final CType comparator, final int rhs) {
            this.literals = literals;
            this.comparator = comparator;
            this.rhs = rhs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rhs, comparator, literals);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other instanceof CCOperands) {
                final CCOperands o = (CCOperands) other;
                return rhs == o.rhs && comparator == o.comparator && literals.equals(o.literals);
            }
            return false;
        }
    }

    /**
     * A class for statistics of the formula factory.
     */
    public static final class Statistics {
        private String name;
        private int positiveLiterals;
        private int negativeLiterals;
        private int negations;
        private int implications;
        private int equivalences;
        private int conjunctions2;
        private int conjunctions3;
        private int conjunctions4;
        private int conjunctionsN;
        private int disjunctions2;
        private int disjunctions3;
        private int disjunctions4;
        private int disjunctionsN;
        private int pbcs;
        private int ccs;
        private int ccCounter;
        private int pbCounter;
        private int cnfCounter;

        /**
         * Returns the name of the formula factory.
         * @return the name of the formula factory
         */
        public String name() {
            return name;
        }

        /**
         * Returns the number of positive literals in the factory.
         * @return the number of positive literals in the factory
         */
        public int positiveLiterals() {
            return positiveLiterals;
        }

        /**
         * Returns the number of negative literals in the factory.
         * @return the number of negative literals in the factory
         */
        public int negativeLiterals() {
            return negativeLiterals;
        }

        /**
         * Returns the number of negations in the factory.
         * @return the number of negations in the factory
         */
        public int negations() {
            return negations;
        }

        /**
         * Returns the number of implications in the factory.
         * @return the number of implications in the factory
         */
        public int implications() {
            return implications;
        }

        /**
         * Returns the number of equivalences in the factory.
         * @return the number of equivalences in the factory
         */
        public int equivalences() {
            return equivalences;
        }

        /**
         * Returns the number of conjunctions of size 2 in the factory.
         * @return the number of conjunctions of size 2 in the factory
         */
        public int conjunctions2() {
            return conjunctions2;
        }

        /**
         * Returns the number of conjunctions of size 3 in the factory.
         * @return the number of conjunctions of size 3 in the factory
         */
        public int conjunctions3() {
            return conjunctions3;
        }

        /**
         * Returns the number of conjunctions of size 4 in the factory.
         * @return the number of conjunctions of size 4 in the factory
         */
        public int conjunctions4() {
            return conjunctions4;
        }

        /**
         * Returns the number of conjunctions of a size &gt;4 in the factory.
         * @return the number of conjunctions of a size &gt;4 in the factory
         */
        public int conjunctionsN() {
            return conjunctionsN;
        }

        /**
         * Returns the number of disjunctions of size 2 in the factory.
         * @return the number of disjunctions of size 2 in the factory
         */
        public int disjunctions2() {
            return disjunctions2;
        }

        /**
         * Returns the number of disjunctions of size 3 in the factory.
         * @return the number of disjunctions of size 3 in the factory
         */
        public int disjunctions3() {
            return disjunctions3;
        }

        /**
         * Returns the number of disjunctions of size 4 in the factory.
         * @return the number of disjunctions of size 4 in the factory
         */
        public int disjunctions4() {
            return disjunctions4;
        }

        /**
         * Returns the number of disjunctions of a size &gt;4 in the factory.
         * @return the number of disjunctions of a size &gt;4 in the factory
         */
        public int disjunctionsN() {
            return disjunctionsN;
        }

        /**
         * Returns the number of pseudo-Boolean constraints in the factory.
         * @return the number of pseudo-Boolean constraints in the factory
         */
        public int pbcs() {
            return pbcs;
        }

        /**
         * Returns the number of cardinality constraints in the factory.
         * @return the number of cardinality constraints in the factory
         */
        public int ccs() {
            return ccs;
        }

        /**
         * Returns the number of generated cardinality constraint auxiliary
         * variables.
         * @return the number of generated cardinality constraint auxiliary
         *         variables
         */
        public int ccCounter() {
            return ccCounter;
        }

        /**
         * Returns the number of generated pseudo-Boolean auxiliary variables.
         * @return the number of generated pseudo-Boolean auxiliary variables
         */
        public int pbCounter() {
            return pbCounter;
        }

        /**
         * Returns the number of generated CNF auxiliary variables.
         * @return the number of generated CNF auxiliary variables
         */
        public int cnfCounter() {
            return cnfCounter;
        }

        /**
         * Returns the number of all formulas in the factory.
         * @return the number of all formulas in the factory
         */
        public int formulas() {
            return positiveLiterals + negativeLiterals + negations + implications + equivalences + conjunctions2 +
                    conjunctions3 + conjunctions4 + conjunctionsN + disjunctions2 + disjunctions3 + disjunctions4 +
                    disjunctionsN + pbcs + ccs;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Statistics)) {
                return false;
            }
            final Statistics that = (Statistics) o;
            return Objects.equals(name, that.name) &&
                    positiveLiterals == that.positiveLiterals &&
                    negativeLiterals == that.negativeLiterals &&
                    negations == that.negations &&
                    implications == that.implications &&
                    equivalences == that.equivalences &&
                    conjunctions2 == that.conjunctions2 &&
                    conjunctions3 == that.conjunctions3 &&
                    conjunctions4 == that.conjunctions4 &&
                    conjunctionsN == that.conjunctionsN &&
                    disjunctions2 == that.disjunctions2 &&
                    disjunctions3 == that.disjunctions3 &&
                    disjunctions4 == that.disjunctions4 &&
                    disjunctionsN == that.disjunctionsN &&
                    pbcs == that.pbcs &&
                    ccs == that.ccs &&
                    ccCounter == that.ccCounter &&
                    pbCounter == that.pbCounter &&
                    cnfCounter == that.cnfCounter;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, positiveLiterals, negativeLiterals, negations, implications, equivalences,
                    conjunctions2, conjunctions3, conjunctions4, conjunctionsN, disjunctions2, disjunctions3,
                    disjunctions4, disjunctionsN, pbcs, ccs, ccCounter, pbCounter, cnfCounter);
        }

        @Override
        public String toString() {
            return "FormulaFactoryStatistics{" +
                    "name='" + name + '\'' +
                    ", positiveLiterals=" + positiveLiterals +
                    ", negativeLiterals=" + negativeLiterals +
                    ", negations=" + negations +
                    ", implications=" + implications +
                    ", equivalences=" + equivalences +
                    ", conjunctions2=" + conjunctions2 +
                    ", conjunctions3=" + conjunctions3 +
                    ", conjunctions4=" + conjunctions4 +
                    ", conjunctionsN=" + conjunctionsN +
                    ", disjunctions2=" + disjunctions2 +
                    ", disjunctions3=" + disjunctions3 +
                    ", disjunctions4=" + disjunctions4 +
                    ", disjunctionsN=" + disjunctionsN +
                    ", pbcs=" + pbcs +
                    ", ccs=" + ccs +
                    ", ccCounter=" + ccCounter +
                    ", pbCounter=" + pbCounter +
                    ", cnfCounter=" + cnfCounter +
                    '}';
        }
    }
}
