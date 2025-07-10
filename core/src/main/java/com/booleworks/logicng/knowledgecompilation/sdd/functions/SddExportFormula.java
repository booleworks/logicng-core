package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;

import java.util.ArrayList;

public class SddExportFormula implements SddFunction<Formula> {
    public final Sdd sdd;

    public SddExportFormula(final Sdd sdd) {
        this.sdd = sdd;
    }

    @Override
    public LngResult<Formula> execute(final SddNode node, final ComputationHandler handler) {
        return LngResult.of(applyRec(node));
    }

    public Formula applyRec(final SddNode node) {
        final FormulaFactory f = sdd.getFactory();
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final ArrayList<Formula> elementFormulas = new ArrayList<>(decomp.getElementsUnsafe().size());
            for (final SddElement element : node.asDecomposition()) {
                final Formula sub = applyRec(element.getSub());
                final Formula prime = applyRec(element.getPrime());
                elementFormulas.add(f.and(sub, prime));
            }
            return f.or(elementFormulas);
        } else {
            return node.asTerminal().toFormula(sdd);
        }
    }
}
