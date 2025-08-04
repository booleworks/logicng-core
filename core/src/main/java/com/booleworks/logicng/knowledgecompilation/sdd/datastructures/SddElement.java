package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

public final class SddElement implements Comparable<SddElement> {
    private final SddNode prime;
    private final SddNode sub;

    public SddElement(final SddNode prime,
                      final SddNode sub) {
        assert !prime.isFalse();
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
    public int compareTo(final SddElement o) {
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

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof SddElement)) {
            return false;
        }

        final SddElement element = (SddElement) o;
        return prime.equals(element.prime) && sub.equals(element.sub);
    }

    @Override
    public int hashCode() {
        int result = prime.hashCode();
        result = 31 * result + sub.hashCode();
        return result;
    }
}
