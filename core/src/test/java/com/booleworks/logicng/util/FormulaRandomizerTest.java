// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.functions.FormulaDepthFunction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class FormulaRandomizerTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder().seed(42).build();

    @Test
    public void testDeterminism() {
        final Formula expected =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build()).formula(3);
        assertThat(new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build()).formula(3))
                .isEqualTo(expected);
        assertThat(new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(43).build()).formula(3))
                .isNotEqualTo(expected);
        assertThat(new FormulaRandomizer(f, FormulaRandomizerConfig.builder().build()).formula(3))
                .isNotEqualTo(expected);
        final List<Formula> expectedList = randomFormulas();
        for (int i = 0; i < 10; i++) {
            assertThat(randomFormulas()).isEqualTo(expectedList);
        }
    }

    @Test
    public void testConstant() {
        final FormulaRandomizer random = new FormulaRandomizer(f, config);
        int numTrue = 0;
        for (int i = 0; i < 100; i++) {
            final Formula constant = random.constant();
            assertThat(constant.isConstantFormula()).isTrue();
            if (constant == f.verum()) {
                numTrue++;
            }
        }
        assertThat(numTrue).isStrictlyBetween(40, 60);
    }

    @Test
    public void testVariable() {
        final List<Variable> vars = Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"));
        final FormulaRandomizer random =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().variables(vars).seed(42).build());
        int numA = 0;
        int numB = 0;
        int numC = 0;
        for (int i = 0; i < 100; i++) {
            final Variable variable = random.variable();
            assertThat(variable.getName()).isIn("A", "B", "C");
            switch (variable.getName()) {
                case "A":
                    numA++;
                    break;
                case "B":
                    numB++;
                    break;
                case "C":
                    numC++;
                    break;
            }
        }
        assertThat(numA).isStrictlyBetween(20, 40);
        assertThat(numB).isStrictlyBetween(20, 40);
        assertThat(numC).isStrictlyBetween(20, 40);

        final SortedSet<Variable> vars2 = new TreeSet<>();
        for (int i = 0; i < 20; i++) {
            vars2.add(f.variable("TEST_VAR_" + i));
        }
        final FormulaRandomizer random2 = new FormulaRandomizer(f, FormulaRandomizerConfig.builder()
                .variables(vars2)
                .weightPbc(1)
                .weightCc(1)
                .weightAmo(1)
                .weightExo(1)
                .seed(42).build());
        for (int i = 0; i < 100; i++) {
            assertThat(random2.formula(4).variables(f)).isSubsetOf(vars2);
        }
    }

    @Test
    public void testLiteral() {
        final FormulaRandomizer random = new FormulaRandomizer(f,
                FormulaRandomizerConfig.builder().weightPositiveLiteral(40).weightNegativeLiteral(60).seed(42).build());
        int numPos = 0;
        for (int i = 0; i < 100; i++) {
            final Literal literal = random.literal();
            if (literal.getPhase()) {
                numPos++;
            }
        }
        assertThat(numPos).isStrictlyBetween(30, 50);
    }

    @Test
    public void testAtom() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder()
                .weightConstant(1).weightPositiveLiteral(2).weightNegativeLiteral(3)
                .weightPbc(4).weightCc(5).weightAmo(6).weightExo(7).seed(42).build());
        int numConst = 0;
        int numPos = 0;
        int numNeg = 0;
        int numPbc = 0;
        int numCc = 0;
        int numAmo = 0;
        int numExo = 0;
        for (int i = 0; i < 1000; i++) {
            final Formula formula = random.atom();
            assertThat(formula.isAtomicFormula()).isTrue();
            if (formula.isConstantFormula()) {
                numConst++;
            } else if (formula.getType() == FType.LITERAL) {
                if (((Literal) formula).getPhase()) {
                    numPos++;
                } else {
                    numNeg++;
                }
            } else {
                final PbConstraint pbc = (PbConstraint) formula;
                if (!pbc.isCc()) {
                    numPbc++;
                } else if (pbc.getRhs() == 1 && pbc.comparator() == CType.LE) {
                    numAmo++;
                } else if (pbc.getRhs() == 1 && pbc.comparator() == CType.EQ) {
                    numExo++;
                } else {
                    numCc++;
                }
            }
        }
        assertThat(numExo).isStrictlyBetween((int) (.8 * 7 / 6 * numAmo), (int) (1.2 * 7 / 6 * numAmo));
        assertThat(numAmo).isStrictlyBetween((int) (.8 * 6 / 5 * numCc), (int) (1.2 * 6 / 5 * numCc));
        assertThat(numCc).isStrictlyBetween((int) (.8 * 5 / 4 * numPbc), (int) (1.2 * 5 / 4 * numPbc));
        assertThat(numPbc).isStrictlyBetween((int) (.8 * 4 / 3 * numNeg), (int) (1.2 * 4 / 3 * numNeg));
        assertThat(numNeg).isStrictlyBetween((int) (.8 * 3 / 2 * numPos), (int) (1.2 * 3 / 2 * numPos));
        assertThat(numPos).isStrictlyBetween((int) (.8 * 2 / 1 * numConst), (int) (1.2 * 2 / 1 * numConst));
        final FormulaRandomizer randomOnlyLiterals = new FormulaRandomizer(f, FormulaRandomizerConfig.builder()
                .weightConstant(0).weightPositiveLiteral(3).weightNegativeLiteral(6).seed(42).build());
        for (int i = 0; i < 100; i++) {
            assertThat(randomOnlyLiterals.atom().getType()).isEqualTo(FType.LITERAL);
        }
    }

    @Test
    public void testAnd() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build());
        for (int i = 0; i < 10; i++) {
            assertThat(random.and(0).isAtomicFormula()).isTrue();
        }
        for (int depth = 1; depth <= 7; depth++) {
            for (int i = 0; i < 10; i++) {
                final Formula formula = random.and(depth);
                assertThat(formula).isInstanceOf(And.class);
                assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(depth);
            }
        }
    }

    @Test
    public void testOr() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build());
        for (int i = 0; i < 10; i++) {
            assertThat(random.or(0).isAtomicFormula()).isTrue();
        }
        for (int depth = 1; depth <= 7; depth++) {
            for (int i = 0; i < 10; i++) {
                final Formula formula = random.or(depth);
                assertThat(formula).isInstanceOf(Or.class);
                assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(depth);
            }
        }
    }

    @Test
    public void testNot() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build());
        for (int i = 0; i < 10; i++) {
            assertThat(random.not(0).isAtomicFormula()).isTrue();
            assertThat(random.not(1).isAtomicFormula()).isTrue();
        }
        for (int depth = 2; depth <= 7; depth++) {
            for (int i = 0; i < 10; i++) {
                final Formula formula = random.not(depth);
                assertThat(formula).isInstanceOf(Not.class);
                assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(depth);
            }
        }
    }

    @Test
    public void testImpl() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build());
        for (int i = 0; i < 10; i++) {
            assertThat(random.impl(0).isAtomicFormula()).isTrue();
        }
        for (int depth = 1; depth <= 7; depth++) {
            for (int i = 0; i < 10; i++) {
                final Formula formula = random.impl(depth);
                assertThat(formula).isInstanceOf(Implication.class);
                assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(depth);
            }
        }
    }

    @Test
    public void testEquiv() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(42).build());
        for (int i = 0; i < 10; i++) {
            assertThat(random.equiv(0).isAtomicFormula()).isTrue();
        }
        for (int depth = 1; depth <= 7; depth++) {
            for (int i = 0; i < 10; i++) {
                final Formula formula = random.equiv(depth);
                assertThat(formula).isInstanceOf(Equivalence.class);
                assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(depth);
            }
        }
    }

    @Test
    public void testPbc() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(4242)
                .weightPbcCoeffPositive(3).weightPbcCoeffNegative(1)
                .weightPositiveLiteral(3).weightNegativeLiteral(1)
                .weightPbcTypeLe(5)
                .weightPbcTypeLt(4)
                .weightPbcTypeGe(3)
                .weightPbcTypeGt(2)
                .weightPbcTypeEq(1)
                .maximumCoefficientPbc(10)
                .build());
        int posCoeff = 0;
        int negCoeff = 0;
        int posLit = 0;
        int negLit = 0;
        int le = 0;
        int lt = 0;
        int ge = 0;
        int gt = 0;
        int eq = 0;
        for (int i = 0; i < 500; i++) {
            final Formula formula = random.pbc();
            assertThat(formula).isInstanceOf(PbConstraint.class);
            final PbConstraint pbc = (PbConstraint) formula;
            int posSum = 0;
            int negSum = 0;
            for (final int coefficient : pbc.getCoefficients()) {
                if (coefficient > 0) {
                    posCoeff++;
                    posSum += coefficient;
                } else {
                    negCoeff++;
                    negSum += coefficient;
                }
            }
            assertThat(pbc.numberOfOperands()).isLessThanOrEqualTo(10);
            assertThat(pbc.getRhs()).isStrictlyBetween(negSum - 1, posSum + 1);
            for (final Literal literal : pbc.literals(f)) {
                if (literal.getPhase()) {
                    posLit++;
                } else {
                    negLit++;
                }
            }
            switch (pbc.comparator()) {
                case LE:
                    le++;
                    break;
                case LT:
                    lt++;
                    break;
                case GE:
                    ge++;
                    break;
                case GT:
                    gt++;
                    break;
                case EQ:
                    eq++;
                    break;
            }
        }
        assertThat(posCoeff).isStrictlyBetween((int) (.8 * 3 * negCoeff), (int) (1.2 * 3 * negCoeff));
        assertThat(posLit).isStrictlyBetween((int) (.8 * 3 * negLit), (int) (1.2 * 3 * negLit));
        assertThat(le).isStrictlyBetween((int) (.7 * 5 / 4 * lt), (int) (1.3 * 5 / 4 * lt));
        assertThat(lt).isStrictlyBetween((int) (.7 * 4 / 3 * ge), (int) (1.3 * 4 / 3 * ge));
        assertThat(ge).isStrictlyBetween((int) (.7 * 3 / 2 * gt), (int) (1.3 * 3 / 2 * gt));
        assertThat(gt).isStrictlyBetween((int) (.7 * 2 / 1 * eq), (int) (1.3 * 2 / 1 * eq));
    }

    @Test
    public void testCc() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(4242)
                .weightPbcTypeLe(5)
                .weightPbcTypeLt(4)
                .weightPbcTypeGe(3)
                .weightPbcTypeGt(2)
                .weightPbcTypeEq(1)
                .maximumCoefficientPbc(10)
                .build());
        int le = 0;
        int lt = 0;
        int ge = 0;
        int gt = 0;
        int eq = 0;
        for (int i = 0; i < 500; i++) {
            final Formula formula = random.cc();
            assertThat(formula).isInstanceOf(CardinalityConstraint.class);
            final CardinalityConstraint cc = (CardinalityConstraint) formula;
            assertThat(cc.isCc()).isTrue();
            assertThat(cc.numberOfOperands()).isLessThanOrEqualTo(10);
            if (cc.comparator() == CType.GT) {
                assertThat(cc.getRhs()).isStrictlyBetween(-2, 11);
            } else if (cc.comparator() == CType.LT) {
                assertThat(cc.getRhs()).isStrictlyBetween(0, 11);
            } else {
                assertThat(cc.getRhs()).isStrictlyBetween(-1, 11);
            }
            switch (cc.comparator()) {
                case LE:
                    le++;
                    break;
                case LT:
                    lt++;
                    break;
                case GE:
                    ge++;
                    break;
                case GT:
                    gt++;
                    break;
                case EQ:
                    eq++;
                    break;
            }
        }
        assertThat(le).isStrictlyBetween((int) (.7 * 5 / 4 * lt), (int) (1.3 * 5 / 4 * lt));
        assertThat(lt).isStrictlyBetween((int) (.7 * 4 / 3 * ge), (int) (1.3 * 4 / 3 * ge));
        assertThat(ge).isStrictlyBetween((int) (.7 * 3 / 2 * gt), (int) (1.3 * 3 / 2 * gt));
        assertThat(gt).isStrictlyBetween((int) (.7 * 2 / 1 * eq), (int) (1.3 * 2 / 1 * eq));
    }

    @Test
    public void testAmo() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(4242)
                .maximumCoefficientPbc(10)
                .build());
        for (int i = 0; i < 100; i++) {
            final Formula formula = random.amo();
            assertThat(formula).isInstanceOf(PbConstraint.class);
            final PbConstraint amo = (PbConstraint) formula;
            assertThat(amo.isCc()).isTrue();
            assertThat(amo.getRhs()).isEqualTo(1);
            assertThat(amo.comparator()).isEqualTo(CType.LE);
        }
    }

    @Test
    public void testExo() {
        final FormulaRandomizer random = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(4242)
                .maximumCoefficientPbc(10)
                .build());
        for (int i = 0; i < 100; i++) {
            final Formula formula = random.exo();
            assertThat(formula).isInstanceOf(PbConstraint.class);
            final PbConstraint amo = (PbConstraint) formula;
            assertThat(amo.isCc()).isTrue();
            assertThat(amo.getRhs()).isEqualTo(1);
            assertThat(amo.comparator()).isEqualTo(CType.EQ);
        }
    }

    @Test
    public void testFormula() {
        final FormulaRandomizer random = new FormulaRandomizer(f,
                FormulaRandomizerConfig.builder().weightConstant(1).weightPositiveLiteral(2).weightNegativeLiteral(3)
                        .weightAnd(4).weightOr(5).weightNot(6).weightImpl(7).weightEquiv(8).seed(42).build());
        final Map<String, Integer> occurrences = new HashMap<>();
        occurrences.put("constant", 0);
        occurrences.put("posLit", 0);
        occurrences.put("negLit", 0);
        occurrences.put("and", 0);
        occurrences.put("or", 0);
        occurrences.put("not", 0);
        occurrences.put("impl", 0);
        occurrences.put("equiv", 0);
        for (int i = 0; i < 10000; i++) {
            final Formula formula = random.formula(3);
            countOccurrences(formula, occurrences, 3);
            assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(3);
        }
        final int totalOccurrences =
                occurrences.get("and") + occurrences.get("or") + occurrences.get("impl") + occurrences.get("equiv");
        // Considering constants does not make sense (they are always removed)
        // Considering literals does not make sense (they must be at the leafs,
        // so their effective weight will be considerably higher)
        // Not is also a special case (it will be reduced to a literal if it's
        // operand is itself a literal)
        assertThat(occurrences.get("and")).isStrictlyBetween(4 * totalOccurrences / 30 / 2,
                4 * totalOccurrences / 30 * 2);
        assertThat(occurrences.get("or")).isStrictlyBetween(5 * totalOccurrences / 30 / 2,
                5 * totalOccurrences / 30 * 2);
        assertThat(occurrences.get("impl")).isStrictlyBetween(7 * totalOccurrences / 30 / 2,
                7 * totalOccurrences / 30 * 2);
        assertThat(occurrences.get("equiv")).isStrictlyBetween(8 * totalOccurrences / 30 / 2,
                8 * totalOccurrences / 30 * 2);
        assertThat(occurrences.get("pbc")).isNull();
        assertThat(occurrences.get("cc")).isNull();
        assertThat(occurrences.get("amo")).isNull();
        assertThat(occurrences.get("exo")).isNull();
    }

    @Test
    public void testFormulaWithPbcs() {
        final FormulaRandomizer random = new FormulaRandomizer(f,
                FormulaRandomizerConfig.builder()
                        .weightConstant(0).weightPositiveLiteral(1).weightNegativeLiteral(1)
                        .weightPbc(1).weightCc(1).weightAmo(1).weightExo(1)
                        .weightAnd(3).weightOr(3).weightNot(0).weightImpl(0).weightEquiv(0)
                        .seed(42).build());
        final Map<String, Integer> occurrences = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            final Formula formula = random.formula(3);
            countOccurrences(formula, occurrences, 3);
            assertThat(formula.apply(new FormulaDepthFunction(f))).isLessThanOrEqualTo(3);
        }
        assertThat(occurrences.get("negLit")).isStrictlyBetween((int) (.8 * occurrences.get("posLit")),
                (int) (1.2 * occurrences.get("posLit")));
        assertThat(occurrences.get("pbc")).isStrictlyBetween((int) (.8 * occurrences.get("posLit")),
                (int) (1.2 * occurrences.get("posLit")));
        assertThat(occurrences.get("cc")).isStrictlyBetween((int) (.8 * occurrences.get("posLit")),
                (int) (1.2 * occurrences.get("posLit")));
        assertThat(occurrences.get("exo")).isStrictlyBetween((int) (.8 * occurrences.get("posLit")),
                (int) (1.2 * occurrences.get("posLit")));
        assertThat(occurrences.get("amo")).isStrictlyBetween((int) (.8 * occurrences.get("posLit")),
                (int) (1.2 * occurrences.get("posLit")));
    }

    private void countOccurrences(final Formula formula, final Map<String, Integer> occurrences,
                                  final int remainingDepth) {
        switch (formula.getType()) {
            case TRUE:
            case FALSE:
                occurrences.merge("constant", 1, Integer::sum);
                return;
            case LITERAL:
                if (((Literal) formula).getPhase()) {
                    occurrences.merge("posLit", 1, Integer::sum);
                } else {
                    occurrences.merge("negLit", 1, Integer::sum);
                }
                return;
            case AND:
                occurrences.merge("and", 1, Integer::sum);
                break;
            case OR:
                occurrences.merge("or", 1, Integer::sum);
                break;
            case NOT:
                occurrences.merge("not", 1, Integer::sum);
                break;
            case IMPL:
                occurrences.merge("impl", 1, Integer::sum);
                break;
            case EQUIV:
                occurrences.merge("equiv", 1, Integer::sum);
                break;
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                if (!pbc.isCc()) {
                    occurrences.merge("pbc", 1, Integer::sum);
                } else if (pbc.getRhs() == 1 && pbc.comparator() == CType.LE) {
                    occurrences.merge("amo", 1, Integer::sum);
                } else if (pbc.getRhs() == 1 && pbc.comparator() == CType.EQ) {
                    occurrences.merge("exo", 1, Integer::sum);
                } else {
                    occurrences.merge("cc", 1, Integer::sum);
                }
                break;
        }
        for (final Formula op : formula) {
            countOccurrences(op, occurrences, remainingDepth - 1);
        }
    }

    private List<Formula> randomFormulas() {
        final FormulaRandomizer random = new FormulaRandomizer(f, config);
        final List<Formula> formulas = new ArrayList<>();
        formulas.add(random.constant());
        formulas.add(random.variable());
        formulas.add(random.literal());
        formulas.add(random.atom());
        formulas.add(random.and(3));
        formulas.add(random.or(3));
        formulas.add(random.not(3));
        formulas.add(random.impl(3));
        formulas.add(random.equiv(3));
        formulas.add(random.formula(3));
        formulas.addAll(random.constraintSet(5, 3));
        return formulas;
    }

    @Test
    public void testMaximumOperandsAnd() {
        final FormulaRandomizer random =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().maximumOperandsAnd(10).seed(42).build());
        for (int i = 0; i < 100; i++) {
            final Formula formula = random.and(1);
            assertThat(formula.getType()).isEqualTo(FType.AND);
            final And and = (And) formula;
            assertThat(and.numberOfOperands()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    public void testMaximumOperandsOr() {
        final FormulaRandomizer random =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().maximumOperandsOr(10).seed(42).build());
        for (int i = 0; i < 100; i++) {
            final Formula formula = random.or(1);
            assertThat(formula.getType()).isEqualTo(FType.OR);
            final Or or = (Or) formula;
            assertThat(or.numberOfOperands()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    public void testMaximumOperandsPbc() {
        final FormulaRandomizer random =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().maximumOperandsPbc(10).seed(42).build());
        for (int i = 0; i < 100; i++) {
            final Formula formula = random.pbc();
            assertThat(formula.getType()).isEqualTo(FType.PBC);
            final PbConstraint pbc = (PbConstraint) formula;
            assertThat(pbc.literals(f).size()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    public void testMaximumOperandsCc() {
        final FormulaRandomizer random =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().maximumOperandsOr(10).seed(42).build());
        for (int i = 0; i < 100; i++) {
            final Formula formula = random.cc();
            assertThat(formula.getType()).isEqualTo(FType.PBC);
            final CardinalityConstraint cc = (CardinalityConstraint) formula;
            assertThat(cc.literals(f).size()).isLessThanOrEqualTo(10);
        }
    }
}
