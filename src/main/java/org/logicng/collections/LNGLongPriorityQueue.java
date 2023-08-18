// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.collections;

/**
 * A simple priority queue implementation for elements with long priorities taken from CleaneLing.
 * @version 1.3
 * @since 1.0
 */
public final class LNGLongPriorityQueue {

    private final LNGIntVector heap;
    private final LNGLongVector prior;
    private final LNGIntVector pos;

    /**
     * Creates a new priority queue.
     */
    public LNGLongPriorityQueue() {
        heap = new LNGIntVector();
        prior = new LNGLongVector();
        pos = new LNGIntVector();
    }

    /**
     * Returns the left position on the heap for a given position.
     * @param position the position
     * @return the left position
     */
    private static int left(final int position) {
        return 2 * position + 1;
    }

    /**
     * Returns the right position on the heap for a given position.
     * @param position the position
     * @return the right position
     */
    private static int right(final int position) {
        return 2 * position + 2;
    }

    /**
     * Returns the parent position on the heap for a given position.
     * @param position the position
     * @return the parent position
     */
    private static int parent(final int position) {
        assert position > 0;
        return (position - 1) / 2;
    }

    /**
     * Returns whether the queue is empty or not.
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    public boolean empty() {
        return heap.empty();
    }

    /**
     * Returns the size of the queue.
     * @return the size of the queue
     */
    public int size() {
        return heap.size();
    }

    /**
     * Returns whether a given element is already imported and present in the queue or not.
     * @param element the element
     * @return {@code true} if the element is already imported and present in the queue, {@code false otherwise}.
     */
    public boolean contains(final int element) {
        return element >= 0 && imported(element) && pos.get(Math.abs(element)) >= 0;
    }

    /**
     * Returns the priority for a given element.
     * @param element the element
     * @return the priority of the element
     */
    public long priority(final int element) {
        assert imported(element);
        return prior.get(Math.abs(element));
    }

    /**
     * Returns the top element of the priority queue (= the element with the largest priority).
     * @return the top element of the priority queue
     */
    public int top() {
        return heap.get(0);
    }

    /**
     * Pushes a new element to the queue.
     * @param element the element
     * @throws IllegalArgumentException if the element to add is negative
     */
    public void push(final int element) {
        if (element < 0) {
            throw new IllegalArgumentException("Cannot add negative integers to the priority queue");
        }
        assert !contains(element);
        doImport(element);
        pos.set(element, heap.size());
        heap.push(element);
        assert heap.get(pos.get(element)) == element;
        up(element);
    }

    /**
     * Updated the priority of a given element.
     * @param element  the element
     * @param priority the new priority
     */
    public void update(final int element, final long priority) {
        doImport(element);
        final long q = prior.get(element);
        if (q == priority) {
            return;
        }
        prior.set(element, priority);
        if (pos.get(element) < 0) {
            return;
        }
        if (priority < q) {
            down(element);
        }
        if (q < priority) {
            up(element);
        }
    }

    /**
     * Removes a given element from the priority queue.  Its priority is kept as is.
     * @param element the element
     */
    public void pop(final int element) {
        assert contains(element);
        final int i = pos.get(element);
        pos.set(element, -1);
        final int last = heap.back();
        heap.pop();
        final int j = heap.size();
        if (i == j) {
            return;
        }
        assert i < j;
        pos.set(last, i);
        heap.set(i, last);
        up(last);
        down(last);
    }

    /**
     * Removes the top element from the priority queue.
     */
    public void pop() {
        pop(top());
    }

    /**
     * Compares two elements by their priority and returns whether the first element's priority is less than the second
     * element's priority.
     * @param e1 the first element
     * @param e2 the second element
     * @return {@code true} if the priority of the first element is less than the priority of the second element
     */
    private boolean less(final int e1, final int e2) {
        return prior.get(e1) < prior.get(e2);
    }

    /**
     * Bubbles a given element up.
     * @param element the element
     */
    private void up(final int element) {
        int epos = pos.get(element);
        while (epos > 0) {
            final int ppos = parent(epos);
            final int p = heap.get(ppos);
            if (!less(p, element)) {
                break;
            }
            heap.set(epos, p);
            heap.set(ppos, element);
            pos.set(p, epos);
            epos = ppos;
        }
        pos.set(element, epos);
    }

    /**
     * Bubbles a given element down.
     * @param element the element
     */
    private void down(final int element) {
        assert contains(element);
        int epos = pos.get(element);
        final int size = heap.size();
        while (true) {
            int cpos = left(epos);
            if (cpos >= size) {
                break;
            }
            int c = heap.get(cpos);
            final int o;
            final int opos = right(epos);
            if (!less(element, c)) {
                if (opos >= size) {
                    break;
                }
                o = heap.get(opos);
                if (!less(element, o)) {
                    break;
                }
                cpos = opos;
                c = o;
            } else if (opos < size) {
                o = heap.get(opos);
                if (!less(o, c)) {
                    cpos = opos;
                    c = o;
                }
            }
            heap.set(cpos, element);
            heap.set(epos, c);
            pos.set(c, epos);
            epos = cpos;
        }
        pos.set(element, epos);
    }

    /**
     * Returns whether a given element is already imported.
     * @param element the element
     * @return {@code true} if the element is imported, {@code false} otherwise
     */
    private boolean imported(final int element) {
        assert 0 <= element;
        return element < pos.size();
    }

    /**
     * Imports a given element.
     * @param element the element
     */
    private void doImport(final int element) {
        while (!imported(element)) {
            pos.push(-1);
            prior.push(0);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LNGLongPriorityQueue{");
        for (int i = 0; i < heap.size(); i++) {
            sb.append(String.format("<elem=%d, pos=%d, prio=%d>", heap.get(i), pos.get(i), prior.get(i)));
            if (i != heap.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}

