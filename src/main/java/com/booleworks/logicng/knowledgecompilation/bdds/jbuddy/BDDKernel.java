// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * ========================================================================
 * Copyright (C) 1996-2002 by Jorn Lind-Nielsen All rights reserved
 *
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, reproduce, prepare derivative works, distribute, and
 * display this software and its documentation for any purpose, provided that
 * (1) the above copyright notice and the following two paragraphs appear in all
 * copies of the source code and (2) redistributions, including without
 * limitation binaries, reproduce these notices in the supporting documentation.
 * Substantial modifications to this software may be copyrighted by their
 * authors and need not follow the licensing terms described here, provided that
 * the new terms are clearly indicated in all files where they apply.
 *
 * IN NO EVENT SHALL JORN LIND-NIELSEN, OR DISTRIBUTORS OF THIS SOFTWARE BE
 * LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR
 * CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 * DOCUMENTATION, EVEN IF THE AUTHORS OR ANY OF THE ABOVE PARTIES HAVE BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JORN LIND-NIELSEN SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS,
 * AND THE AUTHORS AND DISTRIBUTORS HAVE NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * ========================================================================
 */

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.BDDHandler;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The jBuddy kernel.
 * @version 2.0.0
 * @since 1.4.0
 */
public class BDDKernel {

    public static final int BDD_ABORT = -1;
    public static final int BDD_TRUE = 1;
    public static final int BDD_FALSE = 0;

    public static final int MAXVAR = 0x1FFFFF;
    public static final int MAXREF = 0x3FF;
    public static final int MARKON = 0x200000;
    public static final int MARKOFF = 0x1FFFFF;
    public static final int MARKHIDE = 0x1FFFFF;

    protected enum Operand {
        AND(0, new int[]{0, 0, 0, 1}),
        OR(2, new int[]{0, 1, 1, 1}),
        IMP(5, new int[]{1, 1, 0, 1}),
        EQUIV(6, new int[]{1, 0, 0, 1}),
        NOT(10, new int[]{1, 1, 0, 0});

        final int v;
        private final int[] tt;

        Operand(final int value, final int[] truthTable) {
            v = value;
            tt = truthTable;
        }
    }

    public static final int CACHEID_RESTRICT = 0x1;
    public static final int CACHEID_SATCOU = 0x2;
    public static final int CACHEID_PATHCOU_ONE = 0x4;
    public static final int CACHEID_PATHCOU_ZERO = 0x8;
    public static final int CACHEID_FORALL = 0x1;

    protected final BDDPrime prime;
    protected final FormulaFactory f;
    protected final SortedMap<Variable, Integer> var2idx;
    protected final SortedMap<Integer, Variable> idx2var;

    protected BDDReordering reordering;

    protected int[] nodes; // All the bdd nodes
    protected int[] vars; // Set of defined BDD variables
    // Minimal % of nodes that has to be left after a garbage collection
    protected final int minfreenodes;
    protected int gbcollectnum; // Number of garbage collections
    protected final int cachesize; // Size of the operator caches
    protected int nodesize; // Number of allocated nodes
    protected final int maxnodeincrease; // Max. # of nodes used to inc. table
    protected int freepos; // First free node
    protected int freenum; // Number of free nodes
    protected long produced; // Number of new nodes ever produced
    protected int varnum; // Number of defined BDD variables
    protected int[] refstack; // Internal node reference stack
    protected int refstacktop; // Internal node reference stack top
    protected int[] level2var; // Level -> variable table
    protected int[] var2level; // Variable -> level table

    protected int[] quantvarset; // Current variable set for quant.
    protected int quantvarsetID; // Current id used in quantvarset
    protected int quantlast; // Current last variable to be quant.

    protected BDDCache applycache; // Cache for apply results
    protected BDDCache itecache; // Cache for ITE results
    protected BDDCache quantcache; // Cache for exist/forall results
    protected BDDCache appexcache; // Cache for appex/appall results
    protected BDDCache replacecache; // Cache for replace results
    protected BDDCache misccache; // Cache for other results

    /**
     * Constructor for the BDD kernel.
     * @param f         the formula factory to use
     * @param numVars   the number of variables
     * @param nodeSize  the initial number of nodes in the nodetable
     * @param cacheSize the fixed size of the internal caches
     */
    public BDDKernel(final FormulaFactory f, final int numVars, final int nodeSize, final int cacheSize) {
        this.f = f;
        prime = new BDDPrime();
        var2idx = new TreeMap<>();
        idx2var = new TreeMap<>();
        reordering = new BDDReordering(this);
        nodesize = prime.primeGTE(Math.max(nodeSize, 3));
        nodes = new int[nodesize * 6];
        minfreenodes = 20;
        for (int n = 0; n < nodesize; n++) {
            setRefcou(n, 0);
            setLow(n, -1);
            setHash(n, 0);
            setLevel(n, 0);
            setNext(n, n + 1);
        }
        setNext(nodesize - 1, 0);
        setRefcou(0, MAXREF);
        setRefcou(1, MAXREF);
        setLow(0, 0);
        setHigh(0, 0);
        setLow(1, 1);
        setHigh(1, 1);
        initOperators(Math.max(cacheSize, 3));
        freepos = 2;
        freenum = nodesize - 2;
        varnum = 0;
        gbcollectnum = 0;
        cachesize = cacheSize;
        reordering.usedNodesNextReorder = nodesize;
        maxnodeincrease = 50000;
        setNumberOfVars(numVars);
    }

    /**
     * Constructor for the BDD kernel.
     * @param f         the formula factory to use
     * @param ordering  the variable ordering
     * @param nodeSize  the initial number of nodes in the nodetable
     * @param cacheSize the fixed size of the internal caches
     */
    public BDDKernel(final FormulaFactory f, final List<Variable> ordering, final int nodeSize, final int cacheSize) {
        this(f, ordering.size(), nodeSize, cacheSize);
        for (final Variable var : ordering) {
            getOrAddVarIndex(var);
        }
    }

    /**
     * Sets the number of variables to use. It may be called more than one time,
     * but only to increase the number of variables.
     * @param num the number of variables to use
     */
    protected void setNumberOfVars(final int num) {
        if (num < 0 || num > MAXVAR) {
            throw new IllegalArgumentException("Invalid variable number: " + num);
        }
        reordering.disableReorder();
        vars = new int[num * 2];
        level2var = new int[num + 1];
        var2level = new int[num + 1];
        refstack = new int[num * 2 + 4];
        refstacktop = 0;
        while (varnum < num) {
            vars[varnum * 2] = pushRef(makeNode(varnum, 0, 1));
            vars[varnum * 2 + 1] = makeNode(varnum, 1, 0);
            popref(1);
            setRefcou(vars[varnum * 2], MAXREF);
            setRefcou(vars[varnum * 2 + 1], MAXREF);
            level2var[varnum] = varnum;
            var2level[varnum] = varnum;
            varnum++;
        }
        setLevel(0, num);
        setLevel(1, num);
        level2var[num] = num;
        var2level[num] = num;
        varResize();
        reordering.enableReorder();
    }

    /**
     * Returns the index for the given variable.
     * <p>
     * If the variable hasn't been seen before, the next free variable index is
     * assigned to it. If no free variables are left, an illegal argument
     * exception is thrown.
     * @param variable the variable
     * @return the index for the variable
     * @throws IllegalArgumentException if the variable does not yet exist in
     *                                  the kernel and there are no free
     *                                  variable indices left
     */
    public int getOrAddVarIndex(final Variable variable) {
        Integer index = var2idx.get(variable);
        if (index == null) {
            if (var2idx.size() >= varnum) {
                throw new IllegalArgumentException(
                        "No free variables left! You did not set the number of variables high enough.");
            } else {
                index = var2idx.size();
                var2idx.put(variable, index);
                idx2var.put(index, variable);
            }
        }
        return index;
    }

    /**
     * Returns the formula factory.
     * @return the formula factory
     */
    public FormulaFactory factory() {
        return f;
    }

    /**
     * Returns the reordering object for this kernel.
     * @return the reordering object
     */
    public BDDReordering getReordering() {
        return reordering;
    }

    /**
     * Returns the mapping from variables to indices.
     * @return the mapping from variables to indices
     */
    public SortedMap<Variable, Integer> var2idx() {
        return var2idx;
    }

    /**
     * Returns the mapping from indices to variables.
     * @return the mapping from indices to variables
     */
    public SortedMap<Integer, Variable> idx2var() {
        return idx2var;
    }

    /**
     * Returns the index for the given variable or -1 if the variable is
     * unknown.
     * @param var the variable
     * @return the index for the given variable
     */
    public int getIndexForVariable(final Variable var) {
        final Integer index = var2idx.get(var);
        return index == null ? -1 : index;
    }

    /**
     * Returns the variable for the given index or {@code null} if no such index
     * exists.
     * @param idx the index
     * @return the variable for the given index
     */
    public Variable getVariableForIndex(final int idx) {
        return idx2var.get(idx);
    }

    /**
     * Returns the level of the given variable or -1 if the variable is unknown.
     * @param var the variable
     * @return the level of the given variable
     */
    public int getLevel(final Variable var) {
        final Integer idx = var2idx.get(var);
        return idx != null && idx >= 0 && idx < var2level.length ? var2level[idx] : -1;
    }

    /**
     * Returns the current ordering of the variables.
     * @return the current variable ordering
     */
    public int[] getCurrentVarOrder() {
        // last var is always 0
        return Arrays.copyOf(level2var, level2var.length - 1);
    }

    protected int doWithPotentialReordering(final BddOperation operation) {
        try {
            initRef();
            return operation.perform();
        } catch (final BddReorderRequest reorderRequest) {
            reordering.checkReorder();
            initRef();
            reordering.disableReorder();
            try {
                return operation.perform();
            } catch (final BddReorderRequest e) {
                throw new IllegalStateException("Must not happen");
            } finally {
                reordering.enableReorder();
            }
        }
    }

    protected int apply(final int l, final int r, final Operand op) {
        return doWithPotentialReordering(() -> applyRec(l, r, op));
    }

    protected int applyRec(final int l, final int r, final Operand op) throws BddReorderRequest {
        final int res;
        switch (op) {
            case AND:
                if (l == r) {
                    return l;
                }
                if (isZero(l) || isZero(r)) {
                    return 0;
                }
                if (isOne(l)) {
                    return r;
                }
                if (isOne(r)) {
                    return l;
                }
                break;
            case OR:
                if (l == r) {
                    return l;
                }
                if (isOne(l) || isOne(r)) {
                    return 1;
                }
                if (isZero(l)) {
                    return r;
                }
                if (isZero(r)) {
                    return l;
                }
                break;
            case IMP:
                if (isZero(l)) {
                    return 1;
                }
                if (isOne(l)) {
                    return r;
                }
                if (isOne(r)) {
                    return 1;
                }
                break;
        }
        if (isConst(l) && isConst(r)) {
            res = op.tt[l << 1 | r];
        } else {
            final BDDCacheEntry entry = applycache.lookup(triple(l, r, op.v));
            if (entry.a == l && entry.b == r && entry.c == op.v) {
                return entry.res;
            }
            if (level(l) == level(r)) {
                pushRef(applyRec(low(l), low(r), op));
                pushRef(applyRec(high(l), high(r), op));
                res = makeNode(level(l), readRef(2), readRef(1));
            } else if (level(l) < level(r)) {
                pushRef(applyRec(low(l), r, op));
                pushRef(applyRec(high(l), r, op));
                res = makeNode(level(l), readRef(2), readRef(1));
            } else {
                pushRef(applyRec(l, low(r), op));
                pushRef(applyRec(l, high(r), op));
                res = makeNode(level(r), readRef(2), readRef(1));
            }
            popref(2);
            entry.a = l;
            entry.b = r;
            entry.c = op.v;
            entry.res = res;
        }
        return res;
    }

    /**
     * Adds a reference for a given node. Reference counting is done on
     * externally referenced nodes only and the count for a specific node
     * {@code r} can and must be increased using this function to avoid losing
     * the node in the next garbage collection. If a BDD handler is given, the
     * handler's {@link BDDHandler#newRefAdded()} method is called. If the
     * generation gets aborted due to the handler, the method will return
     * {@link BDDKernel#BDD_ABORT} as result. If {@code null} is passed as
     * handler, the generation will continue without interruption.
     * @param root    the node
     * @param handler the BDD handler
     * @return return the node
     * @throws IllegalArgumentException if the root node was invalid
     */
    public int addRef(final int root, final BDDHandler handler) {
        if (handler != null && !handler.newRefAdded()) {
            return BDD_ABORT;
        }
        if (root < 2) {
            return root;
        }
        if (root >= nodesize) {
            throw new IllegalArgumentException("Not a valid BDD root node: " + root);
        }
        if (low(root) == -1) {
            throw new IllegalArgumentException("Not a valid BDD root node: " + root);
        }
        incRef(root);
        return root;
    }

    /**
     * Deletes a reference for a given node.
     * @param root the node
     * @throws IllegalArgumentException if the root node was invalid
     */
    public void delRef(final int root) {
        if (root < 2) {
            return;
        }
        if (root >= nodesize) {
            throw new IllegalStateException("Cannot dereference a variable > varnum");
        }
        if (low(root) == -1) {
            throw new IllegalStateException("Cannot dereference variable -1");
        }
        if (!hasref(root)) {
            throw new IllegalStateException("Cannot dereference a variable which has no reference");
        }
        decRef(root);
    }

    protected void decRef(final int n) {
        if (refcou(n) != MAXREF && refcou(n) > 0) {
            setRefcou(n, refcou(n) - 1);
        }
    }

    protected void incRef(final int n) {
        if (refcou(n) < MAXREF) {
            setRefcou(n, refcou(n) + 1);
        }
    }

    protected int makeNode(final int level, final int low, final int high) throws BddReorderRequest {
        if (low == high) {
            return low;
        }
        int hash = nodehash(level, low, high);
        int res = hash(hash);
        while (res != 0) {
            if (level(res) == level && low(res) == low && high(res) == high) {
                return res;
            }
            res = next(res);
        }
        if (freepos == 0) {
            gbc();
            if ((nodesize - freenum) >= reordering.usedNodesNextReorder && reordering.reorderReady()) {
                throw new BddReorderRequest();
            }
            if ((freenum * 100) / nodesize <= minfreenodes) {
                nodeResize(true);
                hash = nodehash(level, low, high);
            }
            if (freepos == 0) {
                throw new IllegalStateException("Cannot allocate more space for more nodes.");
            }
        }
        res = freepos;
        freepos = next(freepos);
        freenum--;
        produced++;
        setLevel(res, level);
        setLow(res, low);
        setHigh(res, high);
        setNext(res, hash(hash));
        setHash(hash, res);
        return res;
    }

    protected void unmark(final int i) {
        if (i < 2) {
            return;
        }
        if (!marked(i) || low(i) == -1) {
            return;
        }
        unmarkNode(i);
        unmark(low(i));
        unmark(high(i));
    }

    protected int markCount(final int i) {
        if (i < 2) {
            return 0;
        }
        if (marked(i) || low(i) == -1) {
            return 0;
        }
        setMark(i);
        int count = 1;
        count += markCount(low(i));
        count += markCount(high(i));
        return count;
    }

    protected void gbc() {
        for (int r = 0; r < refstacktop; r++) {
            mark(refstack[r]);
        }
        for (int n = 0; n < nodesize; n++) {
            if (refcou(n) > 0) {
                mark(n);
            }
            setHash(n, 0);
        }
        freepos = 0;
        freenum = 0;
        for (int n = nodesize - 1; n >= 2; n--) {
            if ((level(n) & MARKON) != 0 && low(n) != -1) {
                setLevel(n, level(n) & MARKOFF);
                final int hash = nodehash(level(n), low(n), high(n));
                setNext(n, hash(hash));
                setHash(hash, n);
            } else {
                setLow(n, -1);
                setNext(n, freepos);
                freepos = n;
                freenum++;
            }
        }
        resetCaches();
        gbcollectnum++;
    }

    protected void gbcRehash() {
        freepos = 0;
        freenum = 0;
        for (int n = nodesize - 1; n >= 2; n--) {
            if (low(n) != -1) {
                final int hash = nodehash(level(n), low(n), high(n));
                setNext(n, hash(hash));
                setHash(hash, n);
            } else {
                setNext(n, freepos);
                freepos = n;
                freenum++;
            }
        }
    }

    protected void mark(final int i) {
        if (i < 2) {
            return;
        }
        if ((level(i) & MARKON) != 0 || low(i) == -1) {
            return;
        }
        setLevel(i, level(i) | MARKON);
        mark(low(i));
        mark(high(i));
    }

    protected void nodeResize(final boolean doRehash) {
        final int oldsize = nodesize;
        int n;
        nodesize = nodesize << 1;
        if (nodesize > oldsize + maxnodeincrease) {
            nodesize = oldsize + maxnodeincrease;
        }
        nodesize = prime.primeLTE(nodesize);
        final int[] newnodes = new int[nodesize * 6];
        System.arraycopy(nodes, 0, newnodes, 0, nodes.length);
        nodes = newnodes;
        if (doRehash) {
            for (n = 0; n < oldsize; n++) {
                setHash(n, 0);
            }
        }
        for (n = oldsize; n < nodesize; n++) {
            setRefcou(n, 0);
            setHash(n, 0);
            setLevel(n, 0);
            setLow(n, -1);
            setNext(n, n + 1);
        }
        setNext(nodesize - 1, freepos);
        freepos = oldsize;
        freenum += nodesize - oldsize;
        if (doRehash) {
            gbcRehash();
        }
    }

    protected int refcou(final int node) {
        return nodes[6 * node];
    }

    protected int level(final int node) {
        return nodes[6 * node + 1];
    }

    protected int low(final int node) {
        return nodes[6 * node + 2];
    }

    protected int high(final int node) {
        return nodes[6 * node + 3];
    }

    protected int hash(final int node) {
        return nodes[6 * node + 4];
    }

    protected int next(final int node) {
        return nodes[6 * node + 5];
    }

    protected void setRefcou(final int node, final int refcou) {
        nodes[6 * node] = refcou;
    }

    protected void setLevel(final int node, final int level) {
        nodes[6 * node + 1] = level;
    }

    protected void setLow(final int node, final int low) {
        nodes[6 * node + 2] = low;
    }

    protected void setHigh(final int node, final int high) {
        nodes[6 * node + 3] = high;
    }

    protected void setHash(final int node, final int hash) {
        nodes[6 * node + 4] = hash;
    }

    protected void setNext(final int node, final int next) {
        nodes[6 * node + 5] = next;
    }

    protected void initRef() {
        refstacktop = 0;
    }

    protected int pushRef(final int n) {
        refstack[refstacktop++] = n;
        return n;
    }

    protected int readRef(final int n) {
        return refstack[refstacktop - n];
    }

    protected void popref(final int n) {
        refstacktop -= n;
    }

    protected boolean hasref(final int n) {
        return refcou(n) > 0;
    }

    protected boolean isConst(final int n) {
        return n < 2;
    }

    protected boolean isOne(final int n) {
        return n == 1;
    }

    protected boolean isZero(final int n) {
        return n == 0;
    }

    protected boolean marked(final int n) {
        return (level(n) & MARKON) != 0;
    }

    protected void setMark(final int n) {
        setLevel(n, level(n) | MARKON);
    }

    protected void unmarkNode(final int n) {
        setLevel(n, level(n) & MARKOFF);
    }

    protected int nodehash(final int lvl, final int l, final int h) {
        return Math.abs(triple(lvl, l, h) % nodesize);
    }

    protected int pair(final int a, final int b) {
        return (a + b) * (a + b + 1) / 2 + a;
    }

    protected int triple(final int a, final int b, final int c) {
        return pair(c, pair(a, b));
    }

    protected void initOperators(final int cachesize) {
        applycache = new BDDCache(cachesize);
        itecache = new BDDCache(cachesize);
        quantcache = new BDDCache(cachesize);
        appexcache = new BDDCache(cachesize);
        replacecache = new BDDCache(cachesize);
        misccache = new BDDCache(cachesize);
        quantvarsetID = 0;
        quantvarset = null;
    }

    protected void resetCaches() {
        applycache.reset();
        itecache.reset();
        quantcache.reset();
        appexcache.reset();
        replacecache.reset();
        misccache.reset();
    }

    protected void varResize() {
        quantvarset = new int[varnum];
        quantvarsetID = 0;
    }

    /**
     * Returns the statistics for this BDD Kernel.
     * @return the statistics
     */
    public BDDStatistics statistics() {
        final BDDStatistics statistics = new BDDStatistics();
        statistics.produced = produced;
        statistics.nodesize = nodesize;
        statistics.freenum = freenum;
        statistics.varnum = varnum;
        statistics.cachesize = cachesize;
        statistics.gbcollectnum = gbcollectnum;
        return statistics;
    }

    /**
     * A class for BDD statistics.
     */
    public static class BDDStatistics {
        protected long produced;
        protected int nodesize;
        protected int freenum;
        protected int varnum;
        protected int cachesize;
        protected int gbcollectnum;

        /**
         * Returns the number of produced nodes.
         * @return the number of produced nodes
         */
        public long produced() {
            return produced;
        }

        /**
         * Returns the number of allocated nodes.
         * @return the number of allocated nodes
         */
        public int nodesize() {
            return nodesize;
        }

        /**
         * Returns the number of free nodes.
         * @return the number of free nodes
         */
        public int freenum() {
            return freenum;
        }

        /**
         * Returns the number of variables.
         * @return the number of variables
         */
        public int varnum() {
            return varnum;
        }

        /**
         * Returns the cache size.
         * @return the cache size
         */
        public int cachesize() {
            return cachesize;
        }

        /**
         * Returns the number of garbage collections.
         * @return the number of garbage collections
         */
        public int gbcollectnum() {
            return gbcollectnum;
        }

        /**
         * Returns the number of used nodes.
         * @return the number of used nodes
         */
        public int usedNodes() {
            return nodesize - freenum;
        }

        @Override
        public String toString() {
            return "BDDStatistics{" +
                    "produced nodes=" + produced +
                    ", allocated nodes=" + nodesize +
                    ", free nodes=" + freenum +
                    ", variables=" + varnum +
                    ", cache size=" + cachesize +
                    ", garbage collections=" + gbcollectnum +
                    '}';
        }
    }

    /**
     * Replaces the calls in Buddy for setjmp and longjmp.
     */
    protected static class BddReorderRequest extends RuntimeException {
    }

    @FunctionalInterface
    protected interface BddOperation {
        int perform() throws BddReorderRequest;
    }

    public BDDPrime getPrime() {
        return prime;
    }
}
