package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A collection of functions related to compression of SDDs.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 */
public class SddCompression {
    private SddCompression() {

    }

    /**
     * Checks whether a collection of elements is compressed.
     * <p>
     * A set of SDD elements is called compressed if all sub nodes of the
     * elements are different.
     * <p>
     * <i>Preconditions:</i> {@code elements} must be sorted with {@link SddElement#compareTo(SddElement)}.
     * @param elements the elements
     * @return whether the elements are compressed
     */
    public static boolean isCompressed(final Collection<SddElement> elements) {
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

    /**
     * Compresses and trims a list of elements.
     * <p>
     * The function either returns an existing SDD node if one of the trimming
     * rules applies or a list of compressed elements.
     * <p>
     * A set of SDD elements is called compressed if all sub nodes of the
     * elements are different.  Furthermore, it is called trimmed if it does not
     * contain {@code {(true, α)}} or {@code {(α, true), (~α, false)}}.
     * <p>
     * <ul>
     * <li><i>Preconditions:</i> The primes of {@code elements} must form a
     * complete partition of {@code true} and {@code elements} must be sorted
     * with {@link SddElement#compareTo(SddElement)}.</li>
     * <li><i>Postconditions:</i> Exactly one field of the pair is null.  If a
     * list of elements is returned, it is an SDD decomposition.</li>
     * </ul>
     * @param elements the SDD elements
     * @param sdd      the SDD container
     * @param handler  the computation handler
     * @return either an existing SDD node or list of elements that are an SDD decomposition
     */
    public static LngResult<Pair<SddNode, ArrayList<SddElement>>> compressAndTrim(final ArrayList<SddElement> elements,
                                                                                  final Sdd sdd,
                                                                                  final ComputationHandler handler) {
        assert !elements.isEmpty();

        final SddNode firstSub = elements.get(0).getSub();
        final SddNode lastSub = elements.get(elements.size() - 1).getSub();

        if (firstSub == lastSub) {
            return LngResult.of(new Pair<>(firstSub, null));
        }

        // Trimming rule: node has form prime.T + ~prime.F, return prime
        if (firstSub.isTrue() && lastSub.isFalse()) {
            SddNode prime = sdd.falsum();
            for (final SddElement element : elements) {
                if (!element.getSub().isTrue()) {
                    break;
                }
                final LngResult<SddNode> primeRes = sdd.disjunction(element.getPrime(), prime, handler);
                if (!primeRes.isSuccess()) {
                    return LngResult.canceled(primeRes.getCancelCause());
                }
                prime = primeRes.getResult();
                assert !prime.isTrivial();
            }
            return LngResult.of(new Pair<>(prime, null));
        }

        final LngResult<ArrayList<SddElement>> compressedElements = compress(elements, sdd, handler);
        if (!compressedElements.isSuccess()) {
            return LngResult.canceled(compressedElements.getCancelCause());
        }
        return LngResult.of(new Pair<>(null, compressedElements.getResult()));
    }


    /**
     * Compresses a list of elements.
     * <p>
     * The function either returns an existing SDD node if one of the trimming
     * rules applies or a list of compressed elements.
     * <p>
     * A set of SDD elements is called compressed if all sub nodes of the
     * elements are different.
     * <p>
     * <ul>
     * <li><i>Preconditions:</i> The primes of {@code elements} must form a
     * complete partition of {@code true} and {@code elements} must be sorted
     * with {@link SddElement#compareTo(SddElement)}.</li>
     * <li><i>Postconditions:</i> The list of elements returned is an SDD
     * decomposition.</li>
     * </ul>
     * @param elements the SDD elements
     * @param sdd      the SDD container
     * @param handler  the computation handler
     * @return list of compressed elements that are an SDD decomposition
     */
    public static LngResult<ArrayList<SddElement>> compress(final ArrayList<SddElement> elements, final Sdd sdd,
                                                            final ComputationHandler handler) {
        final ArrayList<SddElement> compressed = new ArrayList<>();
        SddNode prevPrime = null;
        SddNode prevSub = null;
        SddElement prev = null;
        for (final SddElement current : elements) {
            if (prevPrime == null) {
                prevPrime = current.getPrime();
                prevSub = current.getSub();
                prev = current;
                continue;
            }
            if (current.getSub() == prevSub) {
                final LngResult<SddNode> prevPrimeRes = sdd.disjunction(current.getPrime(), prevPrime, handler);
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
