// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.collections;

import java.util.Arrays;

/**
 * A simple vector for double elements implementation (inspired by MiniSat,
 * CleaneLing, Sat4J).
 * <p>
 * In theory one could use the {@link LngVector} also for doubles. But Java's
 * auto-boxing comes with such a large performance penalty that for the
 * mission-critical data structures of the SAT solvers we use this specialized
 * implementation.
 * @version 3.0.0
 * @since 1.0
 */
public final class LngDoubleVector {

    private double[] elements;
    private int size;

    /**
     * Creates a vector with an initial capacity of 5 elements.
     */
    public LngDoubleVector() {
        this(5);
    }

    /**
     * Creates a vector with a given capacity.
     * @param size the capacity of the vector.
     */
    public LngDoubleVector(final int size) {
        elements = new double[size];
    }

    /**
     * Creates a vector with a given capacity and a given initial element.
     * @param size the capacity of the vector
     * @param pad  the initial element
     */
    public LngDoubleVector(final int size, final double pad) {
        elements = new double[size];
        Arrays.fill(elements, pad);
        this.size = size;
    }

    /**
     * Copy constructor.
     * @param other the other byte vector.
     */
    public LngDoubleVector(final LngDoubleVector other) {
        elements = Arrays.copyOf(other.elements, other.size);
        size = other.size;
    }

    /**
     * Creates a vector with the given elements.
     * @param elems the elements
     */
    public LngDoubleVector(final double... elems) {
        elements = Arrays.copyOf(elems, elems.length);
        size = elems.length;
    }

    /**
     * Returns whether the vector is empty or not.
     * @return {@code true} if the vector is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the size of the vector.
     * @return the size of the vector
     */
    public int size() {
        return size;
    }

    /**
     * Returns the last element of the vector and leaves it on the vector.
     * @return the last element of the vector
     */
    public double back() {
        return elements[size - 1];
    }

    /**
     * Pushes an element at the end of the vector.
     * @param element the element to push
     */
    public void push(final double element) {
        final int newSize = size + 1;
        ensure(newSize);
        elements[size++] = element;
    }

    /**
     * Pushes an element and assumes that there is enough space on the vector.
     * @param element the element to push
     * @throws ArrayIndexOutOfBoundsException if there was not enough space on
     *                                        the vector
     */
    public void unsafePush(final double element) {
        elements[size++] = element;
    }

    /**
     * Returns the element at a given position in the vector.
     * @param position the position
     * @return the element at the position
     * @throws ArrayIndexOutOfBoundsException if the position is not found in
     *                                        the vector
     */
    public double get(final int position) {
        return elements[position];
    }

    /**
     * Sets an element at a given position in the vector.
     * @param position the position
     * @param element  the element
     * @throws ArrayIndexOutOfBoundsException if the position is not found in
     *                                        the vector
     */
    public void set(final int position, final double element) {
        elements[position] = element;
    }

    /**
     * Removes the last element of the vector.
     */
    public void pop() {
        elements[--size] = -1.0;
    }

    /**
     * Shrinks the vector to a given size.
     * @param newSize the new size
     */
    public void shrinkTo(final int newSize) {
        if (newSize < size) {
            size = newSize;
        }
    }

    /**
     * Grows the vector to a new size and initializes the new elements with a
     * given value.
     * @param size the new size
     * @param pad  the value for new elements
     */
    public void growTo(final int size, final double pad) {
        if (this.size >= size) {
            return;
        }
        ensure(size);
        for (int i = this.size; i < size; i++) {
            elements[i] = pad;
        }
        this.size = size;
    }

    /**
     * Removes a given number of elements from the vector.
     * @param num the number of elements to remove.
     * @throws ArrayIndexOutOfBoundsException if the number of elements to
     *                                        remove is larger than the size of
     *                                        the vector
     */
    public void removeElements(final int num) {
        int count = num;
        while (count-- > 0) {
            elements[--size] = -1;
        }
    }

    /**
     * Clears the vector.
     */
    public void clear() {
        size = 0;
    }

    /**
     * Sorts this vector.
     */
    public void sort() {
        Arrays.sort(elements, 0, size);
    }

    /**
     * Sorts this vector in reverse order.
     */
    public void sortReverse() {
        Arrays.sort(elements, 0, size);
        for (int i = 0; i < size / 2; i++) {
            final double temp = elements[i];
            elements[i] = elements[size - i - 1];
            elements[size - i - 1] = temp;
        }
    }

    /**
     * Returns this vector's contents as an array.
     * @return the array
     */
    public double[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    /**
     * Ensures that this vector has the given size. If not - the size is doubled
     * and the old elements are copied.
     * @param newSize the size to ensure
     */
    private void ensure(final int newSize) {
        if (newSize >= elements.length) {
            final double[] newArray = new double[Math.max(newSize, size * 2)];
            System.arraycopy(elements, 0, newArray, 0, size);
            elements = newArray;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(elements[i]);
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
