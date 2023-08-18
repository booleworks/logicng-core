// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Assignment;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;

public class RestrictionTest extends TestWithExampleFormulas {

    private final Assignment ass = new Assignment(Arrays.asList(A, NB, NX));

    @Test
    public void testConstantRestrict() {
        assertThat(TRUE.restrict(ass)).isEqualTo(TRUE);
        assertThat(FALSE.restrict(ass)).isEqualTo(FALSE);
    }

    @Test
    public void testLiteralRestrict() {
        assertThat(A.restrict(ass)).isEqualTo(TRUE);
        assertThat(NA.restrict(ass)).isEqualTo(FALSE);
        assertThat(X.restrict(ass)).isEqualTo(FALSE);
        assertThat(NX.restrict(ass)).isEqualTo(TRUE);
        assertThat(C.restrict(ass)).isEqualTo(C);
        assertThat(NY.restrict(ass)).isEqualTo(NY);
    }

    @Test
    public void testNotRestrict() {
        assertThat(NOT1.restrict(ass)).isEqualTo(TRUE);
        assertThat(NOT2.restrict(ass)).isEqualTo(NY);
    }

    @Test
    public void testBinaryRestrict() {
        assertThat(IMP1.restrict(ass)).isEqualTo(FALSE);
        assertThat(IMP2.restrict(ass)).isEqualTo(TRUE);
        assertThat(f.implication(NA, C).restrict(ass)).isEqualTo(TRUE);
        assertThat(IMP3.restrict(ass)).isEqualTo(TRUE);
        assertThat(f.implication(A, C).restrict(ass)).isEqualTo(C);

        assertThat(EQ1.restrict(ass)).isEqualTo(FALSE);
        assertThat(EQ2.restrict(ass)).isEqualTo(FALSE);
        assertThat(EQ3.restrict(ass)).isEqualTo(NY);
        assertThat(EQ4.restrict(ass)).isEqualTo(FALSE);
    }

    @Test
    public void testNAryRestrict() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(OR1.restrict(ass)).isEqualTo(Y);
        assertThat(OR2.restrict(ass)).isEqualTo(TRUE);
        assertThat(OR3.restrict(ass)).isEqualTo(FALSE);
        assertThat(p.parse("~a | b | ~c | x | y").restrict(ass)).isEqualTo(p.parse("~c | y"));
        assertThat(p.parse("~a | b | ~c | ~x | ~y").restrict(ass)).isEqualTo(TRUE);

        assertThat(AND1.restrict(ass)).isEqualTo(FALSE);
        assertThat(AND2.restrict(ass)).isEqualTo(FALSE);
        assertThat(AND3.restrict(ass)).isEqualTo(Y);
        assertThat(p.parse("a & ~b & c & ~x & ~y").restrict(ass)).isEqualTo(p.parse("c & ~y"));
        assertThat(p.parse("a & b & c & ~x & y").restrict(ass)).isEqualTo(FALSE);
    }
}
