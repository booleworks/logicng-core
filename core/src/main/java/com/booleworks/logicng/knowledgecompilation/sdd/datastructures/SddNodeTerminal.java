package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;

import java.util.BitSet;

public final class SddNodeTerminal extends SddNode {
    private final boolean phase;

    SddNodeTerminal(final int id, final Sdd.GSCacheEntry<VTree> vtree, final boolean phase) {
        super(id, vtree, calculateVariableMask(vtree.getElement()));
        assert vtree.getElement() == null || vtree.getElement().isLeaf();
        this.phase = phase;
    }

    private static BitSet calculateVariableMask(final VTree leaf) {
        final BitSet variableMask = new BitSet();
        if (leaf != null) {
            variableMask.set(leaf.asLeaf().getVariable());
        }
        return variableMask;
    }

    public boolean getPhase() {
        return phase;
    }

    public Formula toFormula(final Sdd sdd) {
        if (getVTree() == null) {
            return sdd.getFactory().constant(getPhase());
        } else {
            final int varIdx = getVTree().getVariable();
            final Variable var = sdd.indexToVariable(varIdx);
            return getPhase() ? var : var.negate(sdd.getFactory());
        }
    }

    public VTreeLeaf getVTree() {
        if (super.getVTreeEntry().getElement() != null) {
            return super.getVTreeEntry().getElement().asLeaf();
        } else {
            return null;
        }
    }

    @Override
    public boolean isTrivial() {
        return isTrue() || isFalse();
    }

    @Override
    public boolean isTrue() {
        return getVTreeEntry().getElement() == null && phase;
    }

    @Override
    public boolean isFalse() {
        return getVTreeEntry().getElement() == null && !phase;
    }

    @Override
    public boolean isLiteral() {
        return getVTreeEntry().getElement() != null;
    }

    @Override
    public boolean isDecomposition() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + id + ": " +
                (phase ? "+" : "-") +
                (getVTreeEntry().getElement() == null ? "trivial" : getVTreeEntry().getElement().asLeaf().getVariable())
                + " )";
    }
}
