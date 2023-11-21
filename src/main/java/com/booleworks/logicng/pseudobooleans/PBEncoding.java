// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

import java.util.List;

/**
 * The interface for pseudo Boolean constraint encodings.
 * @version 3.0.0
 * @since 1.1
 */
public interface PBEncoding {

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f      the formula factory to generate new formulas
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraint
     * @param rhs    the right-hand side of the constraint
     * @param result the current result CNF
     * @return the CNF encoding of the constraint
     */
    default List<Formula> encode(final FormulaFactory f, final LNGVector<Literal> lits, final LNGIntVector coeffs, final int rhs, final List<Formula> result) {
        return encode(f, lits, coeffs, rhs, result, (PBConfig) f.configurationFor(ConfigurationType.PB_ENCODER));
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f      the formula factory to generate new formulas
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraint
     * @param rhs    the right-hand side of the constraint
     * @param result the current result CNF
     * @param config the configuration for the encoding
     * @return the CNF encoding of the constraint
     */
    List<Formula> encode(final FormulaFactory f, final LNGVector<Literal> lits, final LNGIntVector coeffs, int rhs, final List<Formula> result,
                         final PBConfig config);
}
