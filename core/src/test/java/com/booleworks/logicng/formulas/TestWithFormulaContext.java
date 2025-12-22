// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class TestWithFormulaContext {
    public static Collection<Object[]> contexts() {
        final List<Object[]> contexts = new ArrayList<>();
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.caching())});
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.nonCaching())});
        return contexts;
    }
}
