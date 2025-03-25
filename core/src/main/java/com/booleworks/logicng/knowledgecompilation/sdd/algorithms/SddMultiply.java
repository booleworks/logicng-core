package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;


import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

public class SddMultiply {
    private SddMultiply() {
    }

    /**
     * multiplying two decompositions
     * <p>
     * if 1st decomposition has elements pi.si and 2nd decomposition has elements pj.sj,
     * then define prime P = pi CONJOIN pj, sub S = si OP sj, and
     * call code fn for each P.S, where prime P is not false
     * <p>
     * when computing a product of two decompositions: (p11 p12 ... p1n) x (p21 p22 ... p2m)
     * --observation 1:
     * if p1i = p2j, then p1k x p2j = false for all k<>i
     * --observation 2:
     * if p1i = !p2j, then p1k x p2j = p1k for all k<>i
     * --observation 3:
     * if p1i*p2j=p2j, then pik x p2j = false for all k<>i
     * <p>
     * the above observations are used to skip some conjoin operations whose result
     * can be predicted upfront
     **/
    public static TreeSet<SddElement> multiplyDecompositions(final TreeSet<SddElement> elements1,
                                                             final TreeSet<SddElement> elements2,
                                                             final SddApplyOperation op, final VTree vTree,
                                                             final VTreeRoot root,
                                                             final SddFactory sf) {
        final ArrayList<SddElement> e1Common = new ArrayList<>();
        final ArrayList<SddElement> e1Other = new ArrayList<>();
        final ArrayList<SddElement> e2Common = new ArrayList<>();
        final ArrayList<SddElement> e2Other = new ArrayList<>();
        final TreeSet<SddElement> newElements = new TreeSet<>();
        SddNode p1 = null, p2 = null, s1 = null, s2 = null;

        final HashSet<SddNode> elements1Primes = new HashSet<>();
        for (final SddElement element : elements1) {
            elements1Primes.add(element.getPrime());
        }

        final HashSet<SddNode> commonPrimes = new HashSet<>();
        for (final SddElement element : elements2) {
            if (elements1Primes.contains(element.getPrime())) {
                commonPrimes.add(element.getPrime());
                e2Common.add(element);
            } else {
                e2Other.add(element);
            }
            final SddNode cachedNegation = sf.getNegationIfCached(element.getPrime());
            if (cachedNegation != null && !commonPrimes.contains(cachedNegation)) {
                p2 = element.getPrime();
                s2 = element.getSub();
            }
        }

        for (final SddElement element : elements1) {
            if (commonPrimes.contains(element.getPrime())) {
                e1Common.add(element);
            } else {
                e1Other.add(element);
            }
            if (p2 != null && element.getPrime() == sf.getNegationIfCached(p2)) {
                p1 = element.getPrime();
                s1 = element.getSub();
            }
        }

        if (p1 == null) {
            //Multiply common elements the following fragment is quadratic in size1 and size2
            if (e1Common.size() * e2Common.size() <= 64) {
                int jStart = 0;
                for (final SddElement element1 : e1Common) {
                    for (int j = jStart; j < e2Common.size(); ++j) {
                        final SddElement element2 = e2Common.get(j);
                        if (element1.getPrime() == element2.getPrime()) {
                            final SddNode prime = element1.getPrime();
                            final SddNode sub =
                                    SddApply.apply(element1.getSub(), element2.getSub(), op, root, sf);
                            Util.pushNewElement(prime, sub, vTree, root, newElements);
                            ++jStart;
                            break;
                        }
                    }
                }
                //multiply common elements the following fragment is linear in size1 and size2
            } else {
                final HashMap<SddNode, SddNode> multiplySub = new HashMap<>();
                for (final SddElement element : e2Common) {
                    multiplySub.put(element.getPrime(), element.getSub());
                }
                for (final SddElement element : e1Common) {
                    if (multiplySub.containsKey(element.getPrime())) {
                        final SddNode prime = element.getPrime();
                        final SddNode sub =
                                SddApply.apply(element.getSub(), multiplySub.get(element.getPrime()), op, root, sf);
                        Util.pushNewElement(prime, sub, vTree, root, newElements);
                    }
                }
            }

            for (final SddElement element1 : e1Other) {
                final Iterator<SddElement> iter2 = elements2.iterator();
                while (iter2.hasNext()) {
                    final SddElement element2 = iter2.next();
                    final SddNode prime =
                            SddApply.apply(element1.getPrime(), element2.getPrime(), SddApplyOperation.CONJUNCTION,
                                    root, sf);
                    if (!prime.isFalse()) {
                        final SddNode sub = SddApply.apply(element1.getSub(), element2.getSub(), op, root, sf);
                        Util.pushNewElement(prime, sub, vTree, root, newElements);
                    }
                    if (prime == element1.getPrime()) {
                        break;
                    }
                    if (prime == element2.getPrime()) {
                        iter2.remove();
                    }
                }
            }
        } else {
            //p1 and p2 are complementary: p1 in e1 and p2 in e2
            //multiply has LINEAR complexity in this case
            for (int i = 0; i < e1Common.size() + e1Other.size(); ++i) {
                final SddElement element = i < e1Common.size() ? e1Common.get(i) : e1Other.get(i);
                if (element.getPrime() == p1) {
                    continue;
                }
                final SddNode prime = element.getPrime();
                final SddNode sub = SddApply.apply(element.getSub(), s2, op, root, sf);
                Util.pushNewElement(prime, sub, vTree, root, newElements);
            }

            for (int i = 0; i < e2Common.size() + e2Other.size(); ++i) {
                final SddElement element = i < e2Common.size() ? e2Common.get(i) : e2Other.get(i);
                if (element.getPrime() == p2) {
                    continue;
                }
                final SddNode prime = element.getPrime();
                final SddNode sub = SddApply.apply(element.getSub(), s1, op, root, sf);
                Util.pushNewElement(prime, sub, vTree, root, newElements);
            }
        }
        return newElements;
    }

}
