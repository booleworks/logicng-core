// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

/**
 * A BDD tree used to represent nested variables blocks or variable reorderings.
 * @version 2.0.0
 * @since 2.0.0
 */
public class BddTree {
    protected int first;
    protected int last; // First and last variable in this block
    protected int pos; // Sifting position
    protected int[] seq; // Sequence of first...last in the current order
    // Are the sub-blocks fixed or may they be reordered
    protected boolean fixed;
    protected final int id; // A sequential id number given by addblock
    protected BddTree next;
    protected BddTree prev;
    protected BddTree nextlevel;

    /**
     * Constructs a new BDD tree with the given id.
     * @param id the id
     */
    public BddTree(final int id) {
        this.id = id;
        first = -1;
        last = -1;
        fixed = true;
        next = null;
        prev = null;
        nextlevel = null;
        seq = null;
    }

    /**
     * Adds a new range in the tree.
     * @param tree      the tree in which the range should be added
     * @param first     the start of the range
     * @param last      the end of the range
     * @param fixed     whether the range should be fixed or not
     * @param id        the id of the tree
     * @param level2var the level to variable mapping
     * @return the (possibly changed) BDD tree
     */
    public static BddTree addRange(final BddTree tree, final int first, final int last, final boolean fixed,
                                   final int id, final int[] level2var) {
        return addRangeRec(tree, null, first, last, fixed, id, level2var);
    }

    /**
     * Returns the first variable of this block.
     * @return the first variable of this block
     */
    public int getFirst() {
        return first;
    }

    /**
     * Sets the first variable of this block.
     * @param first the first variable of this block
     */
    public void setFirst(final int first) {
        this.first = first;
    }

    /**
     * Returns the last variable of this block.
     * @return the last variable of this block
     */
    public int getLast() {
        return last;
    }

    /**
     * Sets the last variable of this block.
     * @param last the last variable of this block
     */
    public void setLast(final int last) {
        this.last = last;
    }

    /**
     * Returns the sifting position.
     * @return the sifting position
     */
    public int getPos() {
        return pos;
    }

    /**
     * Sets the sifting position.
     * @param pos the sifting position
     */
    public void setPos(final int pos) {
        this.pos = pos;
    }

    /**
     * Returns the sequence of variables between {@code first} and {@code last}
     * in the current order.
     * @return the sequence of variables
     */
    public int[] getSeq() {
        return seq;
    }

    /**
     * Sets the sequence of variables between {@code first} and {@code last} in
     * the current order.
     * @param seq the sequence of variables
     */
    public void setSeq(final int[] seq) {
        this.seq = seq;
    }

    /**
     * Returns whether this block is fixed or not.
     * @return whether this block is fixed or not
     */
    public boolean isFixed() {
        return fixed;
    }

    /**
     * Sets whether this block is fixed or not.
     * @param fixed whether this block is fixed or not
     */
    public void setFixed(final boolean fixed) {
        this.fixed = fixed;
    }

    /**
     * Returns the id of this block.
     * @return the id of this block
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the next tree or {@code null} if no such tree exists.
     * @return the next tree
     */
    public BddTree getNext() {
        return next;
    }

    /**
     * Sets the next tree.
     * @param next the next tree
     */
    public void setNext(final BddTree next) {
        this.next = next;
    }

    /**
     * Returns the previous tree or {@code null} if no such tree exists.
     * @return the previous tree
     */
    public BddTree getPrev() {
        return prev;
    }

    /**
     * Sets the previous tree.
     * @param prev the previous tree
     */
    public void setPrev(final BddTree prev) {
        this.prev = prev;
    }

    /**
     * Returns the tree of the next level or {@code null} if no such tree
     * exists.
     * @return the tree of the next level
     */
    public BddTree getNextlevel() {
        return nextlevel;
    }

    /**
     * Sets the tree of the next level.
     * @param nextlevel the tree of the next level
     */
    public void setNextlevel(final BddTree nextlevel) {
        this.nextlevel = nextlevel;
    }

    /**
     * Adds a new range in the tree.
     * @param t         the tree in which the range should be added
     * @param prev      the predecessor if t is {@code null}
     * @param first     the start of the range
     * @param last      the end of the range
     * @param fixed     whether the range should be fixed or not
     * @param id        the id of the tree
     * @param level2var the level to variable mapping
     * @return the (possibly changed) BDD tree
     */
    public static BddTree addRangeRec(BddTree t, final BddTree prev, final int first, final int last,
                                      final boolean fixed, final int id, final int[] level2var) {
        if (first < 0 || last < 0 || last < first) {
            return null;
        }

        // Empty tree -> build one
        if (t == null) {
            t = new BddTree(id);
            t.first = first;
            t.fixed = fixed;
            t.seq = new int[last - first + 1];
            t.last = last;
            t.updateSeq(level2var);
            t.prev = prev;
            return t;
        }

        // Check for identity
        if (first == t.first && last == t.last) {
            return t;
        }

        // Before this section -> insert
        if (last < t.first) {
            final BddTree tnew = new BddTree(id);
            tnew.first = first;
            tnew.last = last;
            tnew.fixed = fixed;
            tnew.seq = new int[last - first + 1];
            tnew.updateSeq(level2var);
            tnew.next = t;
            tnew.prev = t.prev;
            t.prev = tnew;
            return tnew;
        }

        // After this section -> go to next
        if (first > t.last) {
            final BddTree next = addRangeRec(t.next, t, first, last, fixed, id, level2var);
            if (next != null) {
                t.next = next;
            }
            return t;
        }

        // Inside this section -> insert in next level
        if (first >= t.first && last <= t.last) {
            final BddTree nextlevel = addRangeRec(t.nextlevel, null, first, last, fixed, id, level2var);
            if (nextlevel != null) {
                t.nextlevel = nextlevel;
            }
            return t;
        }

        // Covering this section -> insert above this level
        if (first <= t.first) {
            final BddTree tnew;
            BddTree thisTree = t;

            while (true) {
                // Partial cover ->error
                if (last >= thisTree.first && last < thisTree.last) {
                    return null;
                }
                if (thisTree.next == null || last < thisTree.next.first) {
                    tnew = new BddTree(id);
                    tnew.first = first;
                    tnew.last = last;
                    tnew.fixed = fixed;
                    tnew.seq = new int[last - first + 1];
                    tnew.updateSeq(level2var);
                    tnew.nextlevel = t;
                    tnew.next = thisTree.next;
                    tnew.prev = t.prev;
                    if (thisTree.next != null) {
                        thisTree.next.prev = tnew;
                    }
                    thisTree.next = null;
                    t.prev = null;
                    return tnew;
                }
                thisTree = thisTree.next;
            }
        }
        // partial cover
        return null;
    }

    protected void updateSeq(final int[] bddvar2level) {
        int n;
        int low = first;

        for (n = first; n <= last; n++) {
            if (bddvar2level[n] < bddvar2level[low]) {
                low = n;
            }
        }

        for (n = first; n <= last; n++) {
            seq[bddvar2level[n] - bddvar2level[low]] = n;
        }
    }
}
