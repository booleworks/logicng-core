// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * MiniSat -- Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.solvers.datastructures;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

/**
 * A minimalistic heap implementation.
 * @version 1.3
 * @since 1.0
 */
public final class LNGHeap {

    private final LNGCoreSolver s;
    private final LNGIntVector heap;
    private final LNGIntVector indices;

    /**
     * Constructs a new heap for a given solver.  The solver is required to access its activity information stored
     * for variables.  The initial size of the heap is 1000 elements.
     * @param solver the solver
     */
    public LNGHeap(final LNGCoreSolver solver) {
        s = solver;
        heap = new LNGIntVector(1000);
        indices = new LNGIntVector(1000);
    }

    LNGHeap(final LNGCoreSolver s, final LNGIntVector heap, final LNGIntVector indices) {
        this.s = s;
        this.heap = heap;
        this.indices = indices;
    }

    /**
     * Returns the left position on the heap for a given position.
     * @param pos the position
     * @return the left position
     */
    private static int left(final int pos) {
        return pos * 2 + 1;
    }

    /**
     * Returns the right position on the heap for a given position.
     * @param pos the position
     * @return the right position
     */
    private static int right(final int pos) {
        return (pos + 1) * 2;
    }

    /**
     * Returns the parent position on the heap for a given position.
     * @param pos the position
     * @return the parent position
     */
    private static int parent(final int pos) {
        return (pos - 1) >> 1;
    }

    /**
     * Returns the size of the heap.
     * @return the size of the heap
     */
    public int size() {
        return heap.size();
    }

    /**
     * Returns {@code true} if the heap ist empty, {@code false} otherwise.
     * @return {@code true} if the heap ist empty
     */
    public boolean empty() {
        return heap.size() == 0;
    }

    /**
     * Returns {@code true} if a given element is in the heap, {@code false} otherwise.
     * @param n the element
     * @return {@code true} if a given variable index is in the heap
     */
    public boolean inHeap(final int n) {
        return n < indices.size() && indices.get(n) >= 0;
    }

    /**
     * Returns the element at a given position in the heap.
     * @param index the position
     * @return the element at the position
     */
    public int get(final int index) {
        assert index < heap.size();
        return heap.get(index);
    }

    /**
     * Decrease an element's position in the heap
     * @param n the element
     */
    public void decrease(final int n) {
        assert inHeap(n);
        percolateUp(indices.get(n));
    }

    /**
     * Inserts a given element in the heap.
     * @param n the element
     */
    public void insert(final int n) {
        indices.growTo(n + 1, -1);
        assert !inHeap(n);
        indices.set(n, heap.size());
        heap.push(n);
        percolateUp(indices.get(n));
    }

    /**
     * Removes the minimal element of the heap.
     * @return the minimal element of the heap
     */
    public int removeMin() {
        final int x = heap.get(0);
        heap.set(0, heap.back());
        indices.set(heap.get(0), 0);
        indices.set(x, -1);
        heap.pop();
        if (heap.size() > 1) {
            percolateDown(0);
        }
        return x;
    }

    /**
     * Removes a given element of the heap.
     * @param n the element
     */
    public void remove(final int n) {
        assert inHeap(n);
        final int kPos = indices.get(n);
        indices.set(n, -1);
        if (kPos < heap.size() - 1) {
            heap.set(kPos, heap.back());
            indices.set(heap.get(kPos), kPos);
            heap.pop();
            percolateDown(kPos);
        } else {
            heap.pop();
        }
    }

    /**
     * Rebuilds the heap from a given vector of elements.
     * @param ns the vector of elements
     */
    public void build(final LNGIntVector ns) {
        for (int i = 0; i < heap.size(); i++) {
            indices.set(heap.get(i), -1);
        }
        heap.clear();
        for (int i = 0; i < ns.size(); i++) {
            indices.set(ns.get(i), i);
            heap.push(ns.get(i));
        }
        for (int i = heap.size() / 2 - 1; i >= 0; i--) {
            percolateDown(i);
        }
    }

    /**
     * Clears the heap.
     */
    public void clear() {
        for (int i = 0; i < heap.size(); i++) {
            indices.set(heap.get(i), -1);
        }
        heap.clear();
    }

    /**
     * Bubbles a element at a given position up.
     * @param pos the position
     */
    private void percolateUp(final int pos) {
        final int x = heap.get(pos);
        int p = parent(pos);
        int j = pos;
        while (j != 0 && s.lt(x, heap.get(p))) {
            heap.set(j, heap.get(p));
            indices.set(heap.get(p), j);
            j = p;
            p = parent(p);
        }
        heap.set(j, x);
        indices.set(x, j);
    }

    /**
     * Bubbles a element at a given position down.
     * @param pos the position
     */
    private void percolateDown(final int pos) {
        int p = pos;
        final int y = heap.get(p);
        while (left(p) < heap.size()) {
            final int child = right(p) < heap.size() && s.lt(heap.get(right(p)), heap.get(left(p))) ? right(p) : left(p);
            if (!s.lt(heap.get(child), y)) {
                break;
            }
            heap.set(p, heap.get(child));
            indices.set(heap.get(p), p);
            p = child;
        }
        heap.set(p, y);
        indices.set(y, p);
    }

    LNGIntVector getHeap() {
        return heap;
    }

    LNGIntVector getIndices() {
        return indices;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LNGHeap{");
        for (int i = 0; i < heap.size(); i++) {
            sb.append("[").append(heap.get(i)).append(", ");
            sb.append(indices.get(i)).append("]");
            if (i != heap.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
