//  SPDX-License-Identifier: Apache-2.0 and MIT
//  Copyright 2015-2023 Christoph Zengler
//  Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An encoding result that materializes encodings on a formula factory as
 * formulas.
 * @version 3.0.0
 * @since 3.0.0
 */
public class EncodingResultFF implements EncodingResult {

    protected final FormulaFactory f;
    protected final List<Formula> result;

    /**
     * Constructs an encoding result that creates the encoding on a
     * {@link FormulaFactory}.
     * @param f the formula factory
     */
    public EncodingResultFF(final FormulaFactory f) {
        this.f = f;
        result = new ArrayList<>();
    }

    @Override
    public void addClause(final Literal... literals) {
        result.add(f.clause(literals));
    }

    @Override
    public void addClause(final Collection<Literal> literals) {
        result.add(f.clause(literals));
    }

    @Override
    public void addClause(final LngVector<Literal> literals) {
        final List<Literal> lits = new ArrayList<>(literals.size());
        for (final Literal l : literals) {
            lits.add(l);
        }
        result.add(f.clause(lits));
    }

    @Override
    public Variable newVariable(final String auxType) {
        return f.newAuxVariable(auxType);
    }

    /**
     * Returns the clauses added to this result.
     * @return the clauses added to this result
     */
    public List<Formula> getResult() {
        return result;
    }

    @Override
    public FormulaFactory getFactory() {
        return f;
    }
}
