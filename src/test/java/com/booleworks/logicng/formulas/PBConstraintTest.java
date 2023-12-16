// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;
import com.booleworks.logicng.datastructures.Tristate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class PBConstraintTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIllegalPB(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(3, -2, 7, 2);
        assertThatThrownBy(() -> _c.f.pbc(CType.EQ, 3, lits, coeffs)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.pb1.type()).isEqualTo(FType.PBC);
        assertThat(_c.pb2.type()).isEqualTo(FType.PBC);
        assertThat(_c.cc1.type()).isEqualTo(FType.PBC);
        assertThat(_c.cc2.type()).isEqualTo(FType.PBC);
        assertThat(_c.amo1.type()).isEqualTo(FType.PBC);
        assertThat(_c.amo2.type()).isEqualTo(FType.PBC);
        assertThat(_c.exo1.type()).isEqualTo(FType.PBC);
        assertThat(_c.exo2.type()).isEqualTo(FType.PBC);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetters(final FormulaContext _c) {
        final Literal[] lits1 = new Literal[]{_c.f.variable("a")};
        final Literal[] lits2 = new Literal[]{_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c")};
        final Literal[] litsCC2 = new Literal[]{_c.f.variable("a"), _c.f.variable("b"), _c.f.variable("c")};
        final Integer[] coeffs1 = new Integer[]{3};
        final Integer[] coeffs2 = new Integer[]{3, -2, 7};

        final Integer[] coeffsCC1 = new Integer[]{1};
        final Integer[] coeffsCC2 = new Integer[]{1, 1, 1};

        assertThat(_c.pb1.operands()).containsExactly(lits1);
        assertThat(_c.pb1.coefficients()).containsExactly(coeffs1);
        assertThat(_c.pb1.comparator()).isEqualTo(CType.LE);
        assertThat(_c.pb1.rhs()).isEqualTo(2);
        assertThat(_c.pb1.isCC()).isFalse();
        assertThat(_c.pb1.isAmo()).isFalse();
        assertThat(_c.pb1.isExo()).isFalse();
        assertThat(_c.pb1.maxWeight()).isEqualTo(3);

        assertThat(_c.pb2.operands()).containsExactly(lits2);
        assertThat(_c.pb2.coefficients()).containsExactly(coeffs2);
        assertThat(_c.pb2.comparator()).isEqualTo(CType.LE);
        assertThat(_c.pb2.rhs()).isEqualTo(8);
        assertThat(_c.pb2.isCC()).isFalse();
        assertThat(_c.pb2.isAmo()).isFalse();
        assertThat(_c.pb2.isExo()).isFalse();
        assertThat(_c.pb2.maxWeight()).isEqualTo(7);

        assertThat(_c.cc1.operands()).containsExactly(lits1);
        assertThat(_c.cc1.coefficients()).containsExactly(coeffsCC1);
        assertThat(_c.cc1.comparator()).isEqualTo(CType.LT);
        assertThat(_c.cc1.rhs()).isEqualTo(1);
        assertThat(_c.cc1.isCC()).isTrue();
        assertThat(_c.cc1.isAmo()).isFalse();
        assertThat(_c.cc1.isExo()).isFalse();
        assertThat(_c.cc1.maxWeight()).isEqualTo(1);

        assertThat(_c.cc2.operands()).containsExactly(litsCC2);
        assertThat(_c.cc2.coefficients()).containsExactly(coeffsCC2);
        assertThat(_c.cc2.comparator()).isEqualTo(CType.GE);
        assertThat(_c.cc2.rhs()).isEqualTo(2);
        assertThat(_c.cc2.isCC()).isTrue();
        assertThat(_c.cc2.isAmo()).isFalse();
        assertThat(_c.cc2.isExo()).isFalse();
        assertThat(_c.cc2.maxWeight()).isEqualTo(1);

        assertThat(_c.amo1.operands()).containsExactly(lits1);
        assertThat(_c.amo1.coefficients()).containsExactly(coeffsCC1);
        assertThat(_c.amo1.comparator()).isEqualTo(CType.LE);
        assertThat(_c.amo1.rhs()).isEqualTo(1);
        assertThat(_c.amo1.isCC()).isTrue();
        assertThat(_c.amo1.isAmo()).isTrue();
        assertThat(_c.amo1.isExo()).isFalse();
        assertThat(_c.amo1.maxWeight()).isEqualTo(1);

        assertThat(_c.amo2.operands()).containsExactly(litsCC2);
        assertThat(_c.amo2.coefficients()).containsExactly(coeffsCC2);
        assertThat(_c.amo2.comparator()).isEqualTo(CType.LE);
        assertThat(_c.amo2.rhs()).isEqualTo(1);
        assertThat(_c.amo2.isCC()).isTrue();
        assertThat(_c.amo2.isAmo()).isTrue();
        assertThat(_c.amo2.isExo()).isFalse();
        assertThat(_c.amo2.maxWeight()).isEqualTo(1);

        assertThat(_c.exo1.operands()).containsExactly(lits1);
        assertThat(_c.exo1.coefficients()).containsExactly(coeffsCC1);
        assertThat(_c.exo1.comparator()).isEqualTo(CType.EQ);
        assertThat(_c.exo1.rhs()).isEqualTo(1);
        assertThat(_c.exo1.isCC()).isTrue();
        assertThat(_c.exo1.isAmo()).isFalse();
        assertThat(_c.exo1.isExo()).isTrue();
        assertThat(_c.exo1.maxWeight()).isEqualTo(1);

        assertThat(_c.exo2.operands()).containsExactly(litsCC2);
        assertThat(_c.exo2.coefficients()).containsExactly(coeffsCC2);
        assertThat(_c.exo2.comparator()).isEqualTo(CType.EQ);
        assertThat(_c.exo2.rhs()).isEqualTo(1);
        assertThat(_c.exo2.isCC()).isTrue();
        assertThat(_c.exo2.isAmo()).isFalse();
        assertThat(_c.exo2.isExo()).isTrue();
        assertThat(_c.exo2.maxWeight()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCC(final FormulaContext _c) {
        final Literal[] lits1 = new Literal[]{_c.f.variable("a"), _c.f.variable("b")};
        final Literal[] lits2 = new Literal[]{_c.f.variable("a"), _c.f.literal("b", false)};
        final int[] coeffs1 = new int[]{1, 1};
        final int[] coeffs2 = new int[]{-1, 1};

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits1, coeffs1)).isCC()).isTrue();

        // Corner cases
        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 0, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 1, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 0, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, -1, lits1, coeffs1)).isCC()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 0, lits1, coeffs1)).isCC()).isTrue();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, -1, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 0, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, -1, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, -2, lits1, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, -1, lits1, coeffs1)).isCC()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits1, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits1, coeffs2)).isCC()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits2, coeffs1)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits2, coeffs1)).isCC()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits2, coeffs2)).isCC()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits2, coeffs2)).isCC()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsAmo(final FormulaContext _c) {
        final Literal[] lits1 = new Literal[]{_c.f.variable("a"), _c.f.variable("b")};
        final Literal[] lits2 = new Literal[]{_c.f.variable("a"), _c.f.literal("b", false)};
        final int[] coeffs1 = new int[]{1, 1};
        final int[] coeffs2 = new int[]{-1, 1};

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 1, lits1, coeffs1)).isAmo()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 2, lits1, coeffs1)).isAmo()).isTrue();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 1, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 1, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 1, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 1, lits1, coeffs1)).isAmo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 3, lits1, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 3, lits1, coeffs1)).isAmo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits1, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits1, coeffs2)).isAmo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits2, coeffs1)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits2, coeffs1)).isAmo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits2, coeffs2)).isAmo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits2, coeffs2)).isAmo()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsExo(final FormulaContext _c) {
        final Literal[] lits1 = new Literal[]{_c.f.variable("a"), _c.f.variable("b")};
        final Literal[] lits2 = new Literal[]{_c.f.variable("a"), _c.f.literal("b", false)};
        final int[] coeffs1 = new int[]{1, 1};
        final int[] coeffs2 = new int[]{-1, 1};

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 2, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 1, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 1, lits1, coeffs1)).isExo()).isTrue();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 3, lits1, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 3, lits1, coeffs1)).isExo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits1, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits1, coeffs2)).isExo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits2, coeffs1)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits2, coeffs1)).isExo()).isFalse();

        assertThat(((PBConstraint) _c.f.pbc(CType.LE, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.LT, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GE, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.GT, 4, lits2, coeffs2)).isExo()).isFalse();
        assertThat(((PBConstraint) _c.f.pbc(CType.EQ, 4, lits2, coeffs2)).isExo()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.pb1.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.pb2.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.cc1.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.cc2.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.amo1.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.amo2.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.exo1.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.exo2.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.exo2.numberOfAtoms(_c.f)).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.pb1.numberOfNodes(_c.f)).isEqualTo(2);
        assertThat(_c.pb2.numberOfNodes(_c.f)).isEqualTo(4);
        assertThat(_c.cc1.numberOfNodes(_c.f)).isEqualTo(2);
        assertThat(_c.cc2.numberOfNodes(_c.f)).isEqualTo(4);
        assertThat(_c.amo1.numberOfNodes(_c.f)).isEqualTo(2);
        assertThat(_c.amo2.numberOfNodes(_c.f)).isEqualTo(4);
        assertThat(_c.exo1.numberOfNodes(_c.f)).isEqualTo(2);
        assertThat(_c.exo2.numberOfNodes(_c.f)).isEqualTo(4);
        assertThat(_c.exo2.numberOfNodes(_c.f)).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        final SortedSet<Variable> lits1 = new TreeSet<>(Collections.singletonList(_c.f.variable("a")));
        final SortedSet<Variable> lits2 = new TreeSet<>(Arrays.asList(_c.f.variable("a"), _c.f.variable("b"), _c.f.variable("c")));
        assertThat(_c.pb1.variables(_c.f)).isEqualTo(lits1);
        assertThat(_c.pb1.variables(_c.f)).isEqualTo(lits1);
        assertThat(_c.pb2.variables(_c.f)).isEqualTo(lits2);
        assertThat(_c.cc1.variables(_c.f)).isEqualTo(lits1);
        assertThat(_c.cc2.variables(_c.f)).isEqualTo(lits2);
        assertThat(_c.amo1.variables(_c.f)).isEqualTo(lits1);
        assertThat(_c.amo2.variables(_c.f)).isEqualTo(lits2);
        assertThat(_c.exo1.variables(_c.f)).isEqualTo(lits1);
        assertThat(_c.exo2.variables(_c.f)).isEqualTo(lits2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final SortedSet<Variable> lits1 = new TreeSet<>(Collections.singletonList(_c.f.variable("a")));
        final SortedSet<Literal> lits2 = new TreeSet<>(Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c")));
        final SortedSet<Variable> litsCC2 = new TreeSet<>(Arrays.asList(_c.f.variable("a"), _c.f.variable("b"), _c.f.variable("c")));
        assertThat(_c.pb1.literals(_c.f)).isEqualTo(lits1);
        assertThat(_c.pb2.literals(_c.f)).isEqualTo(lits2);
        assertThat(_c.cc1.literals(_c.f)).isEqualTo(lits1);
        assertThat(_c.cc2.literals(_c.f)).isEqualTo(litsCC2);
        assertThat(_c.amo1.literals(_c.f)).isEqualTo(lits1);
        assertThat(_c.amo2.literals(_c.f)).isEqualTo(litsCC2);
        assertThat(_c.exo1.literals(_c.f)).isEqualTo(lits1);
        assertThat(_c.exo2.literals(_c.f)).isEqualTo(litsCC2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.pb2.containsVariable(_c.f.variable("a"))).isTrue();
        assertThat(_c.pb2.containsVariable(_c.f.variable("b"))).isTrue();
        assertThat(_c.pb2.containsVariable(_c.f.variable("c"))).isTrue();
        assertThat(_c.pb2.containsVariable(_c.f.variable("d"))).isFalse();
        assertThat(_c.pb2.containsVariable(_c.f.variable("x"))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.pb1.isNNF(_c.f)).isFalse();
        assertThat(_c.pb2.isNNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.pb1.isDNF(_c.f)).isFalse();
        assertThat(_c.pb2.isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.pb1.isCNF(_c.f)).isFalse();
        assertThat(_c.pb2.isCNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEvaluate(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final Assignment a1 = new Assignment();
        a1.addLiteral(_c.f.variable("a"));
        a1.addLiteral(_c.f.variable("b"));
        a1.addLiteral(_c.f.literal("c", false));
        final Assignment a2 = new Assignment();
        a2.addLiteral(_c.f.variable("a"));
        a2.addLiteral(_c.f.literal("b", false));
        a2.addLiteral(_c.f.literal("c", false));
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.EQ, 2, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) _c.f.pbc(CType.GE, 1, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) _c.f.pbc(CType.GT, 0, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) _c.f.pbc(CType.LE, 1, lits, coeffs);
        final PBConstraint pb6 = (PBConstraint) _c.f.pbc(CType.LT, 2, lits, coeffs);
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

    @ParameterizedTest
    @MethodSource("contexts")
    public void testRestrict(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Literal> litsA1 = Arrays.asList(_c.f.literal("b", false), _c.f.variable("c"));
        final List<Variable> litsA2 = Collections.singletonList(_c.f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final List<Integer> coeffA1 = Arrays.asList(-2, 3);
        final List<Integer> coeffA2 = Collections.singletonList(3);
        final Assignment a1 = new Assignment();
        a1.addLiteral(_c.f.variable("a"));
        final Assignment a2 = new Assignment();
        a2.addLiteral(_c.f.variable("a"));
        a2.addLiteral(_c.f.literal("b", false));
        final Assignment a3 = new Assignment();
        a3.addLiteral(_c.f.variable("a"));
        a3.addLiteral(_c.f.literal("b", false));
        a3.addLiteral(_c.f.variable("c"));
        final Assignment a4 = new Assignment();
        a4.addLiteral(_c.f.literal("a", false));
        a4.addLiteral(_c.f.variable("b"));
        a4.addLiteral(_c.f.literal("c", false));
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.EQ, 2, lits, coeffs);
        assertThat(pb1.restrict(_c.f, a1)).isEqualTo(_c.f.pbc(CType.EQ, 0, litsA1, coeffA1));
        assertThat(pb1.restrict(_c.f, a2)).isEqualTo(_c.f.pbc(CType.EQ, 2, litsA2, coeffA2));
        assertThat(pb1.restrict(_c.f, a3)).isEqualTo(_c.f.falsum());
        assertThat(pb1.restrict(_c.f, a4)).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testRestrictInequality(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"), _c.f.variable("d"), _c.f.variable("e"),
                _c.f.literal("f", false));
        final List<Integer> coeffs = Arrays.asList(75, 50, 201, -3, -24, 1);
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.GE, -24, lits, coeffs);
        final PBConstraint pb2 = (PBConstraint) _c.f.pbc(CType.LE, 150, lits, coeffs);
        final Assignment a1 = new Assignment();
        a1.addLiteral(_c.f.literal("b", false));
        a1.addLiteral(_c.f.variable("c"));
        final Assignment a2 = new Assignment();
        a2.addLiteral(_c.f.literal("a", false));
        a2.addLiteral(_c.f.variable("b"));
        a2.addLiteral(_c.f.literal("c", false));
        a2.addLiteral(_c.f.variable("d"));
        a2.addLiteral(_c.f.variable("e"));
        final Assignment a3 = new Assignment();
        a3.addLiteral(_c.f.literal("c", false));

        assertThat(pb1.restrict(_c.f, a1)).isEqualTo(_c.f.verum());
        assertThat(pb2.restrict(_c.f, a1)).isEqualTo(_c.f.falsum());
        assertThat(pb1.restrict(_c.f, a2)).isEqualTo(_c.f.falsum());
        assertThat(pb2.restrict(_c.f, a3)).isEqualTo(_c.f.verum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContainsSubformula(final FormulaContext _c) {
        assertThat(_c.pb1.containsNode(_c.f.variable("a"))).isTrue();
        assertThat(_c.pb1.containsNode(_c.f.literal("a", false))).isFalse();
        assertThat(_c.pb2.containsNode(_c.f.literal("b", false))).isTrue();
        assertThat(_c.pb2.containsNode(_c.f.variable("b"))).isTrue();
        assertThat(_c.pb2.containsNode(_c.f.variable("d"))).isFalse();
        assertThat(_c.pb1.containsNode(_c.pb1)).isTrue();
        assertThat(_c.pb2.containsNode(_c.pb2)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSubstitute(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Literal> litsS1 = Arrays.asList(_c.f.literal("b", false), _c.f.variable("c"));
        final List<Variable> litsS2 = Collections.singletonList(_c.f.variable("c"));
        final List<Literal> litsS5 = Arrays.asList(_c.f.variable("a2"), _c.f.literal("b2", false), _c.f.variable("c2"));
        final List<Variable> litsS6 = Arrays.asList(_c.f.variable("a2"), _c.f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final List<Integer> coeffS1 = Arrays.asList(-2, 3);
        final List<Integer> coeffS2 = Collections.singletonList(3);
        final List<Integer> coeffS6 = Arrays.asList(2, 3);
        final Substitution s1 = new Substitution();
        s1.addMapping(_c.f.variable("a"), _c.f.verum());
        final Substitution s2 = new Substitution();
        s2.addMapping(_c.f.variable("a"), _c.f.verum());
        s2.addMapping(_c.f.variable("b"), _c.f.falsum());
        final Substitution s3 = new Substitution();
        s3.addMapping(_c.f.variable("a"), _c.f.verum());
        s3.addMapping(_c.f.variable("b"), _c.f.falsum());
        s3.addMapping(_c.f.variable("c"), _c.f.verum());
        final Substitution s4 = new Substitution();
        s4.addMapping(_c.f.variable("a"), _c.f.falsum());
        s4.addMapping(_c.f.variable("b"), _c.f.verum());
        s4.addMapping(_c.f.variable("c"), _c.f.falsum());
        final Substitution s5 = new Substitution();
        s5.addMapping(_c.f.variable("a"), _c.f.variable("a2"));
        s5.addMapping(_c.f.variable("b"), _c.f.variable("b2"));
        s5.addMapping(_c.f.variable("c"), _c.f.variable("c2"));
        s5.addMapping(_c.f.variable("d"), _c.f.variable("d2"));
        final Substitution s6 = new Substitution();
        s6.addMapping(_c.f.variable("a"), _c.f.variable("a2"));
        s6.addMapping(_c.f.variable("b"), _c.f.falsum());
        final PBConstraint pb = (PBConstraint) _c.f.pbc(CType.EQ, 2, lits, coeffs);
        assertThat(pb.substitute(_c.f, s1)).isEqualTo(_c.f.pbc(CType.EQ, 0, litsS1, coeffS1));
        assertThat(pb.substitute(_c.f, s2)).isEqualTo(_c.f.pbc(CType.EQ, 2, litsS2, coeffS2));
        assertThat(pb.substitute(_c.f, s3)).isEqualTo(_c.f.falsum());
        assertThat(_c.pb2.substitute(_c.f, s3)).isEqualTo(_c.f.verum());
        assertThat(pb.substitute(_c.f, s4)).isEqualTo(_c.f.falsum());
        assertThat(_c.pb2.substitute(_c.f, s4)).isEqualTo(_c.f.verum());
        assertThat(pb.substitute(_c.f, s5)).isEqualTo(_c.f.pbc(CType.EQ, 2, litsS5, coeffs));
        assertThat(pb.substitute(_c.f, s6)).isEqualTo(_c.f.pbc(CType.EQ, 4, litsS6, coeffS6));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegation(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3);
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.EQ, 2, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) _c.f.pbc(CType.GE, 1, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) _c.f.pbc(CType.GT, 0, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) _c.f.pbc(CType.LE, 1, lits, coeffs);
        final PBConstraint pb6 = (PBConstraint) _c.f.pbc(CType.LT, 2, lits, coeffs);
        final PBConstraint pb7 = (PBConstraint) _c.f.pbc(CType.EQ, -2, lits, coeffs);
        assertThat(pb1.negate(_c.f)).isEqualTo(_c.f.or(_c.f.pbc(CType.LT, 2, lits, coeffs), _c.f.pbc(CType.GT, 2, lits, coeffs)));
        assertThat(pb3.negate(_c.f)).isEqualTo(_c.f.pbc(CType.LT, 1, lits, coeffs));
        assertThat(pb4.negate(_c.f)).isEqualTo(_c.f.pbc(CType.LE, 0, lits, coeffs));
        assertThat(pb5.negate(_c.f)).isEqualTo(_c.f.pbc(CType.GT, 1, lits, coeffs));
        assertThat(pb6.negate(_c.f)).isEqualTo(_c.f.pbc(CType.GE, 2, lits, coeffs));
        assertThat(pb7.negate(_c.f)).isEqualTo(_c.f.or(_c.f.pbc(CType.LT, -2, lits, coeffs), _c.f.pbc(CType.GT, -2, lits, coeffs)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNNF(final FormulaContext _c) {
        assertThat(_c.pb1.nnf(_c.f)).isEqualTo(_c.f.literal("a", false));
        assertThat(_c.cc1.nnf(_c.f)).isEqualTo(_c.f.literal("a", false));
        assertThat(_c.amo1.nnf(_c.f)).isEqualTo(_c.f.verum());
        assertThat(_c.exo1.nnf(_c.f)).isEqualTo(_c.f.variable("a"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNormalization(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"), _c.f.variable("d"),
                _c.f.literal("b", false));
        final List<Integer> coeffs = Arrays.asList(2, -3, 3, 0, 1);
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.EQ, 2, lits, coeffs);
        final PBConstraint pb2 = (PBConstraint) _c.f.pbc(CType.GE, 1, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) _c.f.pbc(CType.GT, 0, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) _c.f.pbc(CType.LE, 1, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) _c.f.pbc(CType.LT, 2, lits, coeffs);
        assertThat(pb1.normalize(_c.f).toString()).isEqualTo("(2*a + 2*b + 3*c <= 4) & (2*~a + 2*~b + 3*~c <= 3)");
        assertThat(pb2.normalize(_c.f).toString()).isEqualTo("2*~a + 2*~b + 3*~c <= 4");
        assertThat(pb3.normalize(_c.f).toString()).isEqualTo("2*~a + 2*~b + 3*~c <= 4");
        assertThat(pb4.normalize(_c.f).toString()).isEqualTo("2*a + 2*b + 3*c <= 3");
        assertThat(pb5.normalize(_c.f).toString()).isEqualTo("2*a + 2*b + 3*c <= 3");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNormalizationTrivial(final FormulaContext _c) {
        final List<Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"), _c.f.variable("d"));
        final List<Integer> coeffs = Arrays.asList(2, -2, 3, 0);
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.LE, 4, lits, coeffs);
        final PBConstraint pb2 = (PBConstraint) _c.f.pbc(CType.LE, 5, lits, coeffs);
        final PBConstraint pb3 = (PBConstraint) _c.f.pbc(CType.LE, 7, lits, coeffs);
        final PBConstraint pb4 = (PBConstraint) _c.f.pbc(CType.LE, 10, lits, coeffs);
        final PBConstraint pb5 = (PBConstraint) _c.f.pbc(CType.LE, -3, lits, coeffs);
        assertThat(pb1.normalize(_c.f).toString()).isEqualTo("2*a + 2*b + 3*c <= 6");
        assertThat(pb2.normalize(_c.f)).isEqualTo(_c.f.verum());
        assertThat(pb3.normalize(_c.f)).isEqualTo(_c.f.verum());
        assertThat(pb4.normalize(_c.f)).isEqualTo(_c.f.verum());
        assertThat(pb5.normalize(_c.f)).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNormalizationSimplifications(final FormulaContext _c) {
        List<? extends Literal> lits = Arrays.asList(_c.f.variable("a"), _c.f.variable("a"), _c.f.variable("c"), _c.f.variable("d"));
        List<Integer> coeffs = Arrays.asList(2, -2, 4, 4);
        final PBConstraint pb1 = (PBConstraint) _c.f.pbc(CType.LE, 4, lits, coeffs);
        assertThat(pb1.normalize(_c.f).toString()).isEqualTo("c + d <= 1");
        lits = Arrays.asList(_c.f.variable("a"), _c.f.literal("a", false), _c.f.variable("c"), _c.f.variable("d"));
        coeffs = Arrays.asList(2, 2, 4, 2);
        final PBConstraint pb2 = (PBConstraint) _c.f.pbc(CType.LE, 4, lits, coeffs);
        assertThat(pb2.normalize(_c.f).toString()).isEqualTo("2*c + d <= 1");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.pb1.toString()).isEqualTo("3*a <= 2");
        assertThat(_c.pb2.toString()).isEqualTo("3*a + -2*~b + 7*c <= 8");
        assertThat(_c.cc1.toString()).isEqualTo("a < 1");
        assertThat(_c.cc2.toString()).isEqualTo("a + b + c >= 2");
        assertThat(_c.amo1.toString()).isEqualTo("a <= 1");
        assertThat(_c.amo2.toString()).isEqualTo("a + b + c <= 1");
        assertThat(_c.exo1.toString()).isEqualTo("a = 1");
        assertThat(_c.exo2.toString()).isEqualTo("a + b + c = 1");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        final List<Literal> lits2 = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Integer> coeffs2 = Arrays.asList(3, -2, 7);
        final List<Literal> lits2alt1 = Arrays.asList(_c.f.variable("a"), _c.f.literal("b", false));
        final List<Integer> coeffs2alt1 = Arrays.asList(3, -2);
        final List<Variable> lits2alt2 = Arrays.asList(_c.f.variable("a"), _c.f.variable("b"), _c.f.variable("c"));
        final List<Integer> coeffs2alt2 = Arrays.asList(3, -2, 8);
        assertThat(_c.pb1).isEqualTo(_c.pb1);
        assertThat(_c.f.pbc(CType.LE, 8, lits2, coeffs2)).isEqualTo(_c.pb2);
        assertThat(_c.cc2).isNotEqualTo(_c.cc1);
        assertThat(_c.cc1).isNotEqualTo(null);
        assertThat(_c.cc2).isNotEqualTo("String");
        assertThat("String").isNotEqualTo(_c.cc2);
        assertThat(_c.f.pbc(CType.LE, 8, lits2alt1, coeffs2alt1)).isNotEqualTo(_c.pb2);
        assertThat(_c.f.pbc(CType.LE, 8, lits2alt2, coeffs2)).isNotEqualTo(_c.pb2);
        assertThat(_c.f.pbc(CType.LE, 8, lits2, coeffs2alt2)).isNotEqualTo(_c.pb2);
        assertThat(_c.f.pbc(CType.LT, 8, lits2, coeffs2)).isNotEqualTo(_c.pb2);
        assertThat(_c.f.pbc(CType.LE, 7, lits2, coeffs2)).isNotEqualTo(_c.pb2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        assertThat(_c.pb1.hashCode()).isEqualTo(_c.pb1.hashCode());
        assertThat(_c.pb2.hashCode()).isEqualTo(_c.pb2.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) {
        assertThat(_c.pb2.numberOfInternalNodes()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.pb1.numberOfOperands()).isEqualTo(0);
        assertThat(_c.pb2.numberOfOperands()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.pb1.isConstantFormula()).isFalse();
        assertThat(_c.pb2.isConstantFormula()).isFalse();
        assertThat(_c.cc1.isConstantFormula()).isFalse();
        assertThat(_c.cc2.isConstantFormula()).isFalse();
        assertThat(_c.amo1.isConstantFormula()).isFalse();
        assertThat(_c.amo2.isConstantFormula()).isFalse();
        assertThat(_c.exo1.isConstantFormula()).isFalse();
        assertThat(_c.exo2.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.pb1.isAtomicFormula()).isTrue();
        assertThat(_c.pb2.isAtomicFormula()).isTrue();
        assertThat(_c.cc1.isAtomicFormula()).isTrue();
        assertThat(_c.cc2.isAtomicFormula()).isTrue();
        assertThat(_c.amo1.isAtomicFormula()).isTrue();
        assertThat(_c.amo2.isAtomicFormula()).isTrue();
        assertThat(_c.exo1.isAtomicFormula()).isTrue();
        assertThat(_c.exo2.isAtomicFormula()).isTrue();
    }

    @Test
    public void testEvaluateCoeffs() {
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.EQ)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.EQ)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.EQ)).isEqualTo(Tristate.UNDEF);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.EQ)).isEqualTo(Tristate.UNDEF);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.EQ)).isEqualTo(Tristate.UNDEF);

        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.GE)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.GE)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.GE)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.GE)).isEqualTo(Tristate.UNDEF);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.GE)).isEqualTo(Tristate.UNDEF);

        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.GT)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.GT)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.GT)).isEqualTo(Tristate.UNDEF);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.GT)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.GT)).isEqualTo(Tristate.UNDEF);

        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.LE)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.LE)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.LE)).isEqualTo(Tristate.UNDEF);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.LE)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.LE)).isEqualTo(Tristate.UNDEF);

        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -3, CType.LT)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 3, CType.LT)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, -2, CType.LT)).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 2, CType.LT)).isEqualTo(Tristate.UNDEF);
        Assertions.assertThat(PBConstraint.evaluateCoeffs(-2, 2, 0, CType.LT)).isEqualTo(Tristate.UNDEF);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrivialTrue(final FormulaContext _c) {
        assertThat(_c.f.pbc(CType.EQ, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.EQ, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.EQ, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());

        assertThat(_c.f.pbc(CType.GT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.GT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.GT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());

        assertThat(_c.f.pbc(CType.GE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.GE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.GE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());

        assertThat(_c.f.pbc(CType.LT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.LT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.LT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());

        assertThat(_c.f.pbc(CType.LE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.LE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.LE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrivialFalse(final FormulaContext _c) {
        assertThat(_c.f.pbc(CType.EQ, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.EQ, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.EQ, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());

        assertThat(_c.f.pbc(CType.GT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.GT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.GT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());

        assertThat(_c.f.pbc(CType.GE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.GE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.GE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());

        assertThat(_c.f.pbc(CType.LT, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
        assertThat(_c.f.pbc(CType.LT, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.LT, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());

        assertThat(_c.f.pbc(CType.LE, 0, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.LE, 1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.verum());
        assertThat(_c.f.pbc(CType.LE, -1, new ArrayList<>(), new ArrayList<>())).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimplifiedToString(final FormulaContext _c) {
        assertThat(_c.f.pbc(CType.EQ, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.EQ, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.EQ, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.GT, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.GT, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.GT, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.GE, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.GE, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.GE, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.LT, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.LT, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.LT, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
        assertThat(_c.f.pbc(CType.LE, 0, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.LE, 1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$true");
        assertThat(_c.f.pbc(CType.LE, -1, new ArrayList<>(), new ArrayList<>()).toString()).isEqualTo("$false");
    }
}
