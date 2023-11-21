// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.datastructures.Substitution;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;

public class AnonymizerTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleFormulasDefault(final FormulaContext _c) throws ParserException {
        final Anonymizer anonymizer = new Anonymizer(_c.f);
        Assertions.assertThat(_c.p.parse("$true").transform(anonymizer)).isEqualTo(_c.p.parse("$true"));
        Assertions.assertThat(_c.p.parse("$false").transform(anonymizer)).isEqualTo(_c.p.parse("$false"));
        Assertions.assertThat(_c.p.parse("A").transform(anonymizer)).isEqualTo(_c.p.parse("v0"));
        Assertions.assertThat(_c.p.parse("~A").transform(anonymizer)).isEqualTo(_c.p.parse("~v0"));
        Assertions.assertThat(_c.p.parse("A => ~B").transform(anonymizer)).isEqualTo(_c.p.parse("v0 => ~v1"));
        Assertions.assertThat(_c.p.parse("A <=> ~B").transform(anonymizer)).isEqualTo(_c.p.parse("v0 <=> ~v1"));
        Assertions.assertThat(_c.p.parse("A | B | ~D | C").transform(anonymizer)).isEqualTo(_c.p.parse("v0 | v1 | ~v3 | v2"));
        Assertions.assertThat(_c.p.parse("A & B & C & ~D").transform(anonymizer)).isEqualTo(_c.p.parse("v0 & v1 & v2 & ~v3"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleFormulasOwnPrefix(final FormulaContext _c) throws ParserException {
        final Anonymizer anonymizer = new Anonymizer(_c.f, "var");
        Assertions.assertThat(_c.p.parse("$true").transform(anonymizer)).isEqualTo(_c.p.parse("$true"));
        Assertions.assertThat(_c.p.parse("$false").transform(anonymizer)).isEqualTo(_c.p.parse("$false"));
        Assertions.assertThat(_c.p.parse("A").transform(anonymizer)).isEqualTo(_c.p.parse("var0"));
        Assertions.assertThat(_c.p.parse("~A").transform(anonymizer)).isEqualTo(_c.p.parse("~var0"));
        Assertions.assertThat(_c.p.parse("A => ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var0 => ~var1"));
        Assertions.assertThat(_c.p.parse("A <=> ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var0 <=> ~var1"));
        Assertions.assertThat(_c.p.parse("A <=> ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var0 <=> ~var1"));
        Assertions.assertThat(_c.p.parse("A | B | ~D | C").transform(anonymizer)).isEqualTo(_c.p.parse("var0 | var1 | ~var3 | var2"));
        Assertions.assertThat(_c.p.parse("A & B & C & ~D").transform(anonymizer)).isEqualTo(_c.p.parse("var0 & var1 & var2 & ~var3"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleFormulasOwnPrefixAndCounter(final FormulaContext _c) throws ParserException {
        final Anonymizer anonymizer = new Anonymizer(_c.f, "var", 10);
        Assertions.assertThat(_c.p.parse("$true").transform(anonymizer)).isEqualTo(_c.p.parse("$true"));
        Assertions.assertThat(_c.p.parse("$false").transform(anonymizer)).isEqualTo(_c.p.parse("$false"));
        Assertions.assertThat(_c.p.parse("A").transform(anonymizer)).isEqualTo(_c.p.parse("var10"));
        Assertions.assertThat(_c.p.parse("~A").transform(anonymizer)).isEqualTo(_c.p.parse("~var10"));
        Assertions.assertThat(_c.p.parse("A => ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var10 => ~var11"));
        Assertions.assertThat(_c.p.parse("A <=> ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var10 <=> ~var11"));
        Assertions.assertThat(_c.p.parse("A | B | ~D | C").transform(anonymizer)).isEqualTo(_c.p.parse("var10 | var11 | ~var13 | var12"));
        Assertions.assertThat(_c.p.parse("A & B & C & ~D").transform(anonymizer)).isEqualTo(_c.p.parse("var10 & var11 & var12 & ~var13"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleFormulasOwnPrefixAndCounterWithoutCache(final FormulaContext _c) throws ParserException {
        final Anonymizer anonymizer = new Anonymizer(_c.f, "var", 10);
        Assertions.assertThat(_c.p.parse("$true").transform(anonymizer)).isEqualTo(_c.p.parse("$true"));
        Assertions.assertThat(_c.p.parse("$false").transform(anonymizer)).isEqualTo(_c.p.parse("$false"));
        Assertions.assertThat(_c.p.parse("A").transform(anonymizer)).isEqualTo(_c.p.parse("var10"));
        Assertions.assertThat(_c.p.parse("~A").transform(anonymizer)).isEqualTo(_c.p.parse("~var10"));
        Assertions.assertThat(_c.p.parse("A => ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var10 => ~var11"));
        Assertions.assertThat(_c.p.parse("A <=> ~B").transform(anonymizer)).isEqualTo(_c.p.parse("var10 <=> ~var11"));
        Assertions.assertThat(_c.p.parse("A | B | ~D | C").transform(anonymizer)).isEqualTo(_c.p.parse("var10 | var11 | ~var13 | var12"));
        Assertions.assertThat(_c.p.parse("A & B & C & ~D").transform(anonymizer)).isEqualTo(_c.p.parse("var10 & var11 & var12 & ~var13"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetSubstitution(final FormulaContext _c) throws ParserException {
        final Anonymizer anonymizer = new Anonymizer(_c.f, "v", 0);
        Assertions.assertThat(anonymizer.getSubstitution()).isEqualTo(new Substitution());
        Assertions.assertThat(_c.p.parse("A & B & C & ~D").transform(anonymizer)).isEqualTo(_c.p.parse("v0 & v1 & v2 & ~v3"));
        final HashMap<Variable, Formula> mapping = new HashMap<>();
        mapping.put(_c.f.variable("A"), _c.f.variable("v0"));
        mapping.put(_c.f.variable("B"), _c.f.variable("v1"));
        mapping.put(_c.f.variable("C"), _c.f.variable("v2"));
        mapping.put(_c.f.variable("D"), _c.f.variable("v3"));
        Assertions.assertThat(anonymizer.getSubstitution()).isEqualTo(new Substitution(mapping));
        Assertions.assertThat(_c.p.parse("E & A & C & ~F").transform(anonymizer)).isEqualTo(_c.p.parse("v4 & v0 & v2 & ~v5"));
        mapping.put(_c.f.variable("E"), _c.f.variable("v4"));
        mapping.put(_c.f.variable("F"), _c.f.variable("v5"));
        Assertions.assertThat(anonymizer.getSubstitution()).isEqualTo(new Substitution(mapping));
    }
}
