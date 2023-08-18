// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.datastructures.Tristate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class PBConstraintTest extends TestWithExampleFormulas {

    private static final FormulaFactory f =
            FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
    private static final FormulaFactory f2 =
            FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());

    private final PBConstraint pb1;
    private final PBConstraint pb2;
    private final PBConstraint pb22;
    private final CardinalityConstraint cc1;
    private final CardinalityConstraint cc2;
    private final CardinalityConstraint amo1;
    private final CardinalityConstraint amo2;
    private final CardinalityConstraint exo1;
    private final CardinalityConstraint exo2;

    public PBConstraintTest() {
        final Variable[] lits1 = new Variable[]{f.variable("a")};
        final List<Literal> lits2 = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Variable> litsCC2 = Arrays.asList(f.variable("a"), f2.variable("b"), f.variable("c"));
        final int[] coeffs1 = new int[]{3};
        final List<Integer> coeffs2 = Arrays.asList(3, -2, 7);
        pb1 = (PBConstraint) f.pbc(CType.LE, 2, lits1, coeffs1);
        pb2 = (PBConstraint) f.pbc(CType.LE, 8, lits2, coeffs2);
        pb22 = (PBConstraint) f2.pbc(CType.LE, 8, lits2, coeffs2);
        cc1 = (CardinalityConstraint) f.cc(CType.LT, 1, lits1);
        cc2 = (CardinalityConstraint) f.cc(CType.GE, 2, litsCC2);
        amo1 = (CardinalityConstraint) f.amo(lits1);
        amo2 = (CardinalityConstraint) f.amo(litsCC2);
        exo1 = (CardinalityConstraint) f.exo(lits1);
        exo2 = (CardinalityConstraint) f.exo(litsCC2);
    }

    @Test
    public void testIllegalPB() {
        final List<Literal> lits = Arrays.asList(f.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(3, -2, 7, 2);
        assertThatThrownBy(() -> f.pbc(CType.EQ, 3, lits, coeffs)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testType() {
        assertThat(pb1.type()).isEqualTo(FType.PBC);
        assertThat(pb2.type()).isEqualTo(FType.PBC);
        assertThat(cc1.type()).isEqualTo(FType.PBC);
        assertThat(cc2.type()).isEqualTo(FType.PBC);
        assertThat(amo1.type()).isEqualTo(FType.PBC);
        assertThat(amo2.type()).isEqualTo(FType.PBC);
        assertThat(exo1.type()).isEqualTo(FType.PBC);
        assertThat(exo2.type()).isEqualTo(FType.PBC);
    }

    @Test
    public void testGetters() {
        final Literal[] lits1 = new Literal[]{f.variable("a")};
        final Literal[] lits2 = new Literal[]{f2.variable("a"), f.literal("b", false), f.variable("c")};
        final Literal[] litsCC2 = new Literal[]{f.variable("a"), f.variable("b"), f2.variable("c")};
        final Integer[] coeffs1 = new Integer[]{3};
        final Integer[] coeffs2 = new Integer[]{3, -2, 7};

        final Integer[] coeffsCC1 = new Integer[]{1};
        final Integer[] coeffsCC2 = new Integer[]{1, 1, 1};

        assertThat(pb1.operands()).containsExactly(lits1);
        assertThat(pb1.coefficients()).containsExactly(coeffs1);
        assertThat(pb1.comparator()).isEqualTo(CType.LE);
        assertThat(pb1.rhs()).isEqualTo(2);
        assertThat(pb1.isCC()).isFalse();
        assertThat(pb1.isAmo()).isFalse();
        assertThat(pb1.isExo()).isFalse();
        assertThat(pb1.maxWeight()).isEqualTo(3);

        assertThat(pb2.operands()).containsExactly(lits2);
        assertThat(pb2.coefficients()).containsExactly(coeffs2);
        assertThat(pb2.comparator()).isEqualTo(CType.LE);
        assertThat(pb2.rhs()).isEqualTo(8);
        assertThat(pb2.isCC()).isFalse();
        assertThat(pb2.isAmo()).isFalse();
        assertThat(pb2.isExo()).isFalse();
        assertThat(pb2.maxWeight()).isEqualTo(7);

        assertThat(cc1.operands()).containsExactly(lits1);
        assertThat(cc1.coefficients()).containsExactly(coeffsCC1);
        assertThat(cc1.comparator()).isEqualTo(CType.LT);
        assertThat(cc1.rhs()).isEqualTo(1);
        assertThat(cc1.isCC()).isTrue();
        assertThat(cc1.isAmo()).isFalse();
        assertThat(cc1.isExo()).isFalse();
        assertThat(cc1.maxWeight()).isEqualTo(1);

        assertThat(cc2.operands()).containsExactly(litsCC2);
        assertThat(cc2.coefficients()).containsExactly(coeffsCC2);
        assertThat(cc2.comparator()).isEqualTo(CType.GE);
        assertThat(cc2.rhs()).isEqualTo(2);
        assertThat(cc2.isCC()).isTrue();
        assertThat(cc2.isAmo()).isFalse();
        assertThat(cc2.isExo()).isFalse();
        assertThat(cc2.maxWeight()).isEqualTo(1);

        assertThat(amo1.operands()).containsExactly(lits1);
        assertThat(amo1.coefficients()).containsExactly(coeffsCC1);
        assertThat(amo1.comparator()).isEqualTo(CType.LE);
        assertThat(amo1.rhs()).isEqualTo(1);
        assertThat(amo1.isCC()).isTrue();
        assertThat(amo1.isAmo()).isTrue();
        assertThat(amo1.isExo()).isFalse();
        assertThat(amo1.maxWeight()).isEqualTo(1);

        assertThat(amo2.operands()).containsExactly(litsCC2);
        assertThat(amo2.coefficients()).containsExactly(coeffsCC2);
        assertThat(amo2.comparator()).isEqualTo(CType.LE);
        assertThat(amo2.rhs()).isEqualTo(1);
        assertThat(amo2.isCC()).isTrue();
        assertThat(amo2.isAmo()).isTrue();
        assertThat(amo2.isExo()).isFalse();
        assertThat(amo2.maxWeight()).isEqualTo(1);

        assertThat(exo1.operands()).containsExactly(lits1);
        assertThat(exo1.coefficients()).containsExactly(coeffsCC1);
        assertThat(exo1.comparator()).isEqualTo(CType.EQ);
        assertThat(exo1.rhs()).isEqualTo(1);
        assertThat(exo1.isCC()).isTrue();
        assertThat(exo1.isAmo()).isFalse();
        assertThat(exo1.isExo()).isTrue();
        assertThat(exo1.maxWeight()).isEqualTo(1);

        assertThat(exo2.operands()).containsExactly(litsCC2);
        assertThat(exo2.coefficients()).containsExactly(coeffsCC2);
        assertThat(exo2.comparator()).isEqualTo(CType.EQ);
        assertThat(exo2.rhs()).isEqualTo(1);
        assertThat(exo2.isCC()).isTrue();
        assertThat(exo2.isAmo()).isFalse();
        assertThat(exo2.isExo()).isTrue();
        assertThat(exo2.maxWeight()).isEqualTo(1);
    }

    @Test
    public void testIsCC() {
        final Literal[] lits1 = new Literal[]{f2.variable("a"), f.variable("b")};
        final Literal[] lits2 = new Literal[]{f2.variable("a"), f.literal("b", false)};
        final int[] coeffs1 = new int[]{1, 1};
        final int[] coeffs2 = new int[]{-1, 1};

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits1, coeffs1)).isCC()).isTrue();

        // Corner cases
        assertThat(((PBConstraint) f.pbc(CType.LE, 0, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.LT, 1, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.GE, 0, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.GT, -1, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 0, lits1, coeffs1)).isCC()).isTrue();

        assertThat(((PBConstraint) f.pbc(CType.LE, -1, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 0, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, -1, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, -2, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, -1, lits1, coeffs1)).isCC()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits1, coeffs2)).isCC()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits2, coeffs1)).isCC()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits2, coeffs2)).isCC()).isFalse();
    }

    @Test
    public void testIsAmo() {
        final Literal[] lits1 = new Literal[]{f2.variable("a"), f.variable("b")};
        final Literal[] lits2 = new Literal[]{f2.variable("a"), f.literal("b", false)};
        final int[] coeffs1 = new int[]{1, 1};
        final int[] coeffs2 = new int[]{-1, 1};

        assertThat(((PBConstraint) f.pbc(CType.LE, 1, lits1, coeffs1)).isAmo()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.LT, 2, lits1, coeffs1)).isAmo()).isTrue();
        assertThat(((PBConstraint) f.pbc(CType.LT, 1, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 1, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 1, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 1, lits1, coeffs1)).isAmo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 3, lits1, coeffs1)).isAmo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits1, coeffs2)).isAmo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits2, coeffs1)).isAmo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits2, coeffs2)).isAmo()).isFalse();
    }

    @Test
    public void testIsExo() {
        final Literal[] lits1 = new Literal[]{f2.variable("a"), f.variable("b")};
        final Literal[] lits2 = new Literal[]{f2.variable("a"), f.literal("b", false)};
        final int[] coeffs1 = new int[]{1, 1};
        final int[] coeffs2 = new int[]{-1, 1};

        assertThat(((PBConstraint) f.pbc(CType.LE, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 2, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 1, lits1, coeffs1)).isExo()).isTrue();

        assertThat(((PBConstraint) f.pbc(CType.LE, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 3, lits1, coeffs1)).isExo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits1, coeffs2)).isExo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits2, coeffs1)).isExo()).isFalse();

        assertThat(((PBConstraint) f.pbc(CType.LE, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.LT, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GE, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.GT, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) f.pbc(CType.EQ, 4, lits2, coeffs2)).isExo()).isFalse();
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(pb1.numberOfAtoms()).isEqualTo(1);
        assertThat(pb2.numberOfAtoms()).isEqualTo(1);
        assertThat(cc1.numberOfAtoms()).isEqualTo(1);
        assertThat(cc2.numberOfAtoms()).isEqualTo(1);
        assertThat(amo1.numberOfAtoms()).isEqualTo(1);
        assertThat(amo2.numberOfAtoms()).isEqualTo(1);
        assertThat(exo1.numberOfAtoms()).isEqualTo(1);
        assertThat(exo2.numberOfAtoms()).isEqualTo(1);
        assertThat(exo2.numberOfAtoms()).isEqualTo(1);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(pb1.numberOfNodes()).isEqualTo(2);
        assertThat(pb2.numberOfNodes()).isEqualTo(4);
        assertThat(cc1.numberOfNodes()).isEqualTo(2);
        assertThat(cc2.numberOfNodes()).isEqualTo(4);
        assertThat(amo1.numberOfNodes()).isEqualTo(2);
        assertThat(amo2.numberOfNodes()).isEqualTo(4);
        assertThat(exo1.numberOfNodes()).isEqualTo(2);
        assertThat(exo2.numberOfNodes()).isEqualTo(4);
        assertThat(exo2.numberOfNodes()).isEqualTo(4);
    }

    @Test
    public void testVariables() {
        final SortedSet<Variable> lits1 = new TreeSet<>(Collections.singletonList(f.variable("a")));
        final SortedSet<Variable> lits2 = new TreeSet<>(Arrays.asList(f.variable("a"), f.variable("b"), f.variable("c")));
        assertThat(pb1.variables()).isEqualTo(lits1);
        assertThat(pb1.variables()).isEqualTo(lits1);
        assertThat(pb2.variables()).isEqualTo(lits2);
        assertThat(cc1.variables()).isEqualTo(lits1);
        assertThat(cc2.variables()).isEqualTo(lits2);
        assertThat(amo1.variables()).isEqualTo(lits1);
        assertThat(amo2.variables()).isEqualTo(lits2);
        assertThat(exo1.variables()).isEqualTo(lits1);
        assertThat(exo2.variables()).isEqualTo(lits2);
    }

    @Test
    public void testLiterals() {
        final SortedSet<Variable> lits1 = new TreeSet<>(Collections.singletonList(f.variable("a")));
        final SortedSet<Literal> lits2 = new TreeSet<>(Arrays.asList(f.variable("a"), f.literal("b", false), f.variable("c")));
        final SortedSet<Variable> litsCC2 = new TreeSet<>(Arrays.asList(f.variable("a"), f.variable("b"), f.variable("c")));
        assertThat(pb1.literals()).isEqualTo(lits1);
        assertThat(pb2.literals()).isEqualTo(lits2);
        assertThat(cc1.literals()).isEqualTo(lits1);
        assertThat(cc2.literals()).isEqualTo(litsCC2);
        assertThat(amo1.literals()).isEqualTo(lits1);
        assertThat(amo2.literals()).isEqualTo(litsCC2);
        assertThat(exo1.literals()).isEqualTo(lits1);
        assertThat(exo2.literals()).isEqualTo(litsCC2);
    }

    @Test
    public void testContains() {
        assertThat(pb2.containsVariable(f.variable("a"))).isTrue();
        assertThat(pb2.containsVariable(f.variable("b"))).isTrue();
        assertThat(pb2.containsVariable(f.variable("c"))).isTrue();
        assertThat(pb2.containsVariable(f.variable("d"))).isFalse();
        assertThat(pb2.containsVariable(f.variable("x"))).isFalse();
    }

    @Test
    public void testIsNNF() {
        assertThat(pb1.isNNF()).isFalse();
        assertThat(pb2.isNNF()).isFalse();
        assertThat(pb22.isNNF()).isFalse();
    }

    @Test
    public void testIsDNF() {
        assertThat(pb1.isDNF()).isFalse();
        assertThat(pb2.isDNF()).isFalse();
        assertThat(pb22.isDNF()).isFalse();
    }

    @Test
    public void testIsCNF() {
        assertThat(pb1.isCNF()).isFalse();
        assertThat(pb2.isCNF()).isFalse();
        assertThat(pb22.isCNF()).isFalse();
    }

    @Test
    public void testEvaluate() {
        final List<Literal> lits = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final Assignment a1 = new Assignment();
        a1.addLiteral(f.variable("a"));
        a1.addLiteral(f.variable("b"));
        a1.addLiteral(f.literal("c", false));
        final Assignment a2 = new Assignment();
        a2.addLiteral(f.variable("a"));
        a2.addLiteral(f.literal("b", false));
        a2.addLiteral(f.literal("c", false));
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.EQ, 2, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) f.pbc(CType.GE, 1, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) f.pbc(CType.GT, 0, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) f.pbc(CType.LE, 1, lits, coeffs);
        final PBConstraint pb6 = (PBConstraint) f.pbc(CType.LT, 2, lits, coeffs);
        assertThat(pb1.evaluate(a1)).isTrue();
        assertThat(pb1.evaluate(a2)).isFalse();
        assertThat(pb3.evaluate(a1)).isTrue();
        assertThat(pb3.evaluate(a2)).isFalse();
        assertThat(pb4.evaluate(a1)).isTrue();
        assertThat(pb4.evaluate(a2)).isFalse();
        assertThat(pb5.evaluate(a1)).isFalse();
        assertThat(pb5.evaluate(a2)).isTrue();
        assertThat(pb6.evaluate(a1)).isFalse();
        assertThat(pb6.evaluate(a2)).isTrue();
    }

    @Test
    public void testRestrict() {
        final List<Literal> lits = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Literal> litsA1 = Arrays.asList(f.literal("b", false), f.variable("c"));
        final List<Variable> litsA2 = Collections.singletonList(f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final List<Integer> coeffA1 = Arrays.asList(-2, 3);
        final List<Integer> coeffA2 = Collections.singletonList(3);
        final Assignment a1 = new Assignment();
        a1.addLiteral(f.variable("a"));
        final Assignment a2 = new Assignment();
        a2.addLiteral(f.variable("a"));
        a2.addLiteral(f.literal("b", false));
        final Assignment a3 = new Assignment();
        a3.addLiteral(f.variable("a"));
        a3.addLiteral(f.literal("b", false));
        a3.addLiteral(f.variable("c"));
        final Assignment a4 = new Assignment();
        a4.addLiteral(f.literal("a", false));
        a4.addLiteral(f.variable("b"));
        a4.addLiteral(f.literal("c", false));
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.EQ, 2, lits, coeffs);
        assertThat(pb1.restrict(a1)).isEqualTo(f.pbc(CType.EQ, 0, litsA1, coeffA1));
        assertThat(pb1.restrict(a2)).isEqualTo(f.pbc(CType.EQ, 2, litsA2, coeffA2));
        assertThat(pb1.restrict(a3)).isEqualTo(f.falsum());
        assertThat(pb1.restrict(a4)).isEqualTo(f.falsum());
    }

    @Test
    public void testRestrictInequality() {
        final List<Literal> lits = Arrays.asList(f.variable("a"), f.literal("b", false), f.variable("c"), f.variable("d"), f.variable("e"), f.literal("f", false));
        final List<Integer> coeffs = Arrays.asList(75, 50, 201, -3, -24, 1);
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.GE, -24, lits, coeffs);
        final PBConstraint pb2 = (PBConstraint) f.pbc(CType.LE, 150, lits, coeffs);
        final Assignment a1 = new Assignment();
        a1.addLiteral(f.literal("b", false));
        a1.addLiteral(f.variable("c"));
        final Assignment a2 = new Assignment();
        a2.addLiteral(f.literal("a", false));
        a2.addLiteral(f.variable("b"));
        a2.addLiteral(f.literal("c", false));
        a2.addLiteral(f.variable("d"));
        a2.addLiteral(f.variable("e"));
        final Assignment a3 = new Assignment();
        a3.addLiteral(f.literal("c", false));

        assertThat(pb1.restrict(a1)).isEqualTo(f.verum());
        assertThat(pb2.restrict(a1)).isEqualTo(f.falsum());
        assertThat(pb1.restrict(a2)).isEqualTo(f.falsum());
        assertThat(pb2.restrict(a3)).isEqualTo(f.verum());
    }

    @Test
    public void testContainsSubformula() {
        assertThat(pb1.containsNode(f.variable("a"))).isTrue();
        assertThat(pb1.containsNode(f.literal("a", false))).isFalse();
        assertThat(pb2.containsNode(f.literal("b", false))).isTrue();
        assertThat(pb2.containsNode(f.variable("b"))).isTrue();
        assertThat(pb2.containsNode(f.variable("d"))).isFalse();
        assertThat(pb1.containsNode(pb1)).isTrue();
        assertThat(pb2.containsNode(pb2)).isTrue();
        assertThat(pb2.containsNode(pb22)).isTrue();
    }

    @Test
    public void testSubstitute() {
        final List<Literal> lits = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Literal> litsS1 = Arrays.asList(f.literal("b", false), f.variable("c"));
        final List<Variable> litsS2 = Collections.singletonList(f.variable("c"));
        final List<Literal> litsS5 = Arrays.asList(f.variable("a2"), f.literal("b2", false), f.variable("c2"));
        final List<Variable> litsS6 = Arrays.asList(f.variable("a2"), f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final List<Integer> coeffS1 = Arrays.asList(-2, 3);
        final List<Integer> coeffS2 = Collections.singletonList(3);
        final List<Integer> coeffS6 = Arrays.asList(2, 3);
        final Substitution s1 = new Substitution();
        s1.addMapping(f.variable("a"), f.verum());
        final Substitution s2 = new Substitution();
        s2.addMapping(f.variable("a"), f.verum());
        s2.addMapping(f.variable("b"), f.falsum());
        final Substitution s3 = new Substitution();
        s3.addMapping(f.variable("a"), f.verum());
        s3.addMapping(f.variable("b"), f.falsum());
        s3.addMapping(f.variable("c"), f.verum());
        final Substitution s4 = new Substitution();
        s4.addMapping(f.variable("a"), f.falsum());
        s4.addMapping(f.variable("b"), f.verum());
        s4.addMapping(f.variable("c"), f.falsum());
        final Substitution s5 = new Substitution();
        s5.addMapping(f.variable("a"), f.variable("a2"));
        s5.addMapping(f.variable("b"), f.variable("b2"));
        s5.addMapping(f.variable("c"), f.variable("c2"));
        s5.addMapping(f.variable("d"), f.variable("d2"));
        final Substitution s6 = new Substitution();
        s6.addMapping(f.variable("a"), f.variable("a2"));
        s6.addMapping(f.variable("b"), f.falsum());
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.EQ, 2, lits, coeffs);
        assertThat(pb1.substitute(s1)).isEqualTo(f.pbc(CType.EQ, 0, litsS1, coeffS1));
        assertThat(pb1.substitute(s2)).isEqualTo(f.pbc(CType.EQ, 2, litsS2, coeffS2));
        assertThat(pb1.substitute(s3)).isEqualTo(f.falsum());
        assertThat(pb2.substitute(s3)).isEqualTo(f.verum());
        assertThat(pb1.substitute(s4)).isEqualTo(f.falsum());
        assertThat(pb2.substitute(s4)).isEqualTo(f.verum());
        assertThat(pb1.substitute(s5)).isEqualTo(f.pbc(CType.EQ, 2, litsS5, coeffs));
        assertThat(pb1.substitute(s6)).isEqualTo(f.pbc(CType.EQ, 4, litsS6, coeffS6));
    }

    @Test
    public void testNegation() {
        final List<Literal> lits = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.EQ, 2, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) f.pbc(CType.GE, 1, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) f.pbc(CType.GT, 0, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) f.pbc(CType.LE, 1, lits, coeffs);
        final PBConstraint pb6 = (PBConstraint) f.pbc(CType.LT, 2, lits, coeffs);
        final PBConstraint pb7 = (PBConstraint) f.pbc(CType.EQ, -2, lits, coeffs);
        assertThat(pb1.negate()).isEqualTo(f.or(f.pbc(CType.LT, 2, lits, coeffs), f.pbc(CType.GT, 2, lits, coeffs)));
        assertThat(pb3.negate()).isEqualTo(f.pbc(CType.LT, 1, lits, coeffs));
        assertThat(pb4.negate()).isEqualTo(f.pbc(CType.LE, 0, lits, coeffs));
        assertThat(pb5.negate()).isEqualTo(f.pbc(CType.GT, 1, lits, coeffs));
        assertThat(pb6.negate()).isEqualTo(f.pbc(CType.GE, 2, lits, coeffs));
        assertThat(pb7.negate()).isEqualTo(f.or(f.pbc(CType.LT, -2, lits, coeffs), f.pbc(CType.GT, -2, lits, coeffs)));
    }

    @Test
    public void testNNF() {
        assertThat(pb1.nnf()).isEqualTo(f.literal("a", false));
        assertThat(cc1.nnf()).isEqualTo(f.literal("a", false));
        assertThat(amo1.nnf()).isEqualTo(f.verum());
        assertThat(exo1.nnf()).isEqualTo(f.variable("a"));
    }

    @Test
    public void testNormalization() {
        final List<Literal> lits = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"), f.variable("d"),
                f.literal("b", false));
        final List<Integer> coeffs = Arrays.asList(2, -3, 3, 0, 1);
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.EQ, 2, lits, coeffs);
        final PBConstraint pb2 = (PBConstraint) f.pbc(CType.GE, 1, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) f.pbc(CType.GT, 0, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) f.pbc(CType.LE, 1, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) f.pbc(CType.LT, 2, lits, coeffs);
        assertThat(pb1.normalize(f).toString()).isEqualTo("(2*a + 2*b + 3*c <= 4) & (2*~a + 2*~b + 3*~c <= 3)");
        assertThat(pb2.normalize(f).toString()).isEqualTo("2*~a + 2*~b + 3*~c <= 4");
        assertThat(pb3.normalize(f).toString()).isEqualTo("2*~a + 2*~b + 3*~c <= 4");
        assertThat(pb4.normalize(f).toString()).isEqualTo("2*a + 2*b + 3*c <= 3");
        assertThat(pb5.normalize(f).toString()).isEqualTo("2*a + 2*b + 3*c <= 3");
    }

    @Test
    public void testNormalizationTrivial() {
        final List<Literal> lits = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"), f.variable("d"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3, 0);
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.LE, 4, lits, coeffs);
        final PBConstraint pb2 = (PBConstraint) f.pbc(CType.LE, 5, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) f.pbc(CType.LE, 7, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) f.pbc(CType.LE, 10, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) f.pbc(CType.LE, -3, lits, coeffs);
        assertThat(pb1.normalize(f).toString()).isEqualTo("2*a + 2*b + 3*c <= 6");
        assertThat(pb2.normalize(f)).isEqualTo(f.verum());
        assertThat(pb3.normalize(f)).isEqualTo(f.verum());
        assertThat(pb4.normalize(f)).isEqualTo(f.verum());
        assertThat(pb5.normalize(f)).isEqualTo(f.falsum());
    }

    @Test
    public void testNormalizationSimplifications() {
        List<? extends Literal> lits = Arrays.asList(f2.variable("a"), f.variable("a"), f.variable("c"), f.variable("d"));
        List<Integer> coeffs = Arrays.asList(2, -2, 4, 4);
        final PBConstraint pb1 = (PBConstraint) f.pbc(CType.LE, 4, lits, coeffs);
        assertThat(pb1.normalize(f).toString()).isEqualTo("c + d <= 1");
        lits = Arrays.asList(f2.variable("a"), f.literal("a", false), f.variable("c"), f.variable("d"));
        coeffs = Arrays.asList(2, 2, 4, 2);
        final PBConstraint pb2 = (PBConstraint) f.pbc(CType.LE, 4, lits, coeffs);
        assertThat(pb2.normalize(f).toString()).isEqualTo("2*c + d <= 1");
    }

    @Test
    public void testToString() {
        assertThat(pb1.toString()).isEqualTo("3*a <= 2");
        assertThat(pb2.toString()).isEqualTo("3*a + -2*~b + 7*c <= 8");
        assertThat(pb22.toString()).isEqualTo("3*a + -2*~b + 7*c <= 8");
        assertThat(cc1.toString()).isEqualTo("a < 1");
        assertThat(cc2.toString()).isEqualTo("a + b + c >= 2");
        assertThat(amo1.toString()).isEqualTo("a <= 1");
        assertThat(amo2.toString()).isEqualTo("a + b + c <= 1");
        assertThat(exo1.toString()).isEqualTo("a = 1");
        assertThat(exo2.toString()).isEqualTo("a + b + c = 1");
    }

    @Test
    public void testEquals() {
        final List<Literal> lits2 = Arrays.asList(f2.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Integer> coeffs2 = Arrays.asList(3, -2, 7);
        final List<Literal> lits2alt1 = Arrays.asList(f2.variable("a"), f.literal("b", false));
        final List<Integer> coeffs2alt1 = Arrays.asList(3, -2);
        final List<Variable> lits2alt2 = Arrays.asList(f2.variable("a"), f.variable("b"), f.variable("c"));
        final List<Integer> coeffs2alt2 = Arrays.asList(3, -2, 8);
        assertThat(pb1).isEqualTo(pb1);
        assertThat(pb22).isEqualTo(pb2);
        assertThat(f.pbc(CType.LE, 8, lits2, coeffs2)).isEqualTo(pb2);
        assertThat(cc2).isNotEqualTo(cc1);
        assertThat(cc1).isNotEqualTo(null);
        assertThat(cc2).isNotEqualTo("String");
        assertThat("String").isNotEqualTo(cc2);
        assertThat(f.pbc(CType.LE, 8, lits2alt1, coeffs2alt1)).isNotEqualTo(pb2);
        assertThat(f.pbc(CType.LE, 8, lits2alt2, coeffs2)).isNotEqualTo(pb2);
        assertThat(f.pbc(CType.LE, 8, lits2, coeffs2alt2)).isNotEqualTo(pb2);
        assertThat(f.pbc(CType.LT, 8, lits2, coeffs2)).isNotEqualTo(pb2);
        assertThat(f.pbc(CType.LE, 7, lits2, coeffs2)).isNotEqualTo(pb2);
    }

    @Test
    public void testHash() {
        assertThat(pb1.hashCode()).isEqualTo(pb1.hashCode());
        assertThat(pb2.hashCode()).isEqualTo(pb2.hashCode());
        assertThat(pb22.hashCode()).isEqualTo(pb2.hashCode());
    }

    @Test
    public void testNumberOfInternalNodes() {
        assertThat(pb2.numberOfInternalNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(pb1.numberOfOperands()).isEqualTo(0);
        assertThat(pb2.numberOfOperands()).isEqualTo(0);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(pb1.isConstantFormula()).isFalse();
        assertThat(pb2.isConstantFormula()).isFalse();
        assertThat(pb22.isConstantFormula()).isFalse();
        assertThat(cc1.isConstantFormula()).isFalse();
        assertThat(cc2.isConstantFormula()).isFalse();
        assertThat(amo1.isConstantFormula()).isFalse();
        assertThat(amo2.isConstantFormula()).isFalse();
        assertThat(exo1.isConstantFormula()).isFalse();
        assertThat(exo2.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(pb1.isAtomicFormula()).isTrue();
        assertThat(pb2.isAtomicFormula()).isTrue();
        assertThat(pb22.isAtomicFormula()).isTrue();
        assertThat(cc1.isAtomicFormula()).isTrue();
        assertThat(cc2.isAtomicFormula()).isTrue();
        assertThat(amo1.isAtomicFormula()).isTrue();
        assertThat(amo2.isAtomicFormula()).isTrue();
        assertThat(exo1.isAtomicFormula()).isTrue();
        assertThat(exo2.isAtomicFormula()).isTrue();
    }

    @Test
    public void testEvaluateCoeffs() {
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.EQ)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.EQ)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.EQ)).isEqualTo(Tristate.UNDEF);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.EQ)).isEqualTo(Tristate.UNDEF);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.EQ)).isEqualTo(Tristate.UNDEF);

        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.GE)).isEqualTo(Tristate.TRUE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.GE)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.GE)).isEqualTo(Tristate.TRUE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.GE)).isEqualTo(Tristate.UNDEF);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.GE)).isEqualTo(Tristate.UNDEF);

        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.GT)).isEqualTo(Tristate.TRUE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.GT)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.GT)).isEqualTo(Tristate.UNDEF);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.GT)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.GT)).isEqualTo(Tristate.UNDEF);

        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.LE)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.LE)).isEqualTo(Tristate.TRUE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.LE)).isEqualTo(Tristate.UNDEF);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.LE)).isEqualTo(Tristate.TRUE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.LE)).isEqualTo(Tristate.UNDEF);

        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.LT)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.LT)).isEqualTo(Tristate.TRUE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.LT)).isEqualTo(Tristate.FALSE);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.LT)).isEqualTo(Tristate.UNDEF);
        assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.LT)).isEqualTo(Tristate.UNDEF);
    }

    @Test
    public void testTrivialTrue() {
        assertThat(f.pbc(CType.EQ, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.EQ, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.EQ, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());

        assertThat(f.pbc(CType.GT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.GT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.GT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());

        assertThat(f.pbc(CType.GE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.GE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.GE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());

        assertThat(f.pbc(CType.LT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.LT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.LT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());

        assertThat(f.pbc(CType.LE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.LE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.LE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
    }

    @Test
    public void testTrivialFalse() {
        assertThat(f.pbc(CType.EQ, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.EQ, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.EQ, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());

        assertThat(f.pbc(CType.GT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.GT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.GT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());

        assertThat(f.pbc(CType.GE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.GE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.GE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());

        assertThat(f.pbc(CType.LT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
        assertThat(f.pbc(CType.LT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.LT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());

        assertThat(f.pbc(CType.LE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.LE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.verum());
        assertThat(f.pbc(CType.LE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(f.falsum());
    }

    @Test
    public void testSimplifiedToString() {
        assertThat(f.pbc(CType.EQ, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.EQ, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.EQ, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.GT, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.GT, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.GT, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.GE, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.GE, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.GE, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.LT, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.LT, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.LT, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(f.pbc(CType.LE, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.LE, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(f.pbc(CType.LE, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
    }
}
