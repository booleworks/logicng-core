// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.NopHandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of operations on a BDD kernel.
 * @version 2.4.0
 * @since 2.0.0
 */
public class BddOperations {
    protected final BddKernel k;

    protected byte[] allunsatProfile;
    protected int supportId; // Current ID (true value) for support
    protected int supportMax; // Max. used level in support calc.
    protected int[] supportSet; // The found support set

    /**
     * Constructs a new object which will perform operations on the given
     * kernel.
     * @param k the kernel
     */
    public BddOperations(final BddKernel k) {
        this.k = k;
    }

    /**
     * Finds one satisfying variable assignment and returns it as BDD.
     * @param r the BDD root node
     * @return the satisfying variable assignment of the BDD as a BDD itself
     */
    public int satOne(final int r) {
        if (r < 2) {
            return r;
        }
        k.reordering.disableReorder();
        k.initRef();
        final int res = satOneRec(r);
        k.reordering.enableReorder();
        return res;
    }

    protected int satOneRec(final int r) throws BddKernel.BddReorderRequest {
        if (k.isConst(r)) {
            return r;
        }
        if (k.isZero(k.low(r))) {
            final int res = satOneRec(k.high(r));
            return k.pushRef(k.makeNode(k.level(r), BddKernel.BDD_FALSE, res));
        } else {
            final int res = satOneRec(k.low(r));
            return k.pushRef(k.makeNode(k.level(r), res, BddKernel.BDD_FALSE));
        }
    }

    /**
     * Returns an arbitrary model for a given BDD or {@code null} which contains
     * at least the given variables. If a variable is a don't care variable, it
     * will be assigned with the given default value.
     * @param r   the BDD root node
     * @param var the set of variable which has to be contained in the model as
     *            a BDD
     * @param pol the default value for don't care variables as a BDD
     * @return an arbitrary model of this BDD
     */
    public int satOneSet(final int r, final int var, final int pol) {
        if (k.isZero(r)) {
            return r;
        }
        if (!k.isConst(pol)) {
            throw new IllegalArgumentException("polarity for satOneSet must be a constant");
        }
        k.reordering.disableReorder();
        k.initRef();
        final int res = satOneSetRec(r, var, pol);
        k.reordering.enableReorder();
        return res;
    }

    protected int satOneSetRec(final int r, final int var, final int satPolarity) throws BddKernel.BddReorderRequest {
        if (k.isConst(r) && k.isConst(var)) {
            return r;
        }
        if (k.level(r) < k.level(var)) {
            if (k.isZero(k.low(r))) {
                final int res = satOneSetRec(k.high(r), var, satPolarity);
                return k.pushRef(k.makeNode(k.level(r), BddKernel.BDD_FALSE, res));
            } else {
                final int res = satOneSetRec(k.low(r), var, satPolarity);
                return k.pushRef(k.makeNode(k.level(r), res, BddKernel.BDD_FALSE));
            }
        } else if (k.level(var) < k.level(r)) {
            final int res = satOneSetRec(r, k.high(var), satPolarity);
            if (satPolarity == BddKernel.BDD_TRUE) {
                return k.pushRef(k.makeNode(k.level(var), BddKernel.BDD_FALSE, res));
            } else {
                return k.pushRef(k.makeNode(k.level(var), res, BddKernel.BDD_FALSE));
            }
        } else {
            if (k.isZero(k.low(r))) {
                final int res = satOneSetRec(k.high(r), k.high(var), satPolarity);
                return k.pushRef(k.makeNode(k.level(r), BddKernel.BDD_FALSE, res));
            } else {
                final int res = satOneSetRec(k.low(r), k.high(var), satPolarity);
                return k.pushRef(k.makeNode(k.level(r), res, BddKernel.BDD_FALSE));
            }
        }
    }

    /**
     * Returns a full model in all variables for the given BDD.
     * @param r the BDD root node
     * @return a full model of this BDD
     */
    public int fullSatOne(final int r) {
        if (r == 0) {
            return 0;
        }
        k.reordering.disableReorder();
        k.initRef();
        int res = fullSatOneRec(r);
        for (int v = k.level(r) - 1; v >= 0; v--) {
            res = k.pushRef(k.makeNode(v, res, 0));
        }
        k.reordering.enableReorder();
        return res;
    }

    protected int fullSatOneRec(final int r) throws BddKernel.BddReorderRequest {
        if (r < 2) {
            return r;
        }
        if (k.low(r) != 0) {
            int res = fullSatOneRec(k.low(r));
            for (int v = k.level(k.low(r)) - 1; v > k.level(r); v--) {
                res = k.pushRef(k.makeNode(v, res, 0));
            }
            return k.pushRef(k.makeNode(k.level(r), res, 0));
        } else {
            int res = fullSatOneRec(k.high(r));
            for (int v = k.level(k.high(r)) - 1; v > k.level(r); v--) {
                res = k.pushRef(k.makeNode(v, res, 0));
            }
            return k.pushRef(k.makeNode(k.level(r), 0, res));
        }
    }

    /**
     * Returns all models for a given BDD.
     * @param r the BDD root node
     * @return all models for the BDD
     */
    public List<byte[]> allSat(final int r) {
        final byte[] allsatProfile = new byte[k.varnum];
        for (int v = k.level(r) - 1; v >= 0; --v) {
            allsatProfile[k.level2var[v]] = -1;
        }
        k.initRef();
        final List<byte[]> allSat = new ArrayList<>();
        allSatRec(r, allSat, allsatProfile);
        return allSat;
    }

    protected void allSatRec(final int r, final List<byte[]> models, final byte[] allsatProfile) {
        if (k.isOne(r)) {
            models.add(Arrays.copyOf(allsatProfile, allsatProfile.length));
            return;
        }
        if (k.isZero(r)) {
            return;
        }
        if (!k.isZero(k.low(r))) {
            allsatProfile[k.level2var[k.level(r)]] = 0;
            for (int v = k.level(k.low(r)) - 1; v > k.level(r); --v) {
                allsatProfile[k.level2var[v]] = -1;
            }
            allSatRec(k.low(r), models, allsatProfile);
        }
        if (!k.isZero(k.high(r))) {
            allsatProfile[k.level2var[k.level(r)]] = 1;
            for (int v = k.level(k.high(r)) - 1; v > k.level(r); --v) {
                allsatProfile[k.level2var[v]] = -1;
            }
            allSatRec(k.high(r), models, allsatProfile);
        }
    }

    /**
     * Returns the model count for the given BDD.
     * @param r the BDD root node
     * @return the model count for the BDD
     */
    public BigInteger satCount(final int r) {
        final BigInteger size = BigInteger.valueOf(2).pow(k.level(r));
        return satCountRec(r, BddKernel.CACHEID_SATCOU).multiply(size);
    }

    protected BigInteger satCountRec(final int root, final int miscid) {
        if (root < 2) {
            return BigInteger.valueOf(root);
        }
        final BddCacheEntry entry = k.misccache.lookup(root);
        if (entry.a == root && entry.c == miscid) {
            return entry.bdres;
        }
        BigInteger size = BigInteger.ZERO;
        BigInteger s = BigInteger.ONE;
        s = s.multiply(BigInteger.valueOf(2).pow(k.level(k.low(root)) - k.level(root) - 1));
        size = size.add(s.multiply(satCountRec(k.low(root), miscid)));
        s = BigInteger.ONE;
        s = s.multiply(BigInteger.valueOf(2).pow(k.level(k.high(root)) - k.level(root) - 1));
        size = size.add(s.multiply(satCountRec(k.high(root), miscid)));
        entry.a = root;
        entry.c = miscid;
        entry.bdres = size;
        return size;
    }

    /**
     * Returns the number of paths to the terminal node 'one'.
     * @param r the BDD root node
     * @return the number of paths to the terminal node 'one'
     */
    public BigInteger pathCountOne(final int r) {
        return pathCountRecOne(r, BddKernel.CACHEID_PATHCOU_ONE);
    }

    protected BigInteger pathCountRecOne(final int r, final int miscid) {
        final BigInteger size;
        if (k.isZero(r)) {
            return BigInteger.ZERO;
        }
        if (k.isOne(r)) {
            return BigInteger.ONE;
        }
        final BddCacheEntry entry = k.misccache.lookup(r);
        if (entry.a == r && entry.c == miscid) {
            return entry.bdres;
        }
        size = pathCountRecOne(k.low(r), miscid).add(pathCountRecOne(k.high(r), miscid));
        entry.a = r;
        entry.c = miscid;
        entry.bdres = size;
        return size;
    }

    /**
     * Returns the number of paths to the terminal node 'zero'.
     * @param r the BDD root node
     * @return the number of paths to the terminal node 'zero'
     */
    public BigInteger pathCountZero(final int r) {
        return pathCountRecZero(r, BddKernel.CACHEID_PATHCOU_ZERO);
    }

    protected BigInteger pathCountRecZero(final int r, final int miscid) {
        final BigInteger size;
        if (k.isZero(r)) {
            return BigInteger.ONE;
        }
        if (k.isOne(r)) {
            return BigInteger.ZERO;
        }
        final BddCacheEntry entry = k.misccache.lookup(r);
        if (entry.a == r && entry.c == miscid) {
            return entry.bdres;
        }
        size = pathCountRecZero(k.low(r), miscid).add(pathCountRecZero(k.high(r), miscid));
        entry.a = r;
        entry.c = miscid;
        entry.bdres = size;
        return size;
    }

    /**
     * Returns all unsatisfiable assignments for a given BDD.
     * @param r the BDD root node
     * @return all unsatisfiable assignments for the BDD
     */
    public List<byte[]> allUnsat(final int r) {
        allunsatProfile = new byte[k.varnum];
        for (int v = k.level(r) - 1; v >= 0; --v) {
            allunsatProfile[k.level2var[v]] = -1;
        }
        k.initRef();
        final List<byte[]> allUnsat = new ArrayList<>();
        allUnsatRec(r, allUnsat);
        return allUnsat;
    }

    protected void allUnsatRec(final int r, final List<byte[]> models) {
        if (k.isZero(r)) {
            models.add(Arrays.copyOf(allunsatProfile, allunsatProfile.length));
            return;
        }
        if (k.isOne(r)) {
            return;
        }
        if (!k.isOne(k.low(r))) {
            allunsatProfile[k.level2var[k.level(r)]] = 0;
            for (int v = k.level(k.low(r)) - 1; v > k.level(r); --v) {
                allunsatProfile[k.level2var[v]] = -1;
            }
            allUnsatRec(k.low(r), models);
        }
        if (!k.isOne(k.high(r))) {
            allunsatProfile[k.level2var[k.level(r)]] = 1;
            for (int v = k.level(k.high(r)) - 1; v > k.level(r); --v) {
                allunsatProfile[k.level2var[v]] = -1;
            }
            allUnsatRec(k.high(r), models);
        }
    }

    /**
     * Returns all the variables that a given BDD depends on.
     * @param r the BDD root node
     * @return all the variables that the BDD depends on
     */
    public int support(final int r) {
        final int supportSize = 0;
        int res = 1;
        if (r < 2) {
            return BddKernel.BDD_FALSE;
        }
        if (supportSize < k.varnum) {
            supportSet = new int[k.varnum];
            supportId = 0;
        }
        if (supportId == 0x0FFFFFFF) {
            supportId = 0;
        }
        ++supportId;
        final int supportMin = k.level(r);
        supportMax = supportMin;
        supportRec(r, supportSet);
        k.unmark(r);

        k.reordering.disableReorder();
        for (int n = supportMax; n >= supportMin; --n) {
            if (supportSet[n] == supportId) {
                k.addRef(res, NopHandler.get());
                final int tmp = k.makeNode(n, 0, res);
                k.delRef(res);
                res = tmp;
            }
        }
        k.reordering.enableReorder();
        return res;
    }

    protected void supportRec(final int r, final int[] support) {
        if (r < 2) {
            return;
        }
        if ((k.level(r) & BddKernel.MARKON) != 0 || k.low(r) == -1) {
            return;
        }
        support[k.level(r)] = supportId;
        if (k.level(r) > supportMax) {
            supportMax = k.level(r);
        }
        k.setLevel(r, k.level(r) | BddKernel.MARKON);
        supportRec(k.low(r), support);
        supportRec(k.high(r), support);
    }

    /**
     * Returns the number of nodes for a given BDD.
     * @param r the BDD root node
     * @return the number of nodes for the BDD
     */
    public int nodeCount(final int r) {
        final int count = k.markCount(r);
        k.unmark(r);
        return count;
    }

    /**
     * Returns how often each variable occurs in the given BDD.
     * @param r the BDD root node
     * @return how often each variable occurs in the BDD
     */
    public int[] varProfile(final int r) {
        final int[] varprofile = new int[k.varnum];
        varProfileRec(r, varprofile);
        k.unmark(r);
        return varprofile;
    }

    protected void varProfileRec(final int r, final int[] varprofile) {
        if (r < 2) {
            return;
        }
        if ((k.level(r) & BddKernel.MARKON) != 0) {
            return;
        }
        varprofile[k.level2var[k.level(r)]]++;
        k.setLevel(r, k.level(r) | BddKernel.MARKON);
        varProfileRec(k.low(r), varprofile);
        varProfileRec(k.high(r), varprofile);
    }

    /**
     * Returns all nodes for a given root node in their internal representation.
     * The internal representation is stored in an array:
     * {@code [node number, variable, low, high]}
     * @param r the BDD root node
     * @return all Nodes in their internal representation
     */
    public List<int[]> allNodes(final int r) {
        final List<int[]> result = new ArrayList<>();
        if (r < 2) {
            return result;
        }
        k.mark(r);
        for (int n = 0; n < k.nodesize; n++) {
            if ((k.level(n) & BddKernel.MARKON) != 0) {
                k.setLevel(n, k.level(n) & BddKernel.MARKOFF);
                result.add(new int[]{n, k.level2var[k.level(n)], k.low(n), k.high(n)});
            }
        }
        return result;
    }

    /**
     * Returns a formula representation of this BDD. This is done by using the
     * Shannon expansion. If {@code followPathsToTrue} is activated, the paths
     * leading to the {@code true} terminal are followed to generate the
     * formula. If {@code followPathsToTrue} is deactivated, the paths leading
     * to the {@code false} terminal are followed to generate the formula and
     * the resulting formula is negated. Depending on the formula and the number
     * of satisfying assignments, the generated formula can be more compact
     * using the {@code true} paths or {@code false} paths, respectively.
     * @param f                 the formula factory to generate new formulas
     * @param r                 the BDD root node
     * @param followPathsToTrue the extraction style
     * @return the formula
     */
    public Formula toFormula(final FormulaFactory f, final int r, final boolean followPathsToTrue) {
        k.initRef();
        final Formula formula = toFormulaRec(f, r, followPathsToTrue);
        return followPathsToTrue ? formula : formula.negate(f);
    }

    protected Formula toFormulaRec(final FormulaFactory f, final int r, final boolean followPathsToTrue) {
        if (k.isOne(r)) {
            return f.constant(followPathsToTrue);
        }
        if (k.isZero(r)) {
            return f.constant(!followPathsToTrue);
        }
        final Variable var = k.idx2var.get(k.level(r));
        final int low = k.low(r);
        final Formula lowFormula = isRelevant(low, followPathsToTrue)
                ? f.and(var.negate(f), toFormulaRec(f, low, followPathsToTrue)) : f.falsum();
        final int high = k.high(r);
        final Formula rightFormula =
                isRelevant(high, followPathsToTrue) ? f.and(var, toFormulaRec(f, high, followPathsToTrue)) : f.falsum();
        return f.or(lowFormula, rightFormula);
    }

    private boolean isRelevant(final int r, final boolean followPathsToTrue) {
        return followPathsToTrue && !k.isZero(r) || !followPathsToTrue && !k.isOne(r);
    }
}
