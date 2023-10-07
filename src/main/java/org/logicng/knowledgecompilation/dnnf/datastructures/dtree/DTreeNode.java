// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import org.logicng.collections.LNGIntVector;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.knowledgecompilation.dnnf.DnnfSatSolver;
import org.logicng.solvers.sat.MiniSatStyleSolver;

import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An internal node in a DTree.
 * @version 2.0.0
 * @since 2.0.0
 */
public class DTreeNode extends DTree {

    protected final DTree left;
    protected final DTree right;
    protected final int size;

    protected DnnfSatSolver solver;

    protected final SortedSet<Variable> staticVariableSet;
    protected final BitSet staticSeparatorBitSet;
    protected final int[] staticClauseIds;
    protected final int depth;
    protected int widestSeparator;

    protected final DTreeLeaf[] leafs; // all leafs
    protected final DTreeLeaf[] leftLeafs;
    protected final DTreeLeaf[] rightLeafs;

    protected int[] clauseContents; // content of all clauses under this node, e.g. clause {1,3} with id 0, {2,6,8} with id 1, {4,6} with id 6 --> [1,3,-1,2,6,-2,4,6,-7]
    protected int[] leftClauseContents;
    protected int[] rightClauseContents;

    protected BitSet localLeftVarSet;
    protected BitSet localRightVarSet;

    /**
     * Constructs a new DTree node with the given left and right DTree.
     * @param f     the formula factory to use for caching
     * @param left  the left DTree
     * @param right the right DTree
     **/
    public DTreeNode(final FormulaFactory f, final DTree left, final DTree right) {
        this.left = left;
        this.right = right;
        size = left.size() + right.size();

        final List<DTreeLeaf> ll = left.leafs();
        excludeUnitLeafs(ll);
        leftLeafs = ll.toArray(new DTreeLeaf[0]);
        final List<DTreeLeaf> rl = right.leafs();
        excludeUnitLeafs(rl);
        rightLeafs = rl.toArray(new DTreeLeaf[0]);
        leafs = new DTreeLeaf[leftLeafs.length + rightLeafs.length];
        System.arraycopy(leftLeafs, 0, leafs, 0, leftLeafs.length);
        System.arraycopy(rightLeafs, 0, leafs, leftLeafs.length, rightLeafs.length);

        staticVariableSet = new TreeSet<>(left.staticVariableSet(f));
        staticVariableSet.addAll(right.staticVariableSet(f));
        staticSeparatorBitSet = new BitSet();
        final int[] leftClauseIds = left.staticClauseIds();
        final int[] rightClauseIds = right.staticClauseIds();
        staticClauseIds = new int[leftClauseIds.length + rightClauseIds.length];
        System.arraycopy(leftClauseIds, 0, staticClauseIds, 0, leftClauseIds.length);
        System.arraycopy(rightClauseIds, 0, staticClauseIds, leftClauseIds.length, rightClauseIds.length);
        depth = 1 + Math.max(left.depth(), right.depth());
    }

    /**
     * Returns the left DTree
     * @return the left DTree
     */
    public DTree left() {
        return left;
    }

    /**
     * Returns the right DTree
     * @return the right DTree
     */
    public DTree right() {
        return right;
    }

    @Override
    public void initialize(final DnnfSatSolver solver) {
        this.solver = solver;
        left.initialize(solver);
        right.initialize(solver);
        staticVarSet = left.staticVarSet();
        staticVarSet.or(right.staticVarSet());
        staticVariables = toArray(staticVarSet);
        staticSeparator = sortedIntersect(left.staticVarSetArray(), right.staticVarSetArray());
        for (final int i : staticSeparator) {
            staticSeparatorBitSet.set(i);
        }
        widestSeparator = Math.max(staticSeparator.length, Math.max(left.widestSeparator(), right.widestSeparator()));
        localLeftVarSet = new BitSet(staticVariables[staticVariables.length - 1]);
        localRightVarSet = new BitSet(staticVariables[staticVariables.length - 1]);

        final LNGIntVector lClauseContents = new LNGIntVector();
        for (final DTreeLeaf leaf : leftLeafs) {
            for (final int i : leaf.literals()) {
                lClauseContents.push(i);
            }
            lClauseContents.push(-leaf.getId() - 1);
        }
        leftClauseContents = lClauseContents.toArray();
        final LNGIntVector rClauseContents = new LNGIntVector();
        for (final DTreeLeaf leaf : rightLeafs) {
            for (final int i : leaf.literals()) {
                rClauseContents.push(i);
            }
            rClauseContents.push(-leaf.getId() - 1);
        }
        rightClauseContents = rClauseContents.toArray();
        clauseContents = new int[leftClauseContents.length + rightClauseContents.length];
        System.arraycopy(leftClauseContents, 0, clauseContents, 0, leftClauseContents.length);
        System.arraycopy(rightClauseContents, 0, clauseContents, leftClauseContents.length, rightClauseContents.length);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public SortedSet<Variable> staticVariableSet(final FormulaFactory f) {
        return staticVariableSet;
    }

    @Override
    public BitSet dynamicSeparator() {
        localLeftVarSet.clear();
        localRightVarSet.clear();
        varSet(leftClauseContents, localLeftVarSet);
        varSet(rightClauseContents, localRightVarSet);
        localLeftVarSet.and(localRightVarSet);
        return localLeftVarSet;
    }

    protected void varSet(final int[] clausesContents, final BitSet localVarSet) {
        int i = 0;
        while (i < clausesContents.length) {
            int j = i;
            boolean subsumed = false;
            while (clausesContents[j] >= 0) {
                if (!subsumed && solver.valueOf(clausesContents[j]) == Tristate.TRUE) {
                    subsumed = true;
                }
                j++;
            }
            if (!subsumed) {
                for (int n = i; n < j; n++) {
                    if (solver.valueOf(clausesContents[n]) == Tristate.UNDEF) {
                        localVarSet.set(MiniSatStyleSolver.var(clausesContents[n]));
                    }
                }
            }
            i = j + 1;
        }
    }

    @Override
    public int[] staticClauseIds() {
        return staticClauseIds;
    }

    /**
     * Sets the cache key according to this tree.
     * @param key               the key to set
     * @param numberOfVariables the number of variables
     */
    public void cacheKey(final BitSet key, final int numberOfVariables) {
        int i = 0;
        while (i < clauseContents.length) {
            int j = i;
            boolean subsumed = false;
            while (clauseContents[j] >= 0) {
                if (!subsumed && solver.valueOf(clauseContents[j]) == Tristate.TRUE) {
                    subsumed = true;
                }
                j++;
            }
            if (!subsumed) {
                key.set(-clauseContents[j] + 1 + numberOfVariables);
                for (int n = i; n < j; n++) {
                    if (solver.valueOf(clauseContents[n]) == Tristate.UNDEF) {
                        key.set(MiniSatStyleSolver.var(clauseContents[n]));
                    }
                }
            }
            i = j + 1;
        }
    }

    @Override
    public void countUnsubsumedOccurrences(final int[] occurrences) {
        for (final DTreeLeaf leaf : leafs) {
            leaf.countUnsubsumedOccurrences(occurrences);
        }
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public int widestSeparator() {
        return widestSeparator;
    }

    @Override
    public List<DTreeLeaf> leafs() {
        final List<DTreeLeaf> result = left.leafs();
        result.addAll(right.leafs());
        return result;
    }

    @Override
    public String toString() {
        return String.format("DTreeNode: [%s, %s]", left, right);
    }

    protected void excludeUnitLeafs(final List<DTreeLeaf> leafs) {
        leafs.removeIf(dTreeLeaf -> dTreeLeaf.clauseSize() == 1);
    }

    static int[] toArray(final BitSet bits) {
        final int[] result = new int[bits.cardinality()];
        int n = 0;
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            result[n++] = i;
        }
        return result;
    }

    static int[] sortedIntersect(final int[] left, final int[] right) {
        final SortedSet<Integer> l = new TreeSet<>();
        final SortedSet<Integer> intersection = new TreeSet<>();
        for (final int i : left) {
            l.add(i);
        }
        for (final int i : right) {
            if (l.contains(i)) {
                intersection.add(i);
            }
        }
        final int[] result = new int[intersection.size()];
        int i = 0;
        for (final Integer elem : intersection) {
            result[i++] = elem;
        }
        return result;
    }
}
