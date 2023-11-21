// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

/**
 * An enumeration for the different BDD variable orderings.
 * @version 3.0.0
 * @since 1.4.0
 */
public enum VariableOrdering {

    DFS,
    BFS,
    MIN2MAX,
    MAX2MIN,
    FORCE
}
