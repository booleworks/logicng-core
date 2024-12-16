package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * An encoding result that materializes encodings on a formula factory as
 * formulas.
 */
public final class EncodingResultFF implements EncodingResult {

    private final FormulaFactory f;
    private final List<Formula> result;

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

    public List<Formula> getResult() {
        return result;
    }

    @Override
    public FormulaFactory getFactory() {
        return f;
    }
}
