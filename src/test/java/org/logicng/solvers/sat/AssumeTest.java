// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.datastructures.Tristate.FALSE;
import static org.logicng.datastructures.Tristate.TRUE;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.Arrays;
import java.util.List;

public class AssumeTest {

    private final FormulaFactory f;
    private final SATSolver[] solvers;
    private final PropositionalParser parser;

    public AssumeTest() {
        f = FormulaFactory.caching();
        parser = new PropositionalParser(f);
        solvers = new SATSolver[5];
        solvers[0] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useAtMostClauses(false).build());
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useAtMostClauses(false).build());
        solvers[2] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useBinaryWatchers(true).useLbdFeatures(true).build());
        solvers[3] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useAtMostClauses(true).build());
        solvers[4] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useAtMostClauses(true).build());
    }

    @Test
    public void testAssume() throws ParserException {
        final List<Literal> assumptions1 = Arrays.asList(f.literal("c", true), f.literal("d", true));
        final List<Literal> assumptions2 = Arrays.asList(f.literal("x", false), f.literal("y", true), f.literal("d", true));
        final List<Literal> assumptions3 = Arrays.asList(f.literal("a", false), f.literal("c", true), f.literal("a", false));
        final List<Literal> assumptions4 = Arrays.asList(f.literal("c", false), f.literal("d", true));
        final List<Literal> assumptions5 = Arrays.asList(f.literal("x", true), f.literal("x", false));
        final List<Literal> assumptions6 = Arrays.asList(f.literal("a", true), f.literal("a", false));
        for (final SATSolver s : solvers) {
            s.add(parser.parse("~a"));
            s.add(parser.parse("b"));
            s.add(parser.parse("b => c"));
            s.add(parser.parse("c => d"));
            s.add(parser.parse("d => e"));
            s.add(parser.parse("e => f"));
            assertThat(s.sat(f.literal("a", false))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("b"))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("c"))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("d"))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("e"))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("f"))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("g"))).isEqualTo(TRUE);
            assertThat(s.sat(f.variable("a"))).isEqualTo(FALSE);
            assertThat(s.sat(f.literal("b", false))).isEqualTo(FALSE);
            assertThat(s.sat(f.literal("c", false))).isEqualTo(FALSE);
            assertThat(s.sat(f.literal("d", false))).isEqualTo(FALSE);
            assertThat(s.sat(f.literal("e", false))).isEqualTo(FALSE);
            assertThat(s.sat(f.literal("f", false))).isEqualTo(FALSE);
            assertThat(s.sat(f.literal("g", false))).isEqualTo(TRUE);
            assertThat(s.sat(assumptions1)).isEqualTo(TRUE);
            assertThat(s.sat(assumptions2)).isEqualTo(TRUE);
            assertThat(s.sat(assumptions3)).isEqualTo(TRUE);
            assertThat(s.sat(assumptions4)).isEqualTo(FALSE);
            assertThat(s.sat(assumptions5)).isEqualTo(FALSE);
            assertThat(s.sat(assumptions6)).isEqualTo(FALSE);
            s.reset();
        }
    }
}
