package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import org.jetbrains.annotations.NotNull;

public class SddElement implements Comparable<SddElement> {
    private final SddNode prime;
    private final SddNode sub;

    public SddElement(final SddNode prime,
                      final SddNode sub) {
        this.prime = prime;
        this.sub = sub;
    }

    public SddNode getPrime() {
        return prime;
    }

    public SddNode getSub() {
        return sub;
    }

    @Override
    public int compareTo(@NotNull final SddElement o) {
        final int cSub = sub.compareTo(o.sub);
        if (cSub != 0) {
            return cSub;
        }
        return prime.compareTo(o.prime);
    }

    @Override
    public String toString() {
        return "<" + prime + "," + sub + ">";
    }
}
