// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

/**
 * An abstract super class for a style mapper for a graphical representation of a BDD.
 * @version 2.4.0
 * @since 2.4.0
 */
public abstract class BddNodeStyleMapper extends BddMapper implements NodeStyleMapper<Integer> {

    /**
     * Constructs a new BDD style mapper for a given BDD kernel.  The BDDs which are styled
     * must be constructed with this kernel.
     * @param kernel a BDD kernel
     */
    public BddNodeStyleMapper(final BDDKernel kernel) {
        super(kernel);
    }

    @Override
    public abstract GraphicalNodeStyle computeStyle(final Integer index);
}
