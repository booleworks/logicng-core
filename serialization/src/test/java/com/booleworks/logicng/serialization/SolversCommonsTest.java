// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static com.booleworks.logicng.serialization.SolverSerializer.deserializeStack;
import static com.booleworks.logicng.serialization.SolverSerializer.serializeStack;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Stack;

public class SolversCommonsTest {

    @Test
    public void testStack() {
        final Random random = new Random(42);
        final Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < 1000; i++) {
            stack.push(random.nextInt());
        }
        final ProtoBufCollections.PBIntVector serialized = serializeStack(stack);
        final Stack<Integer> deserialized = deserializeStack(serialized);

        assertThat(deserialized).isEqualTo(stack);
    }
}
