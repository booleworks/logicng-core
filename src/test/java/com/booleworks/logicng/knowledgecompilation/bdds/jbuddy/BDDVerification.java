// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

public class BDDVerification {
    private final BDDKernel k;

    public BDDVerification(final BDDKernel k) {
        this.k = k;
    }

    /////////////////// Verification /////////////////////////////////

    /**
     * Debug method for verifying the consistency of the BDD at index {@code root}.
     * @param root the root of the BDD
     * @return whether the BDD is valid or not
     */
    public boolean verify(final int root) {
        final int varnum = k.level2var.length - 1;
        for (int i = 0; i < varnum * 2 + 2; i++) {
            if (k.refcou(i) != BDDKernel.MAXREF) {
                System.out.println("Constant or Variable without MAXREF count: " + i);
                return false;
            }
            if (i == 0 && (k.low(i) != 0 || k.high(i) != 0 || k.level(i) != varnum)) {
                System.out.println("Illegal FALSE node");
                return false;
            }
            if (i == 1 && (k.low(i) != 1 || k.high(i) != 1 || k.level(i) != varnum)) {
                System.out.println("Illegal TRUE node");
                return false;
            }
            if (i > 1 && i % 2 == 0) {
                if (k.low(i) != 0) {
                    System.out.println("VAR Low wrong");
                    return false;
                } else if (k.high(i) != 1) {
                    System.out.println("VAR High wrong");
                    return false;
                }
            }
            if (i > 1 && i % 2 == 1) {
                if (k.low(i) != 1) {
                    System.out.println("VAR Low wrong");
                    return false;
                } else if (k.high(i) != 0) {
                    System.out.println("VAR High wrong");
                    return false;
                }
            }
            if (i > 1 && k.level(i) >= varnum) { //this.level2var[node.level] != i / 2 - 1) {
                System.out.println("VAR Level wrong");
                return false;
            }
        }
        if (root >= 0) {
            for (int i = varnum * 2 + 2; i < k.nodesize; i++) {
                if (k.refcou(i) > 1) {
                    System.out.println("Refcou > 1");
                    return false;
                } else if (k.refcou(i) == 1 && i != root) {
                    System.out.println("Wrong refcou");
                    return false;
                } else if (k.refcou(i) == 0 && i == root) {
                    System.out.println("Entry point not marked");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Debug method for verifying the consistency of the BDD at index {@code root}.
     * @param root the root of the BDD
     * @return whether the BDD is valid or not
     */
    public long verifyTree(final int root) {
        return verifyTreeRec(root, new long[k.nodes.length]);
    }

    protected long verifyTreeRec(final int root, final long[] cache) {
        if (cache[root] > 0) {
            return cache[root];
        }
        final int low = k.low(root);
        final int high = k.high(root);
        final int nodeLevel;
        final int lowLevel;
        final int highLevel;

        nodeLevel = k.level(root);
        lowLevel = k.level(low);
        highLevel = k.level(high);

        if (root == 0 || root == 1) {
            cache[root] = 1;
            return 1;
        }
        if (nodeLevel > lowLevel && nodeLevel > highLevel) {
            System.out.println(root + " inconsistent!");
            return -1;
        }
        final long lowRec = verifyTreeRec(low, cache);
        final long highRec = verifyTreeRec(high, cache);
        final long result = lowRec < 0 || highRec < 0 ? -1 : lowRec + highRec;
        if (result >= 0) {
            cache[root] = result;
        }
        return result;
    }

    protected boolean verifyLevelData() {
        for (int level = 0; level < k.reordering.levels.length; level++) {
            final BDDReordering.LevelData data = k.reordering.levels[level];
            for (int i = data.start; i < data.start + data.size; i++) {
                int r = k.hash(i);
                while (r != 0) {
                    if (k.level(r) != level) {
                        System.out.println("Wrong level!");
                        return false;
                    }
                    r = k.next(r);
                }
            }
        }
        return true;
    }

    protected void hashOutput() {
        System.out.println("------------------------------------------");
        for (int i = 0; i < k.nodes.length; i++) {
            System.out.printf("%2d: Hash = %2d, Next = %2d%n", i, k.hash(i), k.next(i));
        }
        System.out.println("------------------------------------------");
    }
}
