package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;

/**
 * Class for the SDD terminal node.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddNodeTerminal extends SddNode {
    private final boolean phase;

    SddNodeTerminal(final int id, final VTree vtree, final boolean phase) {
        super(id, vtree);
        assert vtree == null || vtree.isLeaf();
        this.phase = phase;
    }

    /**
     * Returns the phase of the literal.
     * @return the phase of the literal
     */
    public boolean getPhase() {
        return phase;
    }

    /**
     * Converts the terminal element to a formula. For terminal nodes that store
     * a literal, it returns the LNG literal, and for trivial terminal nodes it
     * either returns {@code true} or {@code false}.
     * @param sdd the SDD container
     * @return the value of this node
     */
    public Formula toFormula(final Sdd sdd) {
        if (getVTree() == null) {
            return sdd.getFactory().constant(getPhase());
        } else {
            final int varIdx = getVTree().getVariable();
            final Variable var = sdd.indexToVariable(varIdx);
            return getPhase() ? var : var.negate(sdd.getFactory());
        }
    }

    /**
     * Return the corresponding vtree leaf of this node. Return {@code null} if
     * this is a trivial node.
     * @return the vtree or {@code null}
     */
    @Override
    public VTreeLeaf getVTree() {
        if (isLiteral()) {
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
        return getVTree() == null && phase;
    }

    @Override
    public boolean isFalse() {
        return getVTree() == null && !phase;
    }

    @Override
    public boolean isLiteral() {
        return super.getVTree() != null;
    }

    @Override
    public boolean isDecomposition() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + id + ": " +
                (phase ? "+" : "-") +
                (getVTree() == null ? "trivial" : getVTree().asLeaf().getVariable())
                + " )";
    }
}
