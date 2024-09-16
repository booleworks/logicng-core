// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CacheTest {

    @Test
    public void testDescription() {
        assertThat(TransformationCacheEntry.AIG.getDescription())
                .isEqualTo("TransformationCacheEntry{description=and-inverter graph}");
        assertThat(TransformationCacheEntry.NNF.getDescription())
                .isEqualTo("TransformationCacheEntry{description=negation normal form}");
        assertThat(TransformationCacheEntry.FACTORIZED_CNF.getDescription())
                .isEqualTo("TransformationCacheEntry{description=factorized conjunctive normal form}");

        assertThat(PredicateCacheEntry.IS_AIG.getDescription())
                .isEqualTo("PredicateCacheEntry{description=and-inverter graph}");
        assertThat(PredicateCacheEntry.IS_CNF.getDescription())
                .isEqualTo("PredicateCacheEntry{description=conjunctive normal form}");

        assertThat(FunctionCacheEntry.LITPROFILE.getDescription())
                .isEqualTo("FunctionCacheEntry{description=literal profile}");
        assertThat(FunctionCacheEntry.VARPROFILE.getDescription())
                .isEqualTo("FunctionCacheEntry{description=variable profile}");
        assertThat(FunctionCacheEntry.SUBFORMULAS.getDescription())
                .isEqualTo("FunctionCacheEntry{description=sub-formulas}");
    }

    @Test
    public void testValues() {
        final List<TransformationCacheEntry> valuesTrans = Arrays.asList(TransformationCacheEntry.values());
        assertThat(valuesTrans.size()).isEqualTo(8);
        assertThat(valuesTrans.contains(TransformationCacheEntry.valueOf("FACTORIZED_DNF"))).isTrue();

        final List<PredicateCacheEntry> valuesPred = Arrays.asList(PredicateCacheEntry.values());
        assertThat(valuesPred.size()).isEqualTo(5);
        assertThat(valuesPred.contains(PredicateCacheEntry.valueOf("IS_NNF"))).isTrue();
        assertThat(valuesPred.contains(PredicateCacheEntry.valueOf("IS_CNF"))).isTrue();
        assertThat(valuesPred.contains(PredicateCacheEntry.valueOf("IS_DNF"))).isTrue();
        assertThat(valuesPred.contains(PredicateCacheEntry.valueOf("IS_SAT"))).isTrue();

        final List<FunctionCacheEntry> valuesFunc = Arrays.asList(FunctionCacheEntry.values());
        assertThat(valuesFunc.size()).isEqualTo(9);
        assertThat(valuesFunc.contains(FunctionCacheEntry.valueOf("LITPROFILE"))).isTrue();
        assertThat(valuesFunc.contains(FunctionCacheEntry.valueOf("SUBFORMULAS"))).isTrue();
    }
}
