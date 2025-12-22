// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

import com.booleworks.logicng.collections.LngIntVector;

/**
 * A bounded integer queue (for Glucose)
 * @version 1.3
 * @since 1.0
 */
public final class LngBoundedIntQueue {
    private final LngIntVector elems;
    private int first;
    private int last;
    private long sumOfQueue;
    private int maxSize;
    private int queueSize;

    /**
     * Constructs a new bounded int queue.
     */
    public LngBoundedIntQueue() {
        elems = new LngIntVector();
        first = 0;
        last = 0;
        sumOfQueue = 0;
        maxSize = 0;
        queueSize = 0;
    }

    public LngBoundedIntQueue(final LngIntVector elems, final int first, final int last, final long sumOfQueue,
                              final int maxSize, final int queueSize) {
        this.elems = elems;
        this.first = first;
        this.last = last;
        this.sumOfQueue = sumOfQueue;
        this.maxSize = maxSize;
        this.queueSize = queueSize;
    }

    /**
     * Initializes the size of this queue.
     * @param size the size
     */
    public void initSize(final int size) {
        growTo(size);
    }

    /**
     * Pushes a new element to the queue.
     * @param x the new element
     */
    public void push(final int x) {
        if (queueSize == maxSize) {
            assert last == first;
            sumOfQueue -= elems.get(last);
            if ((++last) == maxSize) {
                last = 0;
            }
        } else {
            queueSize++;
        }
        sumOfQueue += x;
        elems.set(first, x);
        if ((++first) == maxSize) {
            first = 0;
            last = 0;
        }
    }

    /**
     * Returns the average value of this queue.
     * @return the average value of this queue
     */
    public int avg() {
        return (int) (sumOfQueue / queueSize);
    }

    /**
     * Grows this queue to a given size.
     * @param size the size
     */
    private void growTo(final int size) {
        elems.growTo(size, 0);
        first = 0;
        maxSize = size;
        queueSize = 0;
        last = 0;
    }

    public LngIntVector getElems() {
        return elems;
    }

    public int getFirst() {
        return first;
    }

    public int getLast() {
        return last;
    }

    public long getSumOfQueue() {
        return sumOfQueue;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    @Override
    public String toString() {
        return String.format("LngBoundedIntQueue{first=%d, last=%d, sumOfQueue=%d, maxSize=%d, queueSize=%d, elems=%s}",
                first, last, sumOfQueue, maxSize, queueSize, elems);
    }
}
