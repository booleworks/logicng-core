// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FormulaFactoryImporterTest extends TestWithFormulaContext {

    private CachingFormulaFactory myF;
    private FormulaContext c;
    private CachingFormulaFactory myG;
    private FormulaFactoryImporter importer;

    @BeforeEach
    public void initialize() {
        myF = new CachingFormulaFactory();
        c = new FormulaContext(myF);
        myG = new CachingFormulaFactory(FormulaFactoryConfig.builder().name("Factory G").build());
        importer = new FormulaFactoryImporter(myG);
    }

    @Test
    public void testSameFactory() {
        final FormulaFactoryImporter sameImporter = new FormulaFactoryImporter(myF);
        assertThat(c.not1.getFactory()).isSameAs(myF);
        assertThat(c.not2.getFactory()).isSameAs(myF);
        assertThat(sameImporter.apply(c.not1).getFactory()).isSameAs(myF);
        assertThat(sameImporter.apply(c.not2).getFactory()).isSameAs(myF);
        assertThat(sameImporter.apply(c.not1)).isEqualTo(c.not1);
        assertThat(sameImporter.apply(c.not2)).isEqualTo(c.not2);
    }

    @Test
    public void testConstants() {
        assertThat(c.verum.getFactory()).isSameAs(myF);
        assertThat(c.falsum.getFactory()).isSameAs(myF);
        assertThat(imp(c.verum).getFactory()).isSameAs(myG);
        assertThat(imp(c.falsum).getFactory()).isSameAs(myG);
        assertThat(imp(c.verum)).isEqualTo(c.verum);
        assertThat(imp(c.falsum)).isEqualTo(c.falsum);
    }

    @Test
    public void testLiteral() {
        assertThat(c.a.getFactory()).isSameAs(myF);
        assertThat(c.na.getFactory()).isSameAs(myF);
        assertThat(imp(c.a).getFactory()).isSameAs(myG);
        assertThat(imp(c.na).getFactory()).isSameAs(myG);
        assertThat(imp(c.a)).isEqualTo(c.a);
        assertThat(imp(c.na)).isEqualTo(c.na);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(1);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(1);
    }

    @Test
    public void testNot() {
        assertThat(c.not1.getFactory()).isSameAs(myF);
        assertThat(c.not2.getFactory()).isSameAs(myF);
        assertThat(imp(c.not1).getFactory()).isSameAs(myG);
        assertThat(imp(c.not2).getFactory()).isSameAs(myG);
        assertThat(imp(c.not1)).isEqualTo(c.not1);
        assertThat(imp(c.not2)).isEqualTo(c.not2);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(4);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(0);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(1);
        assertThat(myG.statistics().negations()).isEqualTo(2);
    }

    @Test
    public void testImplication() {
        assertThat(c.imp1.getFactory()).isSameAs(myF);
        assertThat(c.imp2.getFactory()).isSameAs(myF);
        assertThat(c.imp3.getFactory()).isSameAs(myF);
        assertThat(c.imp4.getFactory()).isSameAs(myF);
        assertThat(imp(c.imp1).getFactory()).isSameAs(myG);
        assertThat(imp(c.imp2).getFactory()).isSameAs(myG);
        assertThat(imp(c.imp3).getFactory()).isSameAs(myG);
        assertThat(imp(c.imp4).getFactory()).isSameAs(myG);
        assertThat(imp(c.imp1)).isEqualTo(c.imp1);
        assertThat(imp(c.imp2)).isEqualTo(c.imp2);
        assertThat(imp(c.imp3)).isEqualTo(c.imp3);
        assertThat(imp(c.imp4)).isEqualTo(c.imp4);
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
        assertThat(c.eq1.getFactory()).isSameAs(myF);
        assertThat(c.eq2.getFactory()).isSameAs(myF);
        assertThat(c.eq3.getFactory()).isSameAs(myF);
        assertThat(c.eq4.getFactory()).isSameAs(myF);
        assertThat(imp(c.eq1).getFactory()).isSameAs(myG);
        assertThat(imp(c.eq2).getFactory()).isSameAs(myG);
        assertThat(imp(c.eq3).getFactory()).isSameAs(myG);
        assertThat(imp(c.eq4).getFactory()).isSameAs(myG);
        assertThat(imp(c.eq1)).isEqualTo(c.eq1);
        assertThat(imp(c.eq2)).isEqualTo(c.eq2);
        assertThat(imp(c.eq3)).isEqualTo(c.eq3);
        assertThat(imp(c.eq4)).isEqualTo(c.eq4);
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
        assertThat(c.or1.getFactory()).isSameAs(myF);
        assertThat(c.or2.getFactory()).isSameAs(myF);
        assertThat(c.or3.getFactory()).isSameAs(myF);
        assertThat(imp(c.or1).getFactory()).isSameAs(myG);
        assertThat(imp(c.or2).getFactory()).isSameAs(myG);
        assertThat(imp(c.or3).getFactory()).isSameAs(myG);
        assertThat(imp(c.or1)).isEqualTo(c.or1);
        assertThat(imp(c.or2)).isEqualTo(c.or2);
        assertThat(imp(c.or3)).isEqualTo(c.or3);
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
        assertThat(c.and1.getFactory()).isSameAs(myF);
        assertThat(c.and2.getFactory()).isSameAs(myF);
        assertThat(c.and3.getFactory()).isSameAs(myF);
        assertThat(imp(c.and1).getFactory()).isSameAs(myG);
        assertThat(imp(c.and2).getFactory()).isSameAs(myG);
        assertThat(imp(c.and3).getFactory()).isSameAs(myG);
        assertThat(imp(c.and1)).isEqualTo(c.and1);
        assertThat(imp(c.and2)).isEqualTo(c.and2);
        assertThat(imp(c.and3)).isEqualTo(c.and3);
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
        assertThat(c.pbc1.getFactory()).isSameAs(myF);
        assertThat(c.pbc2.getFactory()).isSameAs(myF);
        assertThat(c.pbc3.getFactory()).isSameAs(myF);
        assertThat(c.pbc4.getFactory()).isSameAs(myF);
        assertThat(c.pbc5.getFactory()).isSameAs(myF);
        assertThat(imp(c.pbc1).getFactory()).isSameAs(myG);
        assertThat(imp(c.pbc2).getFactory()).isSameAs(myG);
        assertThat(imp(c.pbc3).getFactory()).isSameAs(myG);
        assertThat(imp(c.pbc4).getFactory()).isSameAs(myG);
        assertThat(imp(c.pbc5).getFactory()).isSameAs(myG);
        assertThat(imp(c.pbc1)).isEqualTo(c.pbc1);
        assertThat(imp(c.pbc2)).isEqualTo(c.pbc2);
        assertThat(imp(c.pbc3)).isEqualTo(c.pbc3);
        assertThat(imp(c.pbc4)).isEqualTo(c.pbc4);
        assertThat(imp(c.pbc5)).isEqualTo(c.pbc5);
        assertThat(myG.statistics().positiveLiterals()).isEqualTo(3);
        assertThat(myG.statistics().negativeLiterals()).isEqualTo(0);
        assertThat(myG.statistics().conjunctions2()).isEqualTo(0);
        assertThat(myG.statistics().disjunctions2()).isEqualTo(0);
        assertThat(myG.statistics().negations()).isEqualTo(0);
        assertThat(myG.statistics().implications()).isEqualTo(0);
        assertThat(myG.statistics().equivalences()).isEqualTo(0);
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
