// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.testutils.TestUtil.equivalentModels;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.parsers.PseudoBooleanParser;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

public class BDDCNFTest extends TestWithExampleFormulas {

    private final BDDCNFTransformation bddcnf = new BDDCNFTransformation(f);

    @Test
    public void testConstants() {
        assertThat(TRUE.transform(bddcnf)).isEqualTo(TRUE);
        assertThat(FALSE.transform(bddcnf)).isEqualTo(FALSE);
    }

    @Test
    public void testLiterals() {
        assertThat(A.transform(bddcnf)).isEqualTo(A);
        assertThat(NA.transform(bddcnf)).isEqualTo(NA);
    }

    @Test
    public void testBinaryOperators() {
        assertThat(IMP1.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(IMP1, IMP1.transform(bddcnf), IMP1.variables())).isTrue();
        assertThat(IMP2.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(IMP2, IMP2.transform(bddcnf), IMP2.variables())).isTrue();
        assertThat(IMP3.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(IMP3, IMP3.transform(bddcnf), IMP3.variables())).isTrue();
        assertThat(EQ1.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(EQ1, EQ1.transform(bddcnf), EQ1.variables())).isTrue();
        assertThat(EQ2.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(EQ2, EQ2.transform(bddcnf), EQ2.variables())).isTrue();
        assertThat(EQ3.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(EQ3, EQ3.transform(bddcnf), EQ3.variables())).isTrue();
        assertThat(EQ4.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(EQ4, EQ4.transform(bddcnf), EQ4.variables())).isTrue();
    }

    @Test
    public void testNAryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(AND1.transform(bddcnf)).isEqualTo(AND1);
        assertThat(OR1.transform(bddcnf)).isEqualTo(OR1);
        final Formula f1 = p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f1, f1.transform(bddcnf), f1.variables())).isTrue();
        assertThat(f2.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f2, f2.transform(bddcnf), f2.variables())).isTrue();
        assertThat(f3.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f3, f3.transform(bddcnf), f3.variables())).isTrue();
    }

    @Test
    public void testNAryOperatorsWithExternalFactory() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        final BDDCNFTransformation transformation = new BDDCNFTransformation(f, new BDDKernel(f, 7, 100, 1000));
        assertThat(AND1.transform(bddcnf)).isEqualTo(AND1);
        assertThat(OR1.transform(bddcnf)).isEqualTo(OR1);
        final Formula f1 = p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(transformation).isCNF()).isTrue();
        assertThat(equivalentModels(f1, f1.transform(transformation), f1.variables())).isTrue();
        assertThat(f2.transform(transformation).isCNF()).isTrue();
        assertThat(equivalentModels(f2, f2.transform(transformation), f2.variables())).isTrue();
        assertThat(f3.transform(transformation).isCNF()).isTrue();
        assertThat(equivalentModels(f3, f3.transform(transformation), f3.variables())).isTrue();
    }

    @Test
    public void testNAryOperatorsWithExternalFactory2() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        final BDDCNFTransformation transformation = new BDDCNFTransformation(f, new BDDKernel(f, 7, 50, 50));
        assertThat(AND1.transform(bddcnf)).isEqualTo(AND1);
        assertThat(OR1.transform(bddcnf)).isEqualTo(OR1);
        final Formula f1 = p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(transformation).isCNF()).isTrue();
        assertThat(equivalentModels(f1, f1.transform(transformation), f1.variables())).isTrue();
        assertThat(f2.transform(transformation).isCNF()).isTrue();
        assertThat(equivalentModels(f2, f2.transform(transformation), f2.variables())).isTrue();
        assertThat(f3.transform(transformation).isCNF()).isTrue();
        assertThat(equivalentModels(f3, f3.transform(transformation), f3.variables())).isTrue();
    }

    @Test
    public void testNot() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(p.parse("~a").transform(bddcnf)).isEqualTo(p.parse("~a"));
        assertThat(p.parse("~~a").transform(bddcnf)).isEqualTo(p.parse("a"));
        assertThat(p.parse("~(a => b)").transform(bddcnf)).isEqualTo(p.parse("a & ~b"));
        final Formula f1 = p.parse("~(~(a | b) => ~(x | y))");
        final Formula f2 = p.parse("~(a <=> b)");
        final Formula f3 = p.parse("~(~(a | b) <=> ~(x | y))");
        final Formula f4 = p.parse("~(a & b & ~x & ~y)");
        final Formula f5 = p.parse("~(a | b | ~x | ~y)");
        assertThat(f1.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f1, f1.transform(bddcnf), f1.variables())).isTrue();
        assertThat(f2.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f2, f2.transform(bddcnf), f2.variables())).isTrue();
        assertThat(f3.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f3, f3.transform(bddcnf), f3.variables())).isTrue();
        assertThat(f4.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f4, f4.transform(bddcnf), f4.variables())).isTrue();
        assertThat(f5.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f5, f5.transform(bddcnf), f5.variables())).isTrue();
        assertThat(f5.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f5, f5.transform(bddcnf), f5.variables())).isTrue();
    }

    @Test
    public void testCC() throws ParserException {
        final PseudoBooleanParser p = new PseudoBooleanParser(f);
        final Formula f1 = p.parse("a <=> (1 * b <= 1)");
        final Formula f2 = p.parse("~(1 * b <= 1)");
        final Formula f3 = p.parse("(1 * b + 1 * c + 1 * d <= 1)");
        final Formula f4 = p.parse("~(1 * b + 1 * c + 1 * d <= 1)");
        assertThat(f1.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f1, f1.transform(bddcnf), f1.variables())).isTrue();
        assertThat(f2.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f2, f2.transform(bddcnf), f2.variables())).isTrue();
        assertThat(f3.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f3, f3.transform(bddcnf), f3.variables())).isTrue();
        assertThat(f4.transform(bddcnf).isCNF()).isTrue();
        assertThat(equivalentModels(f4, f4.transform(bddcnf), f4.variables())).isTrue();
    }
}
