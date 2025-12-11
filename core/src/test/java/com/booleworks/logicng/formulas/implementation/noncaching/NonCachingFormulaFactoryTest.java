package com.booleworks.logicng.formulas.implementation.noncaching;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.api.Test;

public class NonCachingFormulaFactoryTest {

    @Test
    public void testToString() {
        final FormulaFactory f =
                FormulaFactory.nonCaching(FormulaFactoryConfig.builder().name("MyFormulaFactory").build());
        final String expected = "Name: MyFormulaFactory";
        assertThat(f.toString()).isEqualTo(expected);
    }


    @Test
    public void testImportFormula() throws ParserException {
        final NonCachingFormulaFactory f =
                FormulaFactory.nonCaching(FormulaFactoryConfig.builder().name("Factory F").build());
        final NonCachingFormulaFactory g =
                FormulaFactory.nonCaching(FormulaFactoryConfig.builder().name("Factory G").build());
        final PropositionalParser pf = new PropositionalParser(f);
        final String formula = "x1 & x2 & ~x3 => (x4 | (x5 <=> ~x1))";
        final Formula ff = pf.parse(formula);
        final Formula fg = g.importFormula(ff);
        assertThat(fg).isEqualTo(ff);
        assertThat(ff.getFactory()).isSameAs(f);
        assertThat(fg.getFactory()).isSameAs(g);
        for (final Literal litF : ff.literals(f)) {
            assertThat(litF.getFactory()).isSameAs(f);
        }
        for (final Literal litG : fg.literals(f)) {
            assertThat(litG.getFactory()).isSameAs(f);
        }
    }
}
