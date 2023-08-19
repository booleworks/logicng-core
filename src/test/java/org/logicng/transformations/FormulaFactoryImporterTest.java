// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaFactoryConfig;
import org.logicng.formulas.implementation.cached.CachingFormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PseudoBooleanParser;
import org.logicng.transformations.cnf.TseitinTransformation;

public class FormulaFactoryImporterTest extends TestWithExampleFormulas {

    private CachingFormulaFactory myF;
    private CachingFormulaFactory myG;
    private FormulaFactoryImporter importer;

    @BeforeEach
    public void initialize() {
        myF = f;
        myG = new CachingFormulaFactory(FormulaFactoryConfig.builder().name("Factory G").build());
        importer = new FormulaFactoryImporter(myG);
    }

    @Test
    public void testSameFactory() {
        final FormulaFactoryImporter sameImporter = new FormulaFactoryImporter(myF);
        assertThat(NOT1.factory()).isSameAs(myF);
        assertThat(NOT2.factory()).isSameAs(myF);
        assertThat(sameImporter.apply(NOT1).factory()).isSameAs(myF);
        assertThat(sameImporter.apply(NOT2).factory()).isSameAs(myF);
        assertThat(sameImporter.apply(NOT1)).isEqualTo(NOT1);
        assertThat(sameImporter.apply(NOT2)).isEqualTo(NOT2);
    }

    @Test
    public void testConstants() {
        assertThat(TRUE.factory()).isSameAs(myF);
        assertThat(FALSE.factory()).isSameAs(myF);
        assertThat(imp(TRUE).factory()).isSameAs(myG);
        assertThat(imp(FALSE).factory()).isSameAs(myG);
        assertThat(imp(TRUE)).isEqualTo(TRUE);
        assertThat(imp(FALSE)).isEqualTo(FALSE);
    }

    @Test
    public void testLiteral() {
        assertThat(A.factory()).isSameAs(myF);
        assertThat(NA.factory()).isSameAs(myF);
        assertThat(imp(A).factory()).isSameAs(myG);
        assertThat(imp(NA).factory()).isSameAs(myG);
        assertThat(imp(A)).isEqualTo(A);
        assertThat(imp(NA)).isEqualTo(NA);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(1);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(1);
    }

    @Test
    public void testNot() {
        assertThat(NOT1.factory()).isSameAs(myF);
        assertThat(NOT2.factory()).isSameAs(myF);
        assertThat(imp(NOT1).factory()).isSameAs(myG);
        assertThat(imp(NOT2).factory()).isSameAs(myG);
        assertThat(imp(NOT1)).isEqualTo(NOT1);
        assertThat(imp(NOT2)).isEqualTo(NOT2);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(4);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(0);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().negations()).isEqualTo(2);
    }

    @Test
    public void testImplication() {
        assertThat(IMP1.factory()).isSameAs(myF);
        assertThat(IMP2.factory()).isSameAs(myF);
        assertThat(IMP3.factory()).isSameAs(myF);
        assertThat(IMP4.factory()).isSameAs(myF);
        assertThat(imp(IMP1).factory()).isSameAs(myG);
        assertThat(imp(IMP2).factory()).isSameAs(myG);
        assertThat(imp(IMP3).factory()).isSameAs(myG);
        assertThat(imp(IMP4).factory()).isSameAs(myG);
        assertThat(imp(IMP1)).isEqualTo(IMP1);
        assertThat(imp(IMP2)).isEqualTo(IMP2);
        assertThat(imp(IMP3)).isEqualTo(IMP3);
        assertThat(imp(IMP4)).isEqualTo(IMP4);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(4);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(4);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().negations()).isEqualTo(0);
        assertThat(myG.statistics().implications()).isEqualTo(4);
        assertThat(myG.statistics().equivalences()).isEqualTo(2);
    }

    @Test
    public void testEquivalence() {
        assertThat(EQ1.factory()).isSameAs(myF);
        assertThat(EQ2.factory()).isSameAs(myF);
        assertThat(EQ3.factory()).isSameAs(myF);
        assertThat(EQ4.factory()).isSameAs(myF);
        assertThat(imp(EQ1).factory()).isSameAs(myG);
        assertThat(imp(EQ2).factory()).isSameAs(myG);
        assertThat(imp(EQ3).factory()).isSameAs(myG);
        assertThat(imp(EQ4).factory()).isSameAs(myG);
        assertThat(imp(EQ1)).isEqualTo(EQ1);
        assertThat(imp(EQ2)).isEqualTo(EQ2);
        assertThat(imp(EQ3)).isEqualTo(EQ3);
        assertThat(imp(EQ4)).isEqualTo(EQ4);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(4);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(2);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().negations()).isEqualTo(0);
        assertThat(myG.statistics().implications()).isEqualTo(2);
        assertThat(myG.statistics().equivalences()).isEqualTo(4);
    }

    @Test
    public void testOr() {
        assertThat(OR1.factory()).isSameAs(myF);
        assertThat(OR2.factory()).isSameAs(myF);
        assertThat(OR3.factory()).isSameAs(myF);
        assertThat(imp(OR1).factory()).isSameAs(myG);
        assertThat(imp(OR2).factory()).isSameAs(myG);
        assertThat(imp(OR3).factory()).isSameAs(myG);
        assertThat(imp(OR1)).isEqualTo(OR1);
        assertThat(imp(OR2)).isEqualTo(OR2);
        assertThat(imp(OR3)).isEqualTo(OR3);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(4);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(4);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(2);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(3);
        assertThat(myG.statistics().negations()).isEqualTo(0);
        assertThat(myG.statistics().implications()).isEqualTo(0);
        assertThat(myG.statistics().equivalences()).isEqualTo(0);
    }

    @Test
    public void testAnd() {
        assertThat(AND1.factory()).isSameAs(myF);
        assertThat(AND2.factory()).isSameAs(myF);
        assertThat(AND3.factory()).isSameAs(myF);
        assertThat(imp(AND1).factory()).isSameAs(myG);
        assertThat(imp(AND2).factory()).isSameAs(myG);
        assertThat(imp(AND3).factory()).isSameAs(myG);
        assertThat(imp(AND1)).isEqualTo(AND1);
        assertThat(imp(AND2)).isEqualTo(AND2);
        assertThat(imp(AND3)).isEqualTo(AND3);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(4);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(4);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(3);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(2);
        assertThat(myG.statistics().negations()).isEqualTo(0);
        assertThat(myG.statistics().implications()).isEqualTo(0);
        assertThat(myG.statistics().equivalences()).isEqualTo(0);
    }

    @Test
    public void testPBC() {
        assertThat(PBC1.factory()).isSameAs(myF);
        assertThat(PBC2.factory()).isSameAs(myF);
        assertThat(PBC3.factory()).isSameAs(myF);
        assertThat(PBC4.factory()).isSameAs(myF);
        assertThat(PBC5.factory()).isSameAs(myF);
        assertThat(imp(PBC1).factory()).isSameAs(myG);
        assertThat(imp(PBC2).factory()).isSameAs(myG);
        assertThat(imp(PBC3).factory()).isSameAs(myG);
        assertThat(imp(PBC4).factory()).isSameAs(myG);
        assertThat(imp(PBC5).factory()).isSameAs(myG);
        assertThat(imp(PBC1)).isEqualTo(PBC1);
        assertThat(imp(PBC2)).isEqualTo(PBC2);
        assertThat(imp(PBC3)).isEqualTo(PBC3);
        assertThat(imp(PBC4)).isEqualTo(PBC4);
        assertThat(imp(PBC5)).isEqualTo(PBC5);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(3);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(0);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(0);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(0);
        assertThat(myG.statistics().negations()).isEqualTo(0);
        assertThat(myG.statistics().implications()).isEqualTo(0);
        assertThat(myG.statistics().equivalences()).isEqualTo(0);
    }

    @Test
    public void testAdjustCounters() throws ParserException {
        final CachingFormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().name("Factory").build());
        final PseudoBooleanParser p = new PseudoBooleanParser(f);
        final Formula cc = p.parse("A + B + C + D + E <= 2").cnf();
        final Formula pbc = p.parse("2*A + -2*B + 3*C + D + 2*E <= 3").cnf();
        final Formula cnf = p.parse("A & B & C | C & D & ~A").transform(new TseitinTransformation(f, 0));

        final CachingFormulaFactory g = new CachingFormulaFactory();
        g.newCNFVariable();
        g.newCNFVariable();
        g.newCCVariable();
        g.newCCVariable();
        g.newCCVariable();
        g.newPBVariable();
        g.newPBVariable();
        g.newPBVariable();
        g.importFormula(cc);
        g.importFormula(pbc);
        g.importFormula(cnf);
        assertThat(g.statistics().cnfCounter()).isEqualTo(2);
        assertThat(g.statistics().ccCounter()).isEqualTo(13);
        assertThat(g.statistics().pbCounter()).isEqualTo(25);
    }

    /**
     * Imports the given formula in the new formula factory.
     * @param formula the formula to import
     * @return the imported formula
     */
    private Formula imp(final Formula formula) {
        return importer.apply(formula);
    }
}
