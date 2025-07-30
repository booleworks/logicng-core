package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;


import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Function for computing the product of two partitions.
 * @version 3.0.0
 * @since 3.0.0
 */
final class SddMultiply {
    private SddMultiply() {
    }

    /**
     * Computes the product of two partitions.
     * <p>
     * The resulting partition might not be compressed nor trimmed.
     * @param elements1 first partition
     * @param elements2 second partition
     * @param op        the binary operator
     * @param sdd       the SDD container
     * @param handler   the computation handler
     * @return the product of {@code elements1} and {@code elements2}
     */
    public static LngResult<ArrayList<SddElement>> multiplyDecompositions(final ArrayList<SddElement> elements1,
                                                                          final ArrayList<SddElement> elements2,
                                                                          final SddApplyOperation op, final Sdd sdd,
                                                                          final ComputationHandler handler) {
        final ArrayList<SddElement> e1Common = new ArrayList<>();
        final ArrayList<SddElement> e1Other = new ArrayList<>();
        final ArrayList<SddElement> e2Common = new ArrayList<>();
        final ArrayList<SddElement> e2Other = new ArrayList<>();
        final ArrayList<SddElement> newElements = new ArrayList<>();
        SddNode complementPrime1 = null, complementPrime2 = null, complementSub1 = null, complementSub2 = null;

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
            final SddNode cachedNegation = sdd.getNegationIfCached(element.getPrime());
            if (cachedNegation != null && !commonPrimes.contains(cachedNegation)) {
                complementPrime2 = element.getPrime();
                complementSub2 = element.getSub();
            }
        }

        for (final SddElement element : elements1) {
            if (commonPrimes.contains(element.getPrime())) {
                e1Common.add(element);
            } else {
                e1Other.add(element);
            }
            if (complementPrime2 != null && element.getPrime() == sdd.getNegationIfCached(complementPrime2)) {
                complementPrime1 = element.getPrime();
                complementSub1 = element.getSub();
            }
        }

        if (complementPrime1 == null) {
            //Multiply common elements the following fragment is quadratic in size1 and size2
            if (e1Common.size() * e2Common.size() <= 64) {
                final LngEvent cancelCause =
                        quadraticMultiplyCommonPrimes(e1Common, e2Common, op, sdd, handler, newElements);
                if (cancelCause != null) {
                    return LngResult.canceled(cancelCause);
                }
            } else {
                final HashMap<SddNode, SddNode> multiplySub = new HashMap<>();
                for (final SddElement element : e2Common) {
                    multiplySub.put(element.getPrime(), element.getSub());
                }
                for (final SddElement element : e1Common) {
                    if (multiplySub.containsKey(element.getPrime())) {
                        final SddNode prime = element.getPrime();
                        final LngResult<SddNode> sub =
                                sdd.binaryOperation(element.getSub(), multiplySub.get(element.getPrime()), op, handler);
                        if (!sub.isSuccess()) {
                            return LngResult.canceled(sub.getCancelCause());
                        }
                        newElements.add(new SddElement(prime, sub.getResult()));
                    }
                }
            }

            final LngEvent cancelCause = quadraticMultiply(e1Other, e2Other, op, sdd, handler, newElements);
            if (cancelCause != null) {
                return LngResult.canceled(cancelCause);
            }
        } else {
            //p1 and p2 are complementary: p1 in e1 and p2 in e2
            //multiply has LINEAR complexity in this case
            LngEvent event;
            event = linearMultiply(complementPrime1, complementSub2, op, e1Common, e1Other, sdd, handler,
                    newElements);
            if (event != null) {
                return LngResult.canceled(event);
            }
            event = linearMultiply(complementPrime2, complementSub1, op, e2Common, e2Other, sdd, handler,
                    newElements);
            if (event != null) {
                return LngResult.canceled(event);
            }
        }
        return LngResult.of(newElements);
    }

    /// Will remove elements from e2
    private static LngEvent quadraticMultiplyCommonPrimes(final Collection<SddElement> e1,
                                                          final Collection<SddElement> e2,
                                                          final SddApplyOperation op, final Sdd sf,
                                                          final ComputationHandler handler,
                                                          final ArrayList<SddElement> destination) {
        for (final SddElement element1 : e1) {
            final Iterator<SddElement> e2Iter = e2.iterator();
            while (e2Iter.hasNext()) {
                final SddElement element2 = e2Iter.next();
                if (element1.getPrime() == element2.getPrime()) {
                    final SddNode prime = element1.getPrime();
                    final LngResult<SddNode> sub =
                            sf.binaryOperation(element1.getSub(), element2.getSub(), op, handler);
                    if (!sub.isSuccess()) {
                        return sub.getCancelCause();
                    }
                    destination.add(new SddElement(prime, sub.getResult()));
                    e2Iter.remove();
                    break;
                }
            }
        }
        return null;
    }

    private static LngEvent quadraticMultiply(final Collection<SddElement> e1,
                                              final Collection<SddElement> e2,
                                              final SddApplyOperation op, final Sdd sf,
                                              final ComputationHandler handler,
                                              final ArrayList<SddElement> destination) {
        for (final SddElement element1 : e1) {
            final Iterator<SddElement> iter2 = e2.iterator();
            while (iter2.hasNext()) {
                final SddElement element2 = iter2.next();
                final LngResult<SddNode> primeResult =
                        sf.conjunction(element1.getPrime(), element2.getPrime(), handler);
                if (!primeResult.isSuccess()) {
                    return primeResult.getCancelCause();
                }
                final SddNode prime = primeResult.getResult();
                if (!prime.isFalse()) {
                    final LngResult<SddNode> sub =
                            sf.binaryOperation(element1.getSub(), element2.getSub(), op, handler);
                    if (!sub.isSuccess()) {
                        return sub.getCancelCause();
                    }
                    destination.add(new SddElement(prime, sub.getResult()));
                }
                if (prime == element1.getPrime()) {
                    break;
                }
                if (prime == element2.getPrime()) {
                    iter2.remove();
                }
            }
        }
        return null;
    }

    private static LngEvent linearMultiply(final SddNode prime, final SddNode complementarySub,
                                           final SddApplyOperation op, final ArrayList<SddElement> list1,
                                           final ArrayList<SddElement> list2, final Sdd sf,
                                           final ComputationHandler handler, final ArrayList<SddElement> destination) {
        for (int i = 0; i < list1.size() + list2.size(); ++i) {
            final SddElement element = i < list1.size() ? list1.get(i) : list2.get(i - list1.size());
            if (element.getPrime() == prime) {
                continue;
            }
            final SddNode newPrime = element.getPrime();
            final LngResult<SddNode> newSub = sf.binaryOperation(element.getSub(), complementarySub, op, handler);
            if (!newSub.isSuccess()) {
                return newSub.getCancelCause();
            }
            destination.add(new SddElement(newPrime, newSub.getResult()));
        }
        return null;
    }
}
