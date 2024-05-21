// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Glucose -- Copyright (c) 2009-2014, Gilles Audemard, Laurent Simon CRIL -
 * Univ. Artois, France LRI - Univ. Paris Sud, France (2009-2013) Labri - Univ.
 * Bordeaux, France <p> Syrup (Glucose Parallel) -- Copyright (c) 2013-2014,
 * Gilles Audemard, Laurent Simon CRIL - Univ. Artois, France Labri - Univ.
 * Bordeaux, France <p> Glucose sources are based on MiniSat (see below MiniSat
 * copyrights). Permissions and copyrights of Glucose (sources until 2013,
 * Glucose 3.0, single core) are exactly the same as Minisat on which it is
 * based on. (see below). <p> Glucose-Syrup sources are based on another
 * copyright. Permissions and copyrights for the parallel version of
 * Glucose-Syrup (the "Software") are granted, free of charge, to deal with the
 * Software without restriction, including the rights to use, copy, modify,
 * merge, publish, distribute, sublicence, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions: <p> - The above and below copyrights notices and
 * this permission notice shall be included in all copies or substantial
 * portions of the Software; - The parallel version of Glucose (all files
 * modified since Glucose 3.0 releases, 2013) cannot be used in any competitive
 * event (sat competitions/evaluations) without the express permission of the
 * authors (Gilles Audemard / Laurent Simon). This is also the case for any
 * competitive event using Glucose Parallel as an embedded SAT engine (single
 * core or not). <p> <p> --------------- Original Minisat Copyrights <p>
 * Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson Copyright (c)
 * 2007-2010, Niklas Sorensson <p> Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following
 * conditions: <p> The above copyright notice and this permission notice shall
 * be included in all copies or substantial portions of the Software. <p> THE
 * SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.booleworks.logicng.solvers.datastructures;

import com.booleworks.logicng.collections.LNGLongVector;

/**
 * A bounded long queue (for Glucose)
 * @version 1.3
 * @since 1.0
 */
public final class LNGBoundedLongQueue {
    private final LNGLongVector elems;
    private int first;
    private int last;
    private long sumOfQueue;
    private int maxSize;
    private int queueSize;

    /**
     * Constructs a new bounded long queue.
     */
    public LNGBoundedLongQueue() {
        elems = new LNGLongVector();
        first = 0;
        last = 0;
        sumOfQueue = 0;
        maxSize = 0;
        queueSize = 0;
    }

    public LNGBoundedLongQueue(final LNGLongVector elems, final int first, final int last, final long sumOfQueue,
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
        elems.growTo(size, 0);
        first = 0;
        maxSize = size;
        queueSize = 0;
        last = 0;
    }

    /**
     * Pushes a new element to the queue.
     * @param x the new element
     */
    public void push(final long x) {
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
     * Returns {@code true} if this queue is valid (the queue is filled
     * completely) or {@code false} if it is not.
     * @return {@code true} if this queue is valid
     */
    public boolean valid() {
        return queueSize == maxSize;
    }

    public LNGLongVector getElems() {
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

    /**
     * Performs a fast clear of this queue (the elements are left untouched).
     */
    public void fastClear() {
        first = 0;
        last = 0;
        queueSize = 0;
        sumOfQueue = 0;
    }

    @Override
    public String toString() {
        return String.format(
                "LNGBoundedLongQueue{first=%d, last=%d, sumOfQueue=%d, maxSize=%d, queueSize=%d, elems=%s}",
                first, last, sumOfQueue, maxSize, queueSize, elems);
    }
}
