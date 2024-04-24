// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.np;

import com.booleworks.logicng.formulas.FormulaFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SetCoverTest {

    private final FormulaFactory f = FormulaFactory.caching();

    @Test
    public void smallTest() {
        final List<Set<String>> sets = new ArrayList<>();
        sets.add(new TreeSet<>(Arrays.asList("a", "b", "c", "d", "e", "f")));
        sets.add(new TreeSet<>(Arrays.asList("e", "f", "h", "i")));
        sets.add(new TreeSet<>(Arrays.asList("a", "d", "g", "j")));
        sets.add(new TreeSet<>(Arrays.asList("b", "e", "h", "k")));
        sets.add(new TreeSet<>(Arrays.asList("c", "f", "i", "l")));
        sets.add(new TreeSet<>(Arrays.asList("j", "k", "l")));
        Assertions.assertThat(SetCover.compute(f, sets)).containsExactlyInAnyOrder(sets.get(2), sets.get(3),
                sets.get(4));
    }

    @Test
    public void cornerCasesTest() {
        final List<Set<String>> sets = new ArrayList<>();
        Assertions.assertThat(SetCover.compute(f, sets)).isEmpty();
        sets.add(Collections.emptySet());
        Assertions.assertThat(SetCover.compute(f, sets)).isEmpty();
        sets.add(new HashSet<>(Collections.singletonList("A")));
        sets.add(new HashSet<>(Collections.singletonList("A")));
        sets.add(new HashSet<>(Collections.singletonList("A")));
        Assertions.assertThat(SetCover.compute(f, sets)).hasSize(1);
        sets.add(new HashSet<>(Collections.singletonList("B")));
        Assertions.assertThat(SetCover.compute(f, sets)).containsExactlyInAnyOrder(
                new HashSet<>(Collections.singletonList("A")),
                new HashSet<>(Collections.singletonList("B"))
        );
        sets.add(new HashSet<>(Arrays.asList("A", "B")));
        Assertions.assertThat(SetCover.compute(f, sets)).hasSize(1).containsExactly(
                new HashSet<>(Arrays.asList("A", "B"))
        );
    }
}
