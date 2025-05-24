package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Util {

    public static void pushNewElement(final SddNode prime, final SddNode sub, final Collection<SddElement> target) {
        assert !prime.isFalse();
        target.add(new SddElement(prime, sub));
    }

    public static <C extends Collection<Integer>> C varsToIndicesOnlyKnown(final Set<Variable> variables, final Sdd sdd,
                                                                           final C dst) {
        for (final Variable var : variables) {
            final int idx = sdd.variableToIndex(var);
            if (idx != -1) {
                dst.add(idx);
            }
        }
        return dst;
    }

    public static <C extends Collection<Integer>> C varsToIndicesExpectKnown(final Set<Variable> variables,
                                                                             final Sdd sdd,
                                                                             final C dst) {
        for (final Variable var : variables) {
            final int idx = sdd.variableToIndex(var);
            if (idx == -1) {
                throw new IllegalArgumentException("Variable is not known to SDD: " + var);
            } else {
                dst.add(idx);
            }
        }
        return dst;
    }

    public static <C extends Collection<Variable>> C indicesToVars(final Set<Integer> indices, final Sdd sdd,
                                                                   final C dst) {
        for (final int idx : indices) {
            dst.add(sdd.indexToVariable(idx));
        }
        return dst;
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
            final VTree pVTree = element.getPrime().getVTree();
            final VTree sVTree = element.getSub().getVTree();
            assert pVTree != null;

            lLca = lLca == null ? pVTree : root.lcaOf(pVTree, lLca);
            if (sVTree != null && rLca != null) {
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

    public static ArrayList<Formula> sortLitsetsByLca(final Collection<Formula> litsets, final Sdd sdd) {
        final ArrayList<Pair<VTree, Formula>> vTrees = new ArrayList<>(litsets.size());
        for (final Formula litset : litsets) {
            final List<Integer> varIdxs =
                    Util.varsToIndicesExpectKnown(litset.variables(sdd.getFactory()), sdd, new ArrayList<>());
            vTrees.add(new Pair<>(VTreeUtil.lcaFromVariables(varIdxs, sdd), litset));
        }
        vTrees.sort((o1, o2) -> {
            final VTree vTree1 = o1.getFirst();
            final VTree vTree2 = o2.getFirst();
            final VTreeRoot root = sdd.getVTree();
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
                final Set<Variable> ls1 = o1.getSecond().variables(sdd.getFactory());
                final Set<Variable> ls2 = o2.getSecond().variables(sdd.getFactory());
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


    public static LngResult<SddNode> getNodeOfPartition(final TreeSet<SddElement> newElements, final Sdd sf,
                                                        final ComputationHandler handler) {
        final LngResult<Pair<SddNode, TreeSet<SddElement>>> res =
                Util.compressAndTrim(newElements, sf, handler);
        if (!res.isSuccess()) {
            LngResult.canceled(res.getCancelCause());
        }
        if (res.getResult().getFirst() != null) {
            return LngResult.of(res.getResult().getFirst());
        } else {
            return LngResult.of(sf.decomposition(res.getResult().getSecond()));
        }
    }

    private static LngResult<Pair<SddNode, TreeSet<SddElement>>> compressAndTrim(final TreeSet<SddElement> elements,
                                                                                 final Sdd sf,
                                                                                 final ComputationHandler handler) {
        assert !elements.isEmpty();

        final SddNode firstSub = elements.first().getSub();
        final SddNode lastSub = elements.last().getSub();

        if (firstSub == lastSub) {
            return LngResult.of(new Pair<>(firstSub, null));
        }

        // Trimming rule: node has form prime.T + ~prime.F, return prime
        if (firstSub.isTrue() && lastSub.isFalse()) {
            SddNode prime = sf.falsum();
            for (final SddElement element : elements) {
                if (!element.getSub().isTrue()) {
                    break;
                }
                final LngResult<SddNode> primeRes = sf.disjunction(element.getPrime(), prime, handler);
                if (!primeRes.isSuccess()) {
                    return LngResult.canceled(primeRes.getCancelCause());
                }
                prime = primeRes.getResult();
                assert !prime.isTrivial();
            }
            return LngResult.of(new Pair<>(prime, null));
        }

        //no trimming
        //pop uncompressed elements, compressing and placing compressed elements on element_stack
        final LngResult<TreeSet<SddElement>> compressedElements = compress(elements, sf, handler);
        return LngResult.of(new Pair<>(null, compressedElements.getResult()));
    }

    public static LngResult<TreeSet<SddElement>> compress(final TreeSet<SddElement> product, final Sdd sf,
                                                          final ComputationHandler handler) {
        final TreeSet<SddElement> compressed = new TreeSet<>();
        SddNode prevPrime = null;
        SddNode prevSub = null;
        SddElement prev = null;
        for (final SddElement current : product) {
            if (prevPrime == null) {
                prevPrime = current.getPrime();
                prevSub = current.getSub();
                prev = current;
                continue;
            }
            if (current.getSub() == prevSub) {
                final LngResult<SddNode> prevPrimeRes = sf.disjunction(current.getPrime(), prevPrime, handler);
                if (!prevPrimeRes.isSuccess()) {
                    return LngResult.canceled(prevPrimeRes.getCancelCause());
                }
                prevPrime = prevPrimeRes.getResult();
                prev = null;
            } else {
                if (prev != null) {
                    compressed.add(prev);
                } else {
                    compressed.add(new SddElement(prevPrime, prevSub));
                }
                prevPrime = current.getPrime();
                prevSub = current.getSub();
                prev = current;
            }
        }
        if (prev == null) {
            compressed.add(new SddElement(prevPrime, prevSub));
        } else {
            compressed.add(prev);
        }
        return LngResult.of(compressed);
    }
}
