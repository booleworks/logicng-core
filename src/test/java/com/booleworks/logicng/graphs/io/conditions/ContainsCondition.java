// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.io.conditions;

import org.assertj.core.api.Condition;

import java.util.List;

public class ContainsCondition extends Condition<List<? extends String>> {

    private final String element;

    public ContainsCondition(final String element) {
        this.element = element;
    }

    @Override
    public boolean matches(final List<? extends String> strings) {
        return strings.contains(element);
    }

}
