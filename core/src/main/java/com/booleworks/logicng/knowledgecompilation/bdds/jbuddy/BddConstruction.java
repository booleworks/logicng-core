// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

import com.booleworks.logicng.handlers.ComputationHandler;

/**
 * This class provides abstractions for the construction of BDDs.
 * @version 2.0.0
 * @since 2.0.0
 */
public class BddConstruction {

    private final BddKernel k;

    /**
     * Constructs a new object with the given kernel.
     * @param k the kernel
     */
    public BddConstruction(final BddKernel k) {
        this.k = k;
    }

    /**
     * Returns a BDD representing the i-th variable (one node with the children
     * true and false).
     * @param i the index i
     * @return the BDD representing the i-th variable
     * @throws IllegalArgumentException if the index is not within the range of
     *                                  variables
     */
    public int ithVar(final int i) {
        if (i < 0 || i >= k.varnum) {
            throw new IllegalArgumentException("Illegal variable number: " + i);
        }
        return k.vars[i * 2];
    }

    /**
     * Returns a BDD representing the negation of the i-th variable (one node
     * with the children true and false).
     * @param i the index i
     * @return the BDD representing the negated i-th variable
     * @throws IllegalArgumentException if the index is not within the range of
     *                                  variables
     */
    public int nithVar(final int i) {
        if (i < 0 || i >= k.varnum) {
            throw new IllegalArgumentException("Illegal variable number: " + i);
        }
        return k.vars[i * 2 + 1];
    }

    /**
     * Returns the variable index labeling the given root node.
     * @param root the root node of the BDD
     * @return the variable index
     */
    public int bddVar(final int root) {
        if (root < 2) {
            throw new IllegalArgumentException("Illegal node number: " + root);
        }
        return k.level2var[k.level(root)];
    }

    /**
     * Returns the false branch of the given root node.
     * @param root the root node of the BDD
     * @return the false branch
     */
    public int bddLow(final int root) {
        if (root < 2) {
            throw new IllegalArgumentException("Illegal node number: " + root);
        }
        return k.low(root);
    }

    /**
     * Returns the true branch of the given root node.
     * @param root the root node of the BDD
     * @return the true branch
     */
    public int bddHigh(final int root) {
        if (root < 2) {
            throw new IllegalArgumentException("Illegal node number: " + root);
        }
        return k.high(root);
    }

    /**
     * Returns the conjunction of two BDDs.
     * @param l the first BDD
     * @param r the second BDD
     * @return the conjunction of the two BDDs or
     * {@link BddKernel#BDD_ABORT_NEW_NODE} if canceled by the handler
     */
    public int and(final int l, final int r, final ComputationHandler handler) {
        return k.apply(l, r, BddKernel.Operand.AND, handler);
    }

    /**
     * Returns the disjunction of two BDDs.
     * @param l the first BDD
     * @param r the second BDD
     * @return the disjunction of the two BDDs or
     * {@link BddKernel#BDD_ABORT_NEW_NODE} if canceled by the handler
     */
    public int or(final int l, final int r, final ComputationHandler handler) {
        return k.apply(l, r, BddKernel.Operand.OR, handler);
    }

    /**
     * Returns the implication of two BDDs.
     * @param l the first BDD
     * @param r the second BDD
     * @return the implication of the two BDDs or
     * {@link BddKernel#BDD_ABORT_NEW_NODE} if canceled by the handler
     */
    public int implication(final int l, final int r, final ComputationHandler handler) {
        return k.apply(l, r, BddKernel.Operand.IMP, handler);
    }

    /**
     * Returns the equivalence of two BDDs.
     * @param l the first BDD
     * @param r the second BDD
     * @return the equivalence of the two BDDs or
     * {@link BddKernel#BDD_ABORT_NEW_NODE} if canceled by the handler
     */
    public int equivalence(final int l, final int r, final ComputationHandler handler) {
        return k.apply(l, r, BddKernel.Operand.EQUIV, handler);
    }

    /**
     * Returns the negation of a BDD.
     * @param r the BDD
     * @return the negation of the BDD
     */
    public int not(final int r) {
        return k.doWithPotentialReordering(() -> notRec(r));
    }

    protected int notRec(final int r) throws BddKernel.BddReorderRequest {
        if (k.isZero(r)) {
            return BddKernel.BDD_TRUE;
        }
        if (k.isOne(r)) {
            return BddKernel.BDD_FALSE;
        }
        final BddCacheEntry entry = k.applycache.lookup(r);
        if (entry.a == r && entry.c == BddKernel.Operand.NOT.v) {
            return entry.res;
        }
        k.pushRef(notRec(k.low(r)));
        k.pushRef(notRec(k.high(r)));
        final int res = k.makeNode(k.level(r), k.readRef(2), k.readRef(1));
        k.popref(2);
        entry.a = r;
        entry.c = BddKernel.Operand.NOT.v;
        entry.res = res;
        return res;
    }

    /**
     * Restricts the variables in the BDD {@code r} to constants true or false.
     * The restriction is submitted in the BDD {@code var}.
     * @param r   the BDD to be restricted
     * @param var the variable mapping as a BDD
     * @return the restricted BDD
     */
    public int restrict(final int r, final int var) {
        if (var < 2) {
            return r;
        }
        varset2svartable(var);
        return k.doWithPotentialReordering(() -> restrictRec(r, (var << 3) | BddKernel.CACHEID_RESTRICT));
    }

    protected int restrictRec(final int r, final int miscid) throws BddKernel.BddReorderRequest {
        final int res;
        if (k.isConst(r) || k.level(r) > k.quantlast) {
            return r;
        }
        final BddCacheEntry entry = k.misccache.lookup(k.pair(r, miscid));
        if (entry.a == r && entry.c == miscid) {
            return entry.res;
        }
        if (insvarset(k.level(r))) {
            if (k.quantvarset[k.level(r)] > 0) {
                res = restrictRec(k.high(r), miscid);
            } else {
                res = restrictRec(k.low(r), miscid);
            }
        } else {
            k.pushRef(restrictRec(k.low(r), miscid));
            k.pushRef(restrictRec(k.high(r), miscid));
            res = k.makeNode(k.level(r), k.readRef(2), k.readRef(1));
            k.popref(2);
        }
        entry.a = r;
        entry.c = miscid;
        entry.res = res;
        return res;
    }

    /**
     * Existential quantifier elimination for the variables in {@code var}.
     * @param r   the BDD root node
     * @param var the variables to eliminate
     * @return the BDD with the eliminated variables or
     * {@link BddKernel#BDD_ABORT_NEW_NODE} if canceled by the handler
     */
    public int exists(final int r, final int var, final ComputationHandler handler) {
        if (var < 2) {
            return r;
        }
        varset2vartable(var);
        return k.doWithPotentialReordering(() -> quantRec(r, BddKernel.Operand.OR, var << 3, handler));
    }

    /**
     * Universal quantifier elimination for the variables in {@code var}.
     * @param r   the BDD root node
     * @param var the variables to eliminate
     * @return the BDD with the eliminated variables or
     * {@link BddKernel#BDD_ABORT_NEW_NODE} if canceled by the handler
     */
    public int forAll(final int r, final int var, final ComputationHandler handler) {
        if (var < 2) {
            return r;
        }
        varset2vartable(var);
        return k.doWithPotentialReordering(
                () -> quantRec(r, BddKernel.Operand.AND, (var << 3) | BddKernel.CACHEID_FORALL, handler));
    }

    protected int quantRec(final int r, final BddKernel.Operand op, final int quantid, final ComputationHandler handler)
            throws BddKernel.BddReorderRequest {
        final int res;
        if (r < 2 || k.level(r) > k.quantlast) {
            return r;
        }
        final BddCacheEntry entry = k.quantcache.lookup(r);
        if (entry.a == r && entry.c == quantid) {
            return entry.res;
        }
        final int low = quantRec(k.low(r), op, quantid, handler);
        if (BddKernel.isAborted(low)) {
            return low;
        }
        k.pushRef(low);
        final int high = quantRec(k.high(r), op, quantid, handler);
        if (BddKernel.isAborted(high)) {
            k.popref(1);
            return high;
        }
        k.pushRef(high);
        if (invarset(k.level(r))) {
            res = k.applyRec(k.readRef(2), k.readRef(1), op, handler);
        } else {
            res = k.makeNode(k.level(r), k.readRef(2), k.readRef(1));
        }
        k.popref(2);
        if (BddKernel.isAborted(res)) {
            return res;
        }
        entry.a = r;
        entry.c = quantid;
        entry.res = res;
        return res;
    }

    protected void varset2svartable(final int r) {
        if (r < 2) {
            throw new IllegalArgumentException("Illegal variable: " + r);
        }
        k.quantvarsetId++;
        if (k.quantvarsetId == Integer.MAX_VALUE / 2) {
            k.quantvarset = new int[k.varnum];
            k.quantvarsetId = 1;
        }
        for (int n = r; !k.isConst(n); ) {
            if (k.isZero(k.low(n))) {
                k.quantvarset[k.level(n)] = k.quantvarsetId;
                n = k.high(n);
            } else {
                k.quantvarset[k.level(n)] = -k.quantvarsetId;
                n = k.low(n);
            }
            k.quantlast = k.level(n);
        }
    }

    protected void varset2vartable(final int r) {
        if (r < 2) {
            throw new IllegalArgumentException("Illegal variable: " + r);
        }
        k.quantvarsetId++;
        if (k.quantvarsetId == Integer.MAX_VALUE) {
            k.quantvarset = new int[k.varnum];
            k.quantvarsetId = 1;
        }
        for (int n = r; n > 1; n = k.high(n)) {
            k.quantvarset[k.level(n)] = k.quantvarsetId;
            k.quantlast = k.level(n);
        }
    }

    protected boolean insvarset(final int a) {
        return Math.abs(k.quantvarset[a]) == k.quantvarsetId;
    }

    protected boolean invarset(final int a) {
        return k.quantvarset[a] == k.quantvarsetId;
    }
}
