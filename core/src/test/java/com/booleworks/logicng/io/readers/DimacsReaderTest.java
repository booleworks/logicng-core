// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.readers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.booleworks.logicng.formulas.FormulaFactory;
import org.junit.jupiter.api.Test;

import java.io.File;

public class DimacsReaderTest {

    @Test
    public void testExceptionalBehavior() {
        assertThatThrownBy(() -> {
            final FormulaFactory f = FormulaFactory.caching();
            final File file = new File("../test_files/dimacs/malformed/contains-line-without-zero.cnf");
            DimacsReader.readCNF(f, file, "v");
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Line '2 -3' did not end with 0.");
    }
}
