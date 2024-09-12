// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple vector implementation (inspired by MiniSat, CleaneLing, Sat4J).
 * @param <T> the type of the elements
 * @version 1.0
 * @since 1.0
 */
public final class LNGVector<T> implements Iterable<T> {

    private T[] elements;
    private int size;

    /**
     * Creates a vector with an initial capacity of 5 elements.
     */
    public LNGVector() {
        this(5);
    }

    /**
     * Creates a vector with a given capacity.
     * @param size the capacity of the vector.
     */
    @SuppressWarnings("unchecked")
    public LNGVector(final int size) {
        elements = (T[]) new Object[size];
    }

    /**
     * Creates a vector with a given capacity and a given initial element.
     * @param size the capacity of the vector
     * @param pad  the initial element
     */
    @SuppressWarnings("unchecked")
    public LNGVector(final int size, final T pad) {
        elements = (T[]) new Object[size];
        Arrays.fill(elements, pad);
        this.size = size;
    }

    /**
     * Creates a vector with the given elements.
     * @param elems the elements
     */
    @SafeVarargs
    public LNGVector(final T... elems) {
        elements = Arrays.copyOf(elems, elems.length);
        size = elems.length;
    }

    /**
     * Creates a vector with the given elements
     * @param elems the elements
     */
    @SuppressWarnings("unchecked")
    public LNGVector(final Collection<T> elems) {
        elements = (T[]) new Object[elems.size()];
        int count = 0;
        for (final T e : elems) {
            elements[count++] = e;
        }
        size = elems.size();
    }

    /**
     * Returns whether the vector is empty or not.
     * @return {@code true} if the vector is empty, {@code false} otherwise
     */
    public boolean empty() {
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
    public T back() {
        return elements[size - 1];
    }

    /**
     * Pushes an element at the end of the vector.
     * @param element the element
     */
    public void push(final T element) {
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
    public void unsafePush(final T element) {
        elements[size++] = element;
    }

    /**
     * Returns the element at a given position in the vector.
     * @param position the position
     * @return the element at the position
     * @throws ArrayIndexOutOfBoundsException if the position is not found in
     *                                        the vector
     */
    public T get(final int position) {
        return elements[position];
    }

    /**
     * Sets an element at a given position in the vector.
     * @param position the position
     * @param element  the element
     * @throws ArrayIndexOutOfBoundsException if the position is not found in
     *                                        the vector
     */
    public void set(final int position, final T element) {
        elements[position] = element;
    }

    /**
     * Removes the last element of the vector.
     */
    public void pop() {
        elements[--size] = null;
    }

    /**
     * Shrinks the vector to a given size if the new size is less than the
     * current size. Otherwise, the size remains the same.
     * @param newSize the new size
     */
    public void shrinkTo(final int newSize) {
        if (newSize < size) {
            for (int i = size; i > newSize; i--) {
                elements[i - 1] = null;
            }
            size = newSize;
        }
    }

    /**
     * Grows the vector to a new size and initializes the new elements with a
     * given value.
     * @param size the new size
     * @param pad  the value for new elements
     */
    public void growTo(final int size, final T pad) {
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
     * Grows the vector to a new size and initializes the new elements with
     * {@code null}.
     * @param size the new size
     */
    public void growTo(final int size) {
        if (this.size >= size) {
            return;
        }
        ensure(size);
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
        int n = num;
        while (n-- > 0) {
            elements[--size] = null;
        }
    }

    /**
     * Removes the first occurrence of a given element from the vector (the
     * element is compared by {@code equals}).
     * @param element the element to remove
     * @return {@code true} if the element was removed, {@code false} if the
     *         element was not in the vector
     */
    public boolean remove(final T element) {
        for (int i = 0; i < size; i++) {
            if (elements[i].equals(element)) {
                System.arraycopy(elements, i + 1, elements, i, size - (i + 1));
                size--;
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces the contents of this vector with the contents of another vector
     * in-place.
     * @param other the other vector
     * @throws IllegalArgumentException if you try to replace a vector with
     *                                  itself
     */
    @SuppressWarnings("unchecked")
    public void replaceInplace(final LNGVector<? extends T> other) {
        if (this == other) {
            throw new IllegalArgumentException("cannot replace a vector in-place with itself");
        }
        elements = (T[]) new Object[other.size()];
        for (int i = 0; i < other.size(); i++) {
            elements[i] = other.get(i);
        }
        size = other.size;
    }

    /**
     * Clears the vector. This method only sets the size to 0. The elements are
     * not nulled out. Use {@link #release()} for this purpose.
     */
    public void clear() {
        size = 0;
    }

    /**
     * Clears the vector and sets all elements to {@code null}.
     */
    public void release() {
        Arrays.fill(elements, null);
        size = 0;
    }

    /**
     * Sorts this vector with a given comparator (with JDK sorting).
     * @param comparator the comparator
     */
    public void sort(final Comparator<T> comparator) {
        Arrays.sort(elements, 0, size, comparator);
    }

    /**
     * Sorts this vector with a given comparator (with manual implemented merge
     * sort). This method is required for sorting clauses based on activity,
     * since not all requirements of the JDK sorting are met.
     * @param comparator the comparator
     */
    public void manualSort(final Comparator<T> comparator) {
        sort(elements, 0, size, comparator);
    }

    /**
     * Selection sort implementation for a given array.
     * @param array the array
     * @param start the start index for sorting
     * @param end   the end index for sorting
     * @param lt    the comparator for elements of the array
     */
    private void selectionSort(final T[] array, final int start, final int end, final Comparator<T> lt) {
        int i;
        int j;
        int bestI;
        T tmp;
        for (i = start; i < end; i++) {
            bestI = i;
            for (j = i + 1; j < end; j++) {
                if (lt.compare(array[j], array[bestI]) < 0) {
                    bestI = j;
                }
            }
            tmp = array[i];
            array[i] = array[bestI];
            array[bestI] = tmp;
        }
    }

    /**
     * Merge sort implementation for a given array.
     * @param array the array
     * @param start the start index for sorting
     * @param end   the end index for sorting
     * @param lt    the comparator for elements of the array
     */
    private void sort(final T[] array, final int start, final int end, final Comparator<T> lt) {
        if (start == end) {
            return;
        }
        if ((end - start) <= 15) {
            selectionSort(array, start, end, lt);
        } else {
            final T pivot = array[start + ((end - start) / 2)];
            T tmp;
            int i = start - 1;
            int j = end;
            while (true) {
                do {
                    i++;
                } while (lt.compare(array[i], pivot) < 0);
                do {
                    j--;
                } while (lt.compare(pivot, array[j]) < 0);
                if (i >= j) {
                    break;
                }
                tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
            }
            sort(array, start, i, lt);
            sort(array, i, end, lt);
        }
    }

    /**
     * Sorts this vector in reverse order.
     * @param comparator the comparator
     */
    public void sortReverse(final Comparator<T> comparator) {
        sort(comparator);
        for (int i = 0; i < size / 2; i++) {
            final T temp = elements[i];
            elements[i] = elements[size - i - 1];
            elements[size - i - 1] = temp;
        }
    }

    /**
     * Ensures that this vector has the given size. If not - the size is doubled
     * and the old elements are copied.
     * @param newSize the size to ensure
     */
    @SuppressWarnings("unchecked")
    private void ensure(final int newSize) {
        if (newSize >= elements.length) {
            final T[] newArray = (T[]) new Object[Math.max(newSize, size * 2)];
            System.arraycopy(elements, 0, newArray, 0, size);
            elements = newArray;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int i;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public T next() {
                if (i == size) {
                    throw new NoSuchElementException();
                }
                return elements[i++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
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
