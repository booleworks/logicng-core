// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
            Assertions.assertThat(s.sat(f.literal("a", false))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("b"))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("c"))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("d"))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("e"))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("f"))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("g"))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(f.variable("a"))).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(f.literal("b", false))).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(f.literal("c", false))).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(f.literal("d", false))).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(f.literal("e", false))).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(f.literal("f", false))).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(f.literal("g", false))).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(assumptions1)).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(assumptions2)).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(assumptions3)).isEqualTo(Tristate.TRUE);
            Assertions.assertThat(s.sat(assumptions4)).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(assumptions5)).isEqualTo(Tristate.FALSE);
            Assertions.assertThat(s.sat(assumptions6)).isEqualTo(Tristate.FALSE);
            s.reset();
        }
    }
}
