// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.bdds.jbuddy;

import static org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel.MARKHIDE;
import static org.logicng.knowledgecompilation.bdds.jbuddy.BDDTree.addRange;

import org.logicng.util.Pair;

import java.util.Arrays;
import java.util.Random;

/**
 * This class encapsulates the reordering mechanism on BDDs.
 * <p>
 * The class is initialized with the BDD kernel on which it performs all operations.
 * This means that all reordering operations initiated on this object will affect
 * <b>all BDDs created by its kernel</b>.
 * <p>
 * There are three types operations which can be performed here:
 * <ul>
 *     <li>Swapping two variables in the kernel can be performed via {@link #swapVariables}</li>
 *     <li>Reordering all variables can be performed via {@link #reorder}</li>
 *     <li>Reordering during construction of the BDD can be configured via {@link #setReorderDuringConstruction}</li>
 * </ul>
 * The last two operations only have an effect, if variable blocks were added. {@link #addVariableBlock(int, int, boolean) The docuentation}
 * gives more information on variable blocks.
 * To make all variables freely movable, {@link #addVariableBlockAll()} can be used.
 * @version 2.0.0
 * @since 2.0.0
 */
public class BDDReordering {

    protected final BDDKernel k;

    /* Current auto reord. method and number of automatic reorderings left */
    protected BDDReorderingMethod reorderMethod;
    protected int bddreorderTimes;

    /* Flag for disabling reordering temporarily */
    protected boolean reorderDisabled;

    /* Store for the variable relationships */
    protected BDDTree varTree;
    protected int blockId;

    /* Store for the ref.cou. of the external roots */
    protected int[] extRoots;
    protected int extRootSize;

    protected LevelData[] levels; /* Indexed by variable! */

    /* Interaction matrix */
    protected InteractionMatrix interactionMatrix;

    /* Number of live nodes before and after a reordering session */
    protected int usednumBefore;
    protected int usednumAfter;

    /* Flag telling us when a node table resize is done */
    protected boolean resizedInMakenode;

    protected int usedNodesNextReorder;

    /**
     * Creates a new reordering object for the given kernel.
     * @param k the kernel
     */
    public BDDReordering(final BDDKernel k) {
        this.k = k;
        init();
    }

    protected void init() {
        reorderDisabled = false;
        varTree = null;
        clrVarBlocks();
        setReorderDuringConstruction(BDDReorderingMethod.BDD_REORDER_NONE, 0);
        usednumBefore = 0;
        usednumAfter = 0;
        blockId = 0;
    }

    /**
     * Swaps the variables {@code v1} and {@code v2}. This affects all BDDs
     * created by {@link #k the kernel}.
     * @param v1 the first variable to swap
     * @param v2 the second variable to swap
     */
    public void swapVariables(int v1, int v2) {
        int l1, l2;
        /* Do not swap when variable-blocks are used */
        if (varTree != null) {
            throw new IllegalStateException("Swapping variables is not allowed with variable blocks");
        }

        /* Don't bother swapping x with x */
        if (v1 == v2) {
            return;
        }

        /* Make sure the variable exists */
        if (v1 < 0 || v1 >= k.varnum) {
            throw new IllegalArgumentException("Unknown variable number: " + v1);
        }

        if (v2 < 0 || v2 >= k.varnum) {
            throw new IllegalArgumentException("Unknown variable number: " + v2);
        }

        l1 = k.var2level[v1];
        l2 = k.var2level[v2];

        /* Make sure v1 is before v2 */
        if (l1 > l2) {
            final int tmp = v1;
            v1 = v2;
            v2 = tmp;
            l1 = k.var2level[v1];
            l2 = k.var2level[v2];
        }

        reorderInit();
        /* Move v1 to v2's position */
        while (k.var2level[v1] < l2) {
            reorderVardown(v1);
        }

        /* Move v2 to v1's position */
        while (k.var2level[v2] > l1) {
            reorderVarup(v2);
        }
        reorderDone();
    }

    /**
     * Reorders the levels in the kernel using the given reordering method.
     * Only blocks of variables will be reordered. See the documentation of
     * {@link #addVariableBlock} to learn more about such variable blocks.
     * Without the definition of any block, nothing will be reordered.
     * <p>
     * If the reordering should be performed without any restrictions,
     * {@link #addVariableBlockAll()} can be called before this method.
     * @param method the method to be used for the reordering
     */
    public void reorder(final BDDReorderingMethod method) {
        final BDDTree top;
        final BDDReorderingMethod savemethod = reorderMethod;
        final int savetimes = bddreorderTimes;
        reorderMethod = method;
        bddreorderTimes = 1;
        top = new BDDTree(-1);
        if (reorderInit() < 0) {
            return;
        }
        usednumBefore = k.nodesize - k.freenum;
        top.setFirst(0);
        top.setLast(k.varnum - 1);
        top.setFixed(false);
        top.setNext(null);
        top.setNextlevel(varTree);

        reorderBlock(top, method);
        varTree = top.getNextlevel();
        usednumAfter = k.nodesize - k.freenum;
        reorderDone();
        reorderMethod = savemethod;
        bddreorderTimes = savetimes;
    }

    /**
     * Activates or deactivates the automatic reordering during the construction of a BDD.
     * <p>
     * Automatic reordering can be deactivated by passing {@link BDDReorderingMethod#BDD_REORDER_NONE}
     * for the {@code method} parameter, otherwise the reordering is activated with the
     * given method. The reordering is executed at most {@code num} times.
     * @param method the method to be used for reordering
     * @param num    the maximum number of reorders to be performed
     */
    public void setReorderDuringConstruction(final BDDReorderingMethod method, final int num) {
        reorderMethod = method;
        bddreorderTimes = num;
    }

    /**
     * Adds a variable block starting at variable {@code first} and ending in variable
     * {@code last} (both inclusive).
     * <p>
     * <b>Variable blocks</b> are used in the {@link #reorder BDD reordering}
     * or in the automatic reordering during the construction of the BDD (configured by
     * {@link #setReorderDuringConstruction}). Variable blocks can be nested, i.e. one block can
     * contain an arbitrary number of ("child") blocks. Furthermore, a variable block can also
     * be a single variable.
     * <p>
     * During reordering, the child blocks of a parent block can be reordered, but they are kept
     * together. So no other block can be moved in between the child blocks. Furthermore,
     * variables in a block which are not in a child block will be left untouched.
     * <p>
     * Example: Lets assume we have a BDD with the variable ordering {@code v1, v2, v3, v4, v5, v6, v7}.
     * We create the following blocks:
     * <ul>
     *     <li>{@code A}  reaching from {@code v1} to {@code v5}</li>
     *     <li>{@code B}  reaching from {@code v6} to {@code v7}</li>
     *     <li>{@code A1} reaching from {@code v1} to {@code v2}</li>
     *     <li>{@code A2} reaching from {@code v3} to {@code v3}</li>
     *     <li>{@code A3} reaching from {@code v4} to {@code v5}</li>
     * </ul>
     * This means that the variables of {@code A} and {@code B} can never be mixed up in the order.
     * So during reordering the variables {@code v6} and {@code v7} can either be moved to the
     * front (before {@code A}) or remain at their position.
     * Furthermore, for example {@code v1} and {@code v2} will always stay together and neither
     * {@code v3} nor any other variable can be moved in between them. On the other hand, the blocks
     * {@code A1}, {@code A2}, and {@code A3} can be swapped arbitrarily.
     * <p>
     * These are valid result of a reordering based on the above blocks:
     * <ul>
     *     <li>{@code v3, v1, v2, v4, v5, v6, v7}</li>
     *     <li>{@code v6, v7, v4, v5, v3, v1, v2}</li>
     *     <li>{@code v6, v7, v1, v2, v3, v4, v5}</li>
     * </ul>
     * These however would be <b>illegal</b>:
     * <ul>
     *     <li>{@code v2, v1, v3, v4, v5, v6, v7} (variables in a block which are not in a child block will not be reordered)</li>
     *     <li>{@code v1, v3, v2, v4, v5, v6, v7} (variables of different block will not be mixed up)</li>
     * </ul>
     * <p>
     * If a block is <b>fixed</b> (the example above assumed always blocks which are not fixed), its
     * immediate child blocks will remain in their order. E.g. if block {@code A} was fixed, the blocks
     * {@code A1}, {@code A2}, and {@code A3} would not be allowed to be swapped.
     * Let's assume block {@code A} to be fixed and that we have two other unfixed blocks:
     * <ul>
     *     <li>{@code A11} reaching from {@code v1} to {@code v1}</li>
     *     <li>{@code A12} reaching from {@code v2} to {@code v2}</li>
     * </ul>
     * These are examples for <b>legal</b> reorderings:
     * <ul>
     *     <li>{@code v2, v1, v3, v4, v5, v6, v7} (block {@code A} is fixed, but "grandchildren" are still allowed to be reordered</li>
     *     <li>{@code v6, v7, v2, v1, v3, v4, v5}</li>
     * </ul>
     * These are examples for <b>illegal</b> reorderings:
     * <ul>
     *     <li>{@code v3, v2, v1, v4, v5, v6, v7} (block {@code A} is fixed, so it's child blocks must be be reordered</li>
     *     <li>{@code v1, v2, v4, v5, v3, v6, v7}</li>
     * </ul>
     * <p>
     * Each block (including all nested blocks) must be defined by a separate call to this method. The blocks
     * may be added in an arbitrary order, so it is not required to add them top-down or bottom-up.
     * However, the blocks <b>must not intersect</b>, except of one block containing the other. Furthermore,
     * both the {@code first} and the {@code last} variable must be known by the kernel and the level {@code first}
     * must be lower than the level of {@code last}.
     * @param first the variable at which the block starts (inclusive)
     * @param last  the variable at which the block ends (inclusive)
     * @param fixed whether the block should be fixed or not
     */
    public void addVariableBlock(final int first, final int last, final boolean fixed) {
        if (first < 0 || first >= k.varnum || last < 0 || last >= k.varnum) {
            throw new IllegalArgumentException("invalid var range from " + first + " to " + last);
        }
        final BDDTree t = addRange(varTree, first, last, fixed, blockId, k.level2var);
        if (t == null) {
            throw new IllegalStateException("Could not add range to tree");
        }
        varTree = t;
        blockId++;
    }

    /**
     * Adds a single variable block for all variables known by the kernel.
     */
    public void addVariableBlockAll() {
        for (int n = 0; n < k.varnum; n++) {
            addVariableBlock(n, n, false);
        }
    }

    /**
     * IMPORTANT:
     * The semantics of the "level" field in the BddNode struct changes during
     * variable reordering in order to make a fast variable swap possible when
     * two variables are independent. Instead of referring to the level of the node
     * it refers to the *variable* !!!
     * @param n the variable number
     * @return the level of this variable
     */
    protected int var(final int n) {
        return k.level(n);
    }

    protected int reorderNodenum() {
        return k.nodesize - k.freenum;
    }

    protected int nodehashReorder(final int var, final int l, final int h) {
        return Math.abs(k.pair(l, h) % levels[var].size) + levels[var].start;
    }

    protected void reorderBlock(final BDDTree t, final BDDReorderingMethod method) {
        BDDTree thisTree;
        if (t == null) {
            return;
        }
        if (!t.isFixed() && t.getNextlevel() != null) {
            switch (method) {
                case BDD_REORDER_WIN2:
                    t.setNextlevel(reorderWin2(t.getNextlevel()));
                    break;
                case BDD_REORDER_WIN2ITE:
                    t.setNextlevel(reorderWin2ite(t.getNextlevel()));
                    break;
                case BDD_REORDER_SIFT:
                    t.setNextlevel(reorderSift(t.getNextlevel()));
                    break;
                case BDD_REORDER_SIFTITE:
                    t.setNextlevel(reorderSiftite(t.getNextlevel()));
                    break;
                case BDD_REORDER_WIN3:
                    t.setNextlevel(reorderWin3(t.getNextlevel()));
                    break;
                case BDD_REORDER_WIN3ITE:
                    t.setNextlevel(reorderWin3ite(t.getNextlevel()));
                    break;
                case BDD_REORDER_RANDOM:
                    t.setNextlevel(reorderRandom(t.getNextlevel()));
                    break;
            }
        }
        for (thisTree = t.getNextlevel(); thisTree != null; thisTree = thisTree.getNext()) {
            reorderBlock(thisTree, method);
        }
        if (t.getSeq() != null) {
            // qsort(t->seq, t->last-t->first+1, sizeof(int), varseqCmp);
            t.setSeq(Arrays.stream(t.getSeq()).limit(t.getLast() - t.getFirst() + 1).boxed()
                    .sorted(this::varseqCmp)
                    .mapToInt(i -> i)
                    .toArray());
        }
    }

    protected int varseqCmp(final Integer aa, final Integer bb) {
        final int a = k.var2level[aa];
        final int b = k.var2level[bb];
        return Integer.compare(a, b);
    }

    protected void reorderDone() {
        for (int n = 0; n < extRootSize; n++) {
            k.setMark(extRoots[n]);
        }
        for (int n = 2; n < k.nodesize; n++) {
            if (k.marked(n)) {
                k.unmark(n);
            } else {
                k.setRefcou(n, 0);
            }
            /* This is where we go from .var to .level again! - Do NOT use the LEVEL macro here. */
            k.setLevel(n, k.var2level[k.level(n)]);
        }
        k.gbc();
    }

    protected BDDTree reorderWin2(final BDDTree t) {
        BDDTree thisTree = t;
        BDDTree first = t;
        if (t == null) {
            return null;
        }
        while (thisTree.getNext() != null) {
            final int best = reorderNodenum();
            blockdown(thisTree);
            if (best < reorderNodenum()) {
                blockdown(thisTree.getPrev());
                thisTree = thisTree.getNext();
            } else if (first == thisTree) {
                first = thisTree.getPrev();
            }
        }
        return first;
    }

    protected BDDTree reorderWin2ite(final BDDTree t) {
        BDDTree thisTree;
        BDDTree first = t;
        if (t == null) {
            return null;
        }
        int lastsize;

        do {
            lastsize = reorderNodenum();
            thisTree = t;
            while (thisTree.getNext() != null) {
                final int best = reorderNodenum();

                blockdown(thisTree);

                if (best < reorderNodenum()) {
                    blockdown(thisTree.getPrev());
                    thisTree = thisTree.getNext();
                } else if (first == thisTree) {
                    first = thisTree.getPrev();
                }
            }
        }
        while (reorderNodenum() != lastsize);

        return first;
    }

    protected BDDTree reorderWin3(final BDDTree t) {
        BDDTree thisTree = t;
        BDDTree first = t;

        if (t == null) {
            return null;
        }

        while (thisTree.getNext() != null) {
            final Pair<BDDTree, BDDTree> swapResult = reorderSwapwin3(thisTree);
            thisTree = swapResult.first();
            first = swapResult.second() != null ? swapResult.second() : first;
        }

        return first;
    }

    protected BDDTree reorderWin3ite(final BDDTree t) {
        BDDTree thisTree;
        BDDTree first = t;
        int lastsize;

        if (t == null) {
            return null;
        }

        do {
            lastsize = reorderNodenum();
            thisTree = first;

            while (thisTree.getNext() != null && thisTree.getNext().getNext() != null) {
                final Pair<BDDTree, BDDTree> swapResult = reorderSwapwin3(thisTree);
                thisTree = swapResult.first();
                first = swapResult.second() != null ? swapResult.second() : first;
            }
        }
        while (reorderNodenum() != lastsize);
        return first;
    }

    protected Pair<BDDTree, BDDTree> reorderSwapwin3(BDDTree thisTree) {
        BDDTree first = null;
        final boolean setfirst = thisTree.getPrev() == null;
        BDDTree next = thisTree;
        int best = reorderNodenum();

        if (thisTree.getNext().getNext() == null) /* Only two blocks left -> win2 swap */ {
            blockdown(thisTree);

            if (best < reorderNodenum()) {
                blockdown(thisTree.getPrev());
                next = thisTree.getNext();
            } else {
                if (setfirst) {
                    first = thisTree.getPrev();
                }
            }
        } else /* Real win3 swap */ {
            int pos = 0;
            blockdown(thisTree);  /* B A* C (4) */
            pos++;
            if (best > reorderNodenum()) {
                pos = 0;
                best = reorderNodenum();
            }

            blockdown(thisTree);  /* B C A* (3) */
            pos++;
            if (best > reorderNodenum()) {
                pos = 0;
                best = reorderNodenum();
            }

            thisTree = thisTree.getPrev().getPrev();
            blockdown(thisTree);  /* C B* A (2) */
            pos++;
            if (best > reorderNodenum()) {
                pos = 0;
                best = reorderNodenum();
            }

            blockdown(thisTree);  /* C A B* (1) */
            pos++;
            if (best > reorderNodenum()) {
                pos = 0;
                best = reorderNodenum();
            }

            thisTree = thisTree.getPrev().getPrev();
            blockdown(thisTree);  /* A C* B (0)*/
            pos++;
            if (best > reorderNodenum()) {
                pos = 0;
            }

            if (pos >= 1)  /* A C B -> C A* B */ {
                thisTree = thisTree.getPrev();
                blockdown(thisTree);
                next = thisTree;
                if (setfirst) {
                    first = thisTree.getPrev();
                }
            }

            if (pos >= 2)  /* C A B -> C B A* */ {
                blockdown(thisTree);
                next = thisTree.getPrev();
                if (setfirst) {
                    first = thisTree.getPrev().getPrev();
                }
            }

            if (pos >= 3)  /* C B A -> B C* A */ {
                thisTree = thisTree.getPrev().getPrev();
                blockdown(thisTree);
                next = thisTree;
                if (setfirst) {
                    first = thisTree.getPrev();
                }
            }

            if (pos >= 4)  /* B C A -> B A C* */ {
                blockdown(thisTree);
                next = thisTree.getPrev();
                if (setfirst) {
                    first = thisTree.getPrev().getPrev();
                }
            }

            if (pos >= 5)  /* B A C -> A B* C */ {
                thisTree = thisTree.getPrev().getPrev();
                blockdown(thisTree);
                next = thisTree;
                if (setfirst) {
                    first = thisTree.getPrev();
                }
            }
        }
        return new Pair<>(next, first);
    }

    /**
     * Do sifting iteratively until no more improvement can be found
     * @param t the input BDD tree
     * @return the sifted BDD tree
     */
    protected BDDTree reorderSiftite(final BDDTree t) {
        BDDTree first = t;
        int lastsize;

        if (t == null) {
            return null;
        }

        do {
            lastsize = reorderNodenum();
            first = reorderSift(first);
        }
        while (reorderNodenum() != lastsize);

        return first;
    }

    /**
     * Find sifting sequence based on the number of nodes at each level
     * @param t the input BDD tree
     * @return the sifted BDD tree
     */
    protected BDDTree reorderSift(BDDTree t) {
        BDDTree thisTree;
        final BDDTree[] seq;
        final BDDSizePair[] p;
        int n, num;

        for (thisTree = t, num = 0; thisTree != null; thisTree = thisTree.getNext()) {
            thisTree.setPos(num++);
        }

        p = new BDDSizePair[num];
        for (int i = 0; i < p.length; i++) {
            p[i] = new BDDSizePair();
        }
        seq = new BDDTree[num];

        for (thisTree = t, n = 0; thisTree != null; thisTree = thisTree.getNext(), n++) {
            int v;

            /* Accumulate number of nodes for each block */
            p[n].val = 0;
            for (v = thisTree.getFirst(); v <= thisTree.getLast(); v++) {
                p[n].val = p[n].val - levels[v].nodenum;
            }

            p[n].block = thisTree;
        }

        /* Sort according to the number of nodes at each level */
        Arrays.sort(p, 0, num, this::siftTestCmp);

        /* Create sequence */
        for (n = 0; n < num; n++) {
            seq[n] = p[n].block;
        }

        /* Do the sifting on this sequence */
        t = reorderSiftSeq(t, seq, num);

        return t;
    }

    /**
     * Go through all blocks in a specific sequence and find best
     * position for each of them
     * @param t   the input BDD tree
     * @param seq the sequence
     * @param num the current position in the sequence
     * @return the sifted BDD tree
     */
    protected BDDTree reorderSiftSeq(final BDDTree t, final BDDTree[] seq, final int num) {
        BDDTree thisTree;
        int n;

        if (t == null) {
            return null;
        }

        for (n = 0; n < num; n++) {
            reorderSiftBestpos(seq[n], num / 2);
        }

        /* Find first block */
        for (thisTree = t; thisTree.getPrev() != null; thisTree = thisTree.getPrev()) {
            /* nil */
        }

        return thisTree;
    }

    /**
     * Move a specific block up and down in the order and place at last in
     * the best position
     * @param blk       the block
     * @param middlePos the middle position in the block
     */
    protected void reorderSiftBestpos(final BDDTree blk, final int middlePos) {
        int best = reorderNodenum();
        int maxAllowed = best / 5 + best;
        int bestpos = 0;
        boolean dirIsUp = true;
        int n;

        /* Determine initial direction */
        if (blk.getPos() > middlePos) {
            dirIsUp = false;
        }

        /* Move block back and forth */
        for (n = 0; n < 2; n++) {
            boolean first = true;

            if (dirIsUp) {
                while (blk.getPrev() != null &&
                        (reorderNodenum() <= maxAllowed || first)) {
                    first = false;
                    blockdown(blk.getPrev());
                    bestpos--;

                    if (reorderNodenum() < best) {
                        best = reorderNodenum();
                        bestpos = 0;
                        maxAllowed = best / 5 + best;
                    }
                }
            } else {
                while (blk.getNext() != null &&
                        (reorderNodenum() <= maxAllowed || first)) {
                    first = false;
                    blockdown(blk);
                    bestpos++;

                    if (reorderNodenum() < best) {
                        best = reorderNodenum();
                        bestpos = 0;
                        maxAllowed = best / 5 + best;
                    }
                }
            }
            dirIsUp = !dirIsUp;
        }

        /* Move to best pos */
        while (bestpos < 0) {
            blockdown(blk);
            bestpos++;
        }
        while (bestpos > 0) {
            blockdown(blk.getPrev());
            bestpos--;
        }
    }

    protected int siftTestCmp(final BDDSizePair a, final BDDSizePair b) {
        return Integer.compare(a.val, b.val);
    }

    /*
     * === Random reordering (mostly for debugging and test ) =============
     */
    protected BDDTree reorderRandom(final BDDTree t) {
        BDDTree thisTree;
        final BDDTree[] seq;
        int n, num = 0;

        if (t == null) {
            return null;
        }

        for (thisTree = t; thisTree != null; thisTree = thisTree.getNext()) {
            num++;
        }
        seq = new BDDTree[num];
        for (thisTree = t, num = 0; thisTree != null; thisTree = thisTree.getNext()) {
            seq[num++] = thisTree;
        }

        final Random random = new Random(42);
        for (n = 0; n < 4 * num; n++) {
            final int blk = random.nextInt(num);
            if (seq[blk].getNext() != null) {
                blockdown(seq[blk]);
            }
        }

        /* Find first block */
        for (thisTree = t; thisTree.getPrev() != null; thisTree = thisTree.getPrev()) {
            /* nil */
        }

        return thisTree;
    }

    /**
     * Swaps adjacent blocks
     * @param left the left BDD tree
     */
    protected void blockdown(final BDDTree left) {
        final BDDTree right = left.getNext();
        int n;
        final int leftsize = left.getLast() - left.getFirst();
        final int rightsize = right.getLast() - right.getFirst();
        final int leftstart = k.var2level[left.getSeq()[0]];
        final int[] lseq = left.getSeq();
        final int[] rseq = right.getSeq();

        /* Move left past right */
        while (k.var2level[lseq[0]] < k.var2level[rseq[rightsize]]) {
            for (n = 0; n < leftsize; n++) {
                if (k.var2level[lseq[n]] + 1 != k.var2level[lseq[n + 1]] && k.var2level[lseq[n]] < k.var2level[rseq[rightsize]]) {
                    reorderVardown(lseq[n]);
                }
            }

            if (k.var2level[lseq[leftsize]] < k.var2level[rseq[rightsize]]) {
                reorderVardown(lseq[leftsize]);
            }
        }

        /* Move right to where left started */
        while (k.var2level[rseq[0]] > leftstart) {
            for (n = rightsize; n > 0; n--) {
                if (k.var2level[rseq[n]] - 1 != k.var2level[rseq[n - 1]] && k.var2level[rseq[n]] > leftstart) {
                    reorderVarup(rseq[n]);
                }
            }

            if (k.var2level[rseq[0]] > leftstart) {
                reorderVarup(rseq[0]);
            }
        }

        /* Swap left and right data in the order */
        left.setNext(right.getNext());
        right.setPrev(left.getPrev());
        left.setPrev(right);
        right.setNext(left);

        if (right.getPrev() != null) {
            right.getPrev().setNext(right);
        }
        if (left.getNext() != null) {
            left.getNext().setPrev(left);
        }
        n = left.getPos();
        left.setPos(right.getPos());
        right.setPos(n);
    }

    protected void reorderVarup(final int var) {
        if (var < 0 || var >= k.varnum) {
            throw new IllegalStateException("Illegal variable in reordering");
        }
        if (k.var2level[var] != 0) {
            reorderVardown(k.level2var[k.var2level[var] - 1]);
        }
    }

    protected void reorderVardown(final int var) {
        int n;
        final int level;
        if (var < 0 || var >= k.varnum) {
            throw new IllegalStateException("Illegal variable in reordering");
        }
        level = k.var2level[var];
        if (level >= k.varnum - 1) {
            return;
        }
        resizedInMakenode = false;

        if (interactionMatrix.depends(var, k.level2var[level + 1]) > 0) {
            final int toBeProcessed = reorderDownSimple(var);
            reorderSwap(toBeProcessed, var);
            reorderLocalGbc(var);
        }

        /* Swap the var<->level tables */
        n = k.level2var[level];
        k.level2var[level] = k.level2var[level + 1];
        k.level2var[level + 1] = n;
        n = k.var2level[var];
        k.var2level[var] = k.var2level[k.level2var[level]];
        k.var2level[k.level2var[level]] = n;
        /* Update all rename pairs */
        //        this.pairs.vardown(level);

        if (resizedInMakenode) {
            reorderRehashAll();
        }
    }

    protected int reorderDownSimple(final int var0) {
        int toBeProcessed = 0;
        final int var1 = k.level2var[k.var2level[var0] + 1];
        final int vl0 = levels[var0].start;
        final int size0 = levels[var0].size;
        int n;

        levels[var0].nodenum = 0;

        for (n = 0; n < size0; n++) {
            int r;
            r = k.hash(n + vl0);
            k.setHash(n + vl0, 0);
            while (r != 0) {
                final int next = k.next(r);
                if (var(k.low(r)) != var1 && var(k.high(r)) != var1) {
                    /* Node does not depend on next var, let it stay in the chain */
                    k.setNext(r, k.hash(n + vl0));
                    k.setHash(n + vl0, r);
                    levels[var0].nodenum++;
                } else {
                    /* Node depends on next var - save it for later procesing */
                    k.setNext(r, toBeProcessed);
                    toBeProcessed = r;
                }
                r = next;
            }
        }
        return toBeProcessed;
    }

    protected void reorderSwap(int toBeProcessed, final int var0) {
        final int var1 = k.level2var[k.var2level[var0] + 1];
        while (toBeProcessed > 0) {
            final int next = k.next(toBeProcessed);
            int f0 = k.low(toBeProcessed);
            int f1 = k.high(toBeProcessed);
            final int f00;
            final int f01;
            final int f10;
            final int f11;
            final int hash;

            /* Find the cofactors for the new nodes */
            if (var(f0) == var1) {
                f00 = k.low(f0);
                f01 = k.high(f0);
            } else {
                f00 = f01 = f0;
            }
            if (var(f1) == var1) {
                f10 = k.low(f1);
                f11 = k.high(f1);
            } else {
                f10 = f11 = f1;
            }

            /* Note: makenode does refcou. */
            f0 = reorderMakenode(var0, f00, f10);
            f1 = reorderMakenode(var0, f01, f11);
            //            assert node == this.nodes[toBeProcessed];
            //            node = this.nodes[toBeProcessed];  /* Might change in makenode [SHi: why? I don't think so] */

            /* We know that the refcou of the grandchilds of this node
             * is greater than one (these are f00...f11), so there is
             * no need to do a recursive refcou decrease. It is also
             * possible for the LOWp(node)/high nodes to come alive again,
             * so deref. of the childs is delayed until the local GBC. */

            k.decRef(k.low(toBeProcessed));
            k.decRef(k.high(toBeProcessed));

            /* Update in-place */
            k.setLevel(toBeProcessed, var1);
            k.setLow(toBeProcessed, f0);
            k.setHigh(toBeProcessed, f1);
            levels[var1].nodenum++;
            /* Rehash the node since it got new childs */
            hash = nodehashReorder(var(toBeProcessed), k.low(toBeProcessed), k.high(toBeProcessed));
            k.setNext(toBeProcessed, k.hash(hash));
            k.setHash(hash, toBeProcessed);
            toBeProcessed = next;
        }
    }

    protected int reorderMakenode(final int var, final int low, final int high) {
        final int hash;
        int res;

        /* Note: We know that low,high has a refcou greater than zero, so there is no need to add reference *recursively* */
        /* check whether childs are equal */
        if (low == high) {
            k.incRef(low);
            return low;
        }

        /* Try to find an existing node of this kind */
        hash = nodehashReorder(var, low, high);
        res = k.hash(hash);

        while (res != 0) {
            if (k.low(res) == low && k.high(res) == high) {
                k.incRef(res);
                return res;
            }
            res = k.next(res);
        }
        /* No existing node -> build one */
        /* Any free nodes to use ? */
        if (k.freepos == 0) {
            /* Try to allocate more nodes - call noderesize without
             * enabling rehashing.
             * Note: if ever rehashing is allowed here, then remember to
             * update local variable "hash" */
            k.nodeResize(false);
            resizedInMakenode = true;
            assert k.freepos > 0;
        }

        /* Build new node */
        res = k.freepos;
        k.freepos = k.next(k.freepos);
        levels[var].nodenum++;
        k.produced++;
        k.freenum--;

        k.setLevel(res, var);
        k.setLow(res, low);
        k.setHigh(res, high);

        /* Insert node in hash chain */
        k.setNext(res, k.hash(hash));
        k.setHash(hash, res);

        /* Make sure it is reference counted */
        k.setRefcou(res, 1);
        k.incRef(k.low(res));
        k.incRef(k.high(res));
        return res;
    }

    protected void reorderLocalGbc(final int var0) {
        final int var1 = k.level2var[k.var2level[var0] + 1];
        final int vl1 = levels[var1].start;
        final int size1 = levels[var1].size;
        int n;

        for (n = 0; n < size1; n++) {
            final int hash = n + vl1;
            int r = k.hash(hash);
            k.setHash(hash, 0);
            while (r > 0) {
                final int next = k.next(r);

                if (k.refcou(r) > 0) {
                    k.setNext(r, k.hash(hash));
                    k.setHash(hash, r);
                } else {
                    k.decRef(k.low(r));
                    k.decRef(k.high(r));
                    k.setLow(r, -1);
                    k.setNext(r, k.freepos);
                    k.freepos = r;
                    levels[var1].nodenum--;
                    k.freenum++;
                }
                r = next;
            }
        }
    }

    protected void reorderRehashAll() {
        int n;
        reorderSetLevellookup();
        k.freepos = 0;
        for (n = k.nodesize - 1; n >= 0; n--) {
            k.setHash(n, 0);
        }
        for (n = k.nodesize - 1; n >= 2; n--) {
            if (k.refcou(n) > 0) {
                final int hash = nodehashReorder(var(n), k.low(n), k.high(n));
                k.setNext(n, k.hash(hash));
                k.setHash(hash, n);
            } else {
                k.setNext(n, k.freepos);
                k.freepos = n;
            }
        }
    }

    protected void reorderSetLevellookup() {
        int n;
        for (n = 0; n < k.varnum; n++) {
            levels[n].maxsize = k.nodesize / k.varnum;
            levels[n].start = n * levels[n].maxsize;
            levels[n].size = levels[n].maxsize;
            if (levels[n].size >= 4) {
                levels[n].size = k.getPrime().primeLTE(levels[n].size);
            }
        }
    }

    protected void clrVarBlocks() {
        varTree = null;
        blockId = 0;
    }

    protected void disableReorder() {
        reorderDisabled = true;
    }

    protected void enableReorder() {
        reorderDisabled = false;
    }

    protected boolean reorderReady() {
        return reorderMethod != BDDReorderingMethod.BDD_REORDER_NONE && varTree != null && bddreorderTimes != 0 && !reorderDisabled;
    }

    protected void reorderAuto() {
        if (!reorderReady()) {
            return;
        }
        reorder(reorderMethod);
        bddreorderTimes--;
    }

    protected int reorderInit() {
        levels = new LevelData[k.varnum];
        for (int n = 0; n < k.varnum; n++) {
            levels[n] = new LevelData();
            levels[n].start = -1;
            levels[n].size = 0;
            levels[n].nodenum = 0;
        }
        /* First mark and recursive refcou. all roots and childs. Also do some
         * setup here for both setLevellookup and reorder_gbc */
        if (markRoots() < 0) {
            return -1;
        }
        /* Initialize the hash tables */
        reorderSetLevellookup();
        /* Garbage collect and rehash to new scheme */
        reorderGbc();
        return 0;
    }

    protected int markRoots() {
        final int[] dep = new int[k.varnum];
        extRootSize = 0;
        for (int n = 2; n < k.nodesize; n++) {
            /* This is where we go from .level to .var! - Do NOT use the LEVEL macro here. */
            k.setLevel(n, k.level2var[k.level(n)]);
            if (k.refcou(n) > 0) {
                extRootSize++;
                k.setMark(n);
            }
        }
        extRoots = new int[extRootSize];
        interactionMatrix = new InteractionMatrix(k.varnum);
        extRootSize = 0;
        for (int n = 2; n < k.nodesize; n++) {
            if (k.marked(n)) {
                k.unmarkNode(n);
                extRoots[extRootSize++] = n;
                dep[var(n)] = 1;
                levels[var(n)].nodenum++;
                addrefRec(k.low(n), dep);
                addrefRec(k.high(n), dep);
                addDependencies(dep);
            }
            /* Make sure the hash field is empty. This saves a loop in the initial GBC */
            k.setHash(n, 0);
        }
        k.setHash(0, 0);
        k.setHash(1, 0);
        return 0;
    }

    protected void reorderGbc() {
        k.freepos = 0;
        k.freenum = 0;
        /* No need to zero all hash fields - this is done in mark_roots */
        for (int n = k.nodesize - 1; n >= 2; n--) {
            if (k.refcou(n) > 0) {
                final int hash = nodehashReorder(var(n), k.low(n), k.high(n));
                k.setNext(n, k.hash(hash));
                k.setHash(hash, n);
            } else {
                k.setLow(n, -1);
                k.setNext(n, k.freepos);
                k.freepos = n;
                k.freenum++;
            }
        }
    }

    protected void checkReorder() {
        reorderAuto();
        /* Do not reorder before twice as many nodes have been used */
        usedNodesNextReorder = 2 * (k.nodesize - k.freenum);
        /* And if very little was gained this time (< 20%) then wait until
         * even more nodes (upto twice as many again) have been used */
        if (reorderGain() < 20) {
            usedNodesNextReorder += (usedNodesNextReorder * (20 - reorderGain())) / 20;
        }
    }

    protected void addrefRec(final int r, final int[] dep) {
        if (r < 2) {
            return;
        }
        if (k.refcou(r) == 0) {
            k.freenum--;

            /* Detect variable dependencies for the interaction matrix */
            dep[var(r) & MARKHIDE] = 1;

            /* Make sure the nodenum field is updated. Used in the initial GBC */
            levels[var(r) & MARKHIDE].nodenum++;

            addrefRec(k.low(r), dep);
            addrefRec(k.high(r), dep);
        } else {
            int n;

            /* Update (from previously found) variable dependencies
             * for the interaction matrix */
            for (n = 0; n < k.varnum; n++) {
                dep[n] |= interactionMatrix.depends(var(r) & MARKHIDE, n);
            }
        }
        k.incRef(r);
    }

    protected void addDependencies(final int[] dep) {
        for (int n = 0; n < k.varnum; n++) {
            for (int m = n; m < k.varnum; m++) {
                if (dep[n] > 0 && dep[m] > 0) {
                    interactionMatrix.set(n, m);
                    interactionMatrix.set(m, n);
                }
            }
        }
    }

    protected int reorderGain() {
        if (usednumBefore == 0) {
            return 0;
        }
        return (100 * (usednumBefore - usednumAfter)) / usednumBefore;
    }

    /* Level data */
    protected static class LevelData {
        protected int start;    /* Start of this sub-table (entry in "bddnodes") */
        protected int size;     /* Size of this sub-table */
        protected int maxsize;  /* Max. allowed size of sub-table */
        protected int nodenum;  /* Number of nodes in this level */
    }

    protected static class BDDSizePair {
        protected int val;
        protected BDDTree block;
    }

    protected static class InteractionMatrix {
        protected final int[][] rows;

        protected InteractionMatrix(final int size) {
            rows = new int[size][];
            for (int n = 0; n < size; n++) {
                rows[n] = new int[size / 8 + 1];
            }
        }

        protected void set(final int a, final int b) {
            rows[a][b / 8] |= 1 << (b % 8);
        }

        protected int depends(final int a, final int b) {
            return rows[a][b / 8] & (1 << (b % 8));
        }
    }
}
