package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

public final class SddNodeDecomposition extends SddNode {
    private final SortedSet<SddElement> elements;
    private int referenceCounter;

    SddNodeDecomposition(final int id, final Sdd.CacheEntry<VTree> vTree, final SortedSet<SddElement> elements) {
        super(id, vTree, calculateVariableMask(elements));
        this.elements = elements;
        this.referenceCounter = 0;
        for (final SddElement element : elements) {
            if (element.getPrime().isDecomposition()) {
                element.getPrime().asDecomposition().ref();
            }
            if (element.getSub().isDecomposition()) {
                element.getSub().asDecomposition().ref();
            }
        }
    }

    private static BitSet calculateVariableMask(final Set<SddElement> elements) {
        final BitSet variableMask = new BitSet();
        for (final SddElement element : elements) {
            variableMask.or(element.getPrime().getVariableMask());
            variableMask.or(element.getSub().getVariableMask());
        }
        return variableMask;
    }

    public void ref() {
        assert referenceCounter >= 0;
        referenceCounter++;
    }

    public void deref() {
        assert referenceCounter >= 0;
        referenceCounter--;
        assert referenceCounter >= 0;
    }

    public int getRefs() {
        return referenceCounter;
    }

    public void free() {
        referenceCounter = -1;
        for (final SddElement element : elements) {
            if (element.getPrime().isDecomposition()) {
                element.getPrime().asDecomposition().deref();
            }
            if (element.getSub().isDecomposition()) {
                element.getSub().asDecomposition().deref();
            }
        }
        final SddNode negation = getNegationEntry() == null ? null : getNegationEntry().getElement();
        if (negation != null) {
            if (negation.getNegationEntry() != null) {
                negation.setNegationEntry(null);
            }
            setNegationEntry(null);
        }
    }

    public SortedSet<SddElement> getElements() {
        return Collections.unmodifiableSortedSet(elements);
    }

    @Override
    public boolean isTrivial() {
        return false;
    }

    @Override
    public boolean isTrue() {
        return false;
    }

    @Override
    public boolean isFalse() {
        return false;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isDecomposition() {
        return true;
    }

    @Override
    public String toString() {
        return "(" + id + ": " + elements + " )";
    }


}
