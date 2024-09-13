// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static com.booleworks.logicng.serialization.Propositions.deserializePropositions;
import static com.booleworks.logicng.serialization.Propositions.serializePropositions;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.propositions.StandardProposition;
import org.junit.jupiter.api.Test;

public class PropositionsTest {

    final FormulaFactory f = FormulaFactory.caching();

    @Test
    public void testStandardProposition() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        final StandardProposition p = new StandardProposition("description", parser.parse("a & (b => c + d = 1) <=> ~x"));
        assertThat(deserializePropositions(f, serializePropositions(p))).isEqualTo(p);
    }
}
