package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Util {

    public static void pushNewElement(final SddNode prime, final SddNode sub, final VTree currentVTree,
                                      final VTreeRoot root, final Collection<SddElement> target) {
        assert !prime.isFalse();
        assert sub.isTrue() || root.isSubtree(root.getVTree(prime), ((VTreeInternal) currentVTree).getLeft());
        assert sub.isTrivial() || root.isSubtree(root.getVTree(sub), ((VTreeInternal) currentVTree).getRight());
        target.add(new SddElement(prime, sub));
    }

    public static <T> void reverseSlice(final List<T> list, final int start, final int end) {
        assert start >= 0 && start < list.size();
        assert end >= 0 && end < list.size();
        int l = start;
        int r = end;
        while (l < r) {
            final T x = list.get(l);
            list.set(l, list.get(r));
            list.set(r, x);
            --l;
            --r;
        }
    }

    public static boolean elementsCompressed(final Collection<SddElement> elements) {
        SddElement last = null;
        for (final SddElement element : elements) {
            if (last != null) {
                if (last.getSub().getId() >= element.getSub().getId()) {
                    return false;
                }
            }
            last = element;
        }
        return true;
    }

    public static VTree lcaOfCompressedElements(final Collection<SddElement> elements, final VTreeRoot root) {
        assert !elements.isEmpty();

        VTree lLca = null;
        VTree rLca = null;

        for (final SddElement element : elements) {
            final VTree pVTree = root.getVTree(element.getPrime());
            final VTree sVTree = root.getVTree(element.getSub());
            assert pVTree != null;

            lLca = lLca == null ? pVTree : root.lcaOf(pVTree, lLca);
            if (sVTree != null & rLca != null) {
                rLca = root.lcaOf(sVTree, rLca);
            } else if (sVTree != null) {
                rLca = sVTree;
            }
        }

        assert lLca != null && rLca != null;
        assert root.getPosition(lLca) < root.getPosition(rLca);

        final Pair<VTree, VTreeRoot.CmpType> lca = root.cmpVTrees(lLca, rLca);

        assert lca.getSecond() == VTreeRoot.CmpType.INCOMPARABLE;
        assert lca.getFirst() != null;

        return lca.getFirst();
    }

    public static ArrayList<Formula> sortLitsetsByLca(final Collection<Formula> litsets, final VTreeRoot root,
                                                      final FormulaFactory f) {
        final ArrayList<Pair<VTree, Formula>> vTrees = new ArrayList<>(litsets.size());
        for (final Formula litset : litsets) {
            vTrees.add(new Pair<>(VTreeUtil.lcaFromVariables(litset.variables(f), root), litset));
        }
        vTrees.sort((o1, o2) -> {
            final VTree vTree1 = o1.getFirst();
            final VTree vTree2 = o2.getFirst();
            final int pos1 = root.getPosition(o1.getFirst());
            final int pos2 = root.getPosition(o2.getFirst());
            if (o1.getSecond() == o2.getSecond()) {
                return 0;
            }
            if (vTree1 != vTree2 && (root.isSubtree(vTree2, vTree1) || (!root.isSubtree(vTree1, vTree2)
                    && pos1 > pos2))) {
                return 1;
            } else if (vTree1 != vTree2 && (root.isSubtree(vTree1, vTree2) || (!root.isSubtree(vTree2, vTree1)
                    && pos1 < pos2))) {
                return -1;
            } else {
                final Set<Variable> ls1 = o1.getSecond().variables(f);
                final Set<Variable> ls2 = o2.getSecond().variables(f);
                if (ls1.size() > ls2.size()) {
                    return 1;
                } else if (ls1.size() < ls2.size()) {
                    return -1;
                } else {
                    final Iterator<Variable> i1 = ls1.iterator();
                    final Iterator<Variable> i2 = ls2.iterator();
                    for (int i = 0; i < ls1.size(); ++i) {
                        final Variable e1 = i1.next();
                        final Variable e2 = i2.next();
                        final int cmp = e1.compareTo(e2);
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                    return 0;
                }
            }
        });
        final ArrayList<Formula> sorted = new ArrayList<>(vTrees.size());
        for (final Pair<VTree, Formula> p : vTrees) {
            sorted.add(p.getSecond());
        }
        return sorted;
    }

}
