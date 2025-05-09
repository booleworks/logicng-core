package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

public class SddNodeTerminal extends SddNode {
    private final Formula terminal;

    public SddNodeTerminal(final int id, final VTree vtree, final Formula terminal) {
        super(id, vtree);
        assert vtree == null || vtree.isLeaf();
        this.terminal = terminal;
    }

    public Formula getTerminal() {
        return terminal;
    }

    @Override
    public boolean isTrivial() {
        return isTrue() || isFalse();
    }

    @Override
    public boolean isTrue() {
        return terminal.getType() == FType.TRUE;
    }

    @Override
    public boolean isFalse() {
        return terminal.getType() == FType.FALSE;
    }

    @Override
    public boolean isLiteral() {
        return terminal.getType() == FType.LITERAL;
    }

    @Override
    public boolean isDecomposition() {
        return false;
    }

    @Override
    public String toString() {
        return "(" + id + ": " + terminal + " )";
    }
}
