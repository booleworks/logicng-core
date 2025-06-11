package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class SddNodeDecomposition extends SddNode implements Iterable<SddElement> {
    private final ArrayList<SddElement> elements;
    private int referenceCounter;

    SddNodeDecomposition(final int id, final Sdd.GSCacheEntry<VTree> vTree, final ArrayList<SddElement> elements) {
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

    private static BitSet calculateVariableMask(final Collection<SddElement> elements) {
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

    public List<SddElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public ArrayList<SddElement> getElementsUnsafe() {
        return elements;
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


    @Override
    public Iterator<SddElement> iterator() {
        return elements.iterator();
    }
}
