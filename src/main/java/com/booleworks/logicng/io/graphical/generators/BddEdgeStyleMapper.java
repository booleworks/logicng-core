// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

/**
 * An abstract super class for an edge mapper for a graphical representation of a BDD.
 * @version 2.4.0
 * @since 2.4.0
 */
public abstract class BddEdgeStyleMapper extends BddMapper implements EdgeStyleMapper<Integer> {

    /**
     * Constructs a new BDD edge mapper for a given BDD kernel.  The BDDs must be constructed with this kernel.
     * @param kernel a BDD kernel
     */
    public BddEdgeStyleMapper(final BDDKernel kernel) {
        super(kernel);
    }

    @Override
    public abstract GraphicalEdgeStyle computeStyle(final Integer source, final Integer destination);
}

