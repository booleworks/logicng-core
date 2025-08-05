package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class for SDD decomposition node. It stores a compressed and trimmed
 * partition of SDD elements.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddNodeDecomposition extends SddNode implements Iterable<SddElement> {
    private final ArrayList<SddElement> elements;
    private int referenceCounter;

    SddNodeDecomposition(final int id, final Sdd.GSCacheEntry<VTree> vTree, final ArrayList<SddElement> elements) {
        super(id, vTree);
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

    /**
     * Increases the reference counter of the node by one.
     * <p>
     * <strong>Do not use this function!</strong>  Use {@link Sdd#pin(SddNode)}
     * instead.
     */
    public void ref() {
        assert referenceCounter >= 0;
        referenceCounter++;
    }

    /**
     * Decreases the reference counter of the node by one.
     * <p>
     * <strong>Do not use this function!</strong> Use {@link Sdd#unpin(SddNode)}
     */
    public void deref() {
        assert referenceCounter >= 0;
        referenceCounter--;
        assert referenceCounter >= 0;
    }

    /**
     * Returns the current reference count of this node.
     * @return the current reference count of this node
     */
    public int getRefs() {
        return referenceCounter;
    }

    /**
     * Frees the resources and cached values of this node.
     * <p>
     * <strong>Do not use this function!</strong>
     */
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
        setSizeEntry(null);
    }

    /**
     * Returns a view to the SDD elements of this node.
     * @return a view to the SDD elements of this node
     */
    public List<SddElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    /**
     * Returns the list of SDD elements of this node. This function is unsafe
     * as it allows the user to modify the elements of this node, which is
     * undefined behaviour.
     * @return the list of SDD elements of this node
     */
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
