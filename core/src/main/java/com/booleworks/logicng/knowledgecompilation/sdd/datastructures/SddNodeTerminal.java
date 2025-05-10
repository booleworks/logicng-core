package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;

import java.util.BitSet;

public class SddNodeTerminal extends SddNode {
    private final boolean phase;

    public SddNodeTerminal(final int id, final VTreeLeaf vtree, final boolean phase) {
        super(id, vtree, calculateVariableMask(vtree));
        assert vtree == null || vtree.isLeaf();
        this.phase = phase;
    }

    private static BitSet calculateVariableMask(final VTreeLeaf leaf) {
        final BitSet variableMask = new BitSet();
        if (leaf != null) {
            variableMask.set(leaf.getVariable());
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

    @Override
    public VTreeLeaf getVTree() {
        if (super.vTree != null) {
            return super.getVTree().asLeaf();
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
        return vTree == null && phase;
    }

    @Override
    public boolean isFalse() {
        return vTree == null && !phase;
    }

    @Override
    public boolean isLiteral() {
        return vTree != null;
    }

    @Override
    public boolean isDecomposition() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + id + ": " + (vTree == null ? "trivial" : vTree.asLeaf().getVariable()) + " )";
    }
}
