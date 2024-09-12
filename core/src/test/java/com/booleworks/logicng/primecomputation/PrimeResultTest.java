// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class PrimeResultTest extends TestWithFormulaContext {

    private final FormulaContext c = new FormulaContext(FormulaFactory.nonCaching());
    private final PrimeResult result1;
    private final PrimeResult result2;
    private final PrimeResult result3;

    public PrimeResultTest() {
        final List<SortedSet<Literal>> primeImplicants1 = new ArrayList<>();
        primeImplicants1.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        primeImplicants1.add(new TreeSet<>(Arrays.asList(c.a, c.c)));
        final List<SortedSet<Literal>> primeImplicates1 = new ArrayList<>();
        primeImplicates1.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        result1 = new PrimeResult(primeImplicants1, primeImplicates1, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);

        final List<SortedSet<Literal>> primeImplicants2 = new ArrayList<>();
        primeImplicants2.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        primeImplicants2.add(new TreeSet<>(Collections.singletonList(c.c)));
        final List<SortedSet<Literal>> primeImplicates2 = new ArrayList<>();
        result2 = new PrimeResult(primeImplicants2, primeImplicates2, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);

        final List<SortedSet<Literal>> primeImplicants3 = new ArrayList<>();
        primeImplicants3.add(new TreeSet<>());
        final List<SortedSet<Literal>> primeImplicates3 = new ArrayList<>();
        primeImplicates3.add(new TreeSet<>(Collections.singletonList(c.nb)));
        result3 = new PrimeResult(primeImplicants3, primeImplicates3, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
    }

    @Test
    public void testGetters() {
        assertThat(result1.getPrimeImplicants()).hasSize(2);
        assertThat(result1.getPrimeImplicants().get(0)).containsExactly(c.a, c.nb);
        assertThat(result1.getPrimeImplicants().get(1)).containsExactly(c.a, c.c);
        assertThat(result2.getPrimeImplicants()).hasSize(2);
        assertThat(result2.getPrimeImplicants().get(0)).containsExactly(c.a, c.nb);
        assertThat(result2.getPrimeImplicants().get(1)).containsExactly(c.c);
        assertThat(result3.getPrimeImplicants()).hasSize(1);
        assertThat(result3.getPrimeImplicants().get(0)).isEmpty();

        assertThat(result1.getPrimeImplicates()).hasSize(1);
        assertThat(result1.getPrimeImplicates().get(0)).containsExactly(c.a, c.nb);
        assertThat(result2.getPrimeImplicates()).hasSize(0);
        assertThat(result3.getPrimeImplicates()).hasSize(1);
        assertThat(result3.getPrimeImplicates().get(0)).containsExactly(c.nb);

        assertThat(result1.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(result2.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(result3.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICATES_COMPLETE);
    }

    @Test
    public void testHashCode() {
        assertThat(result1.hashCode()).isEqualTo(result1.hashCode());
        final List<SortedSet<Literal>> primeImplicants = new ArrayList<>();
        primeImplicants.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        primeImplicants.add(new TreeSet<>(Arrays.asList(c.a, c.c)));
        final List<SortedSet<Literal>> primeImplicates = new ArrayList<>();
        primeImplicates.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        final PrimeResult otherResult =
                new PrimeResult(primeImplicants, primeImplicates, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(otherResult.hashCode()).isEqualTo(result1.hashCode());
    }

    @Test
    public void testEquals() {
        assertThat(result1.hashCode()).isEqualTo(result1.hashCode());
        final List<SortedSet<Literal>> primeImplicants = new ArrayList<>();
        primeImplicants.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        primeImplicants.add(new TreeSet<>(Arrays.asList(c.a, c.c)));
        final List<SortedSet<Literal>> primeImplicates = new ArrayList<>();
        primeImplicates.add(new TreeSet<>(Arrays.asList(c.a, c.nb)));
        final PrimeResult otherResult =
                new PrimeResult(primeImplicants, primeImplicates, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(result1.equals(result1)).isTrue();
        assertThat(result1.equals(otherResult)).isTrue();
        assertThat(result1.equals(result2)).isFalse();
        assertThat(result2.equals(result1)).isFalse();
        assertThat(result1.equals(result3)).isFalse();
        assertThat(result1.equals("String")).isFalse();
        assertThat(result1.equals(null)).isFalse();
    }

    @Test
    public void testToString() {
        assertThat(result1.toString()).isEqualTo(
                "PrimeResult{" +
                        "primeImplicants=[[a, ~b], [a, c]]" +
                        ", primeImplicates=[[a, ~b]]" +
                        ", coverageInfo=IMPLICANTS_COMPLETE" +
                        '}');
    }
}
