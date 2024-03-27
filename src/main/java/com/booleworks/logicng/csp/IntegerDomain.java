///////////////////////////////////////////////////////////////////////////
//                   __                _      _   ________               //
//                  / /   ____  ____ _(_)____/ | / / ____/               //
//                 / /   / __ \/ __ `/ / ___/  |/ / / __                 //
//                / /___/ /_/ / /_/ / / /__/ /|  / /_/ /                 //
//               /_____/\____/\__, /_/\___/_/ |_/\____/                  //
//                           /____/                                      //
//                                                                       //
//               The Next Generation Logic Library                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////
//                                                                       //
//  Copyright 2015-20xx Christoph Zengler                                //
//                                                                       //
//  Licensed under the Apache License, Version 2.0 (the "License");      //
//  you may not use this file except in compliance with the License.     //
//  You may obtain a copy of the License at                              //
//                                                                       //
//  http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                       //
//  Unless required by applicable law or agreed to in writing, software  //
//  distributed under the License is distributed on an "AS IS" BASIS,    //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      //
//  implied.  See the License for the specific language governing        //
//  permissions and limitations under the License.                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////

/*
Azucar (A SAT-based CSP Solver) version 0.2.4

Copyright (c) 2012
by Tomoya Tanjo (tanjo @ nii.ac.jp),
   Naoyuki Tamura (tamura @ kobe-u.ac.jp), and
   Mutsunori Banbara (banbara @ kobe-u.ac.jp)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.
 * Neither the name of the Kobe University nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.booleworks.logicng.csp;

import java.util.Iterator;
import java.util.SortedSet;

/**
 * Super class for integer domains for constraints.  An integer domain can be contiguous
 * and thus only defined by a lower and upper bound ({@link IntegerRangeDomain}), or
 * defined by a set of concrete values ({@link IntegerSetDomain}).
 */
public abstract class IntegerDomain {
    public static int MAX_SET_SIZE = 128;

    protected final int lb;
    protected final int ub;

    protected IntegerDomain(final int lb, final int ub) {
        this.lb = lb;
        this.ub = ub;
    }

    /**
     * Returns the domain size.
     * @return the domain size
     */
    public abstract int size();

    /**
     * Returns whether the domain is empty or not.
     * @return whether the domain is empty or not
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns whether the domain contains the given element.
     * @param element the element to search for
     * @return true if the element is contained in the domain, false otherwise
     */
    public abstract boolean contains(final int element);

    /**
     * Returns whether the domain is contiguous thus a range domain or not and
     * thus a set domain.
     * @return whether the domain is contiguous
     */
    public abstract boolean isContiguous();

    /**
     * Returns a new domain containing all elements of this domain but bound by the new
     * given lower and upper bound.
     * @param lb the new lower bound
     * @param ub the new upper bound
     * @return the new bound domain
     */
    public abstract IntegerDomain bound(int lb, int ub);

    /**
     * Returns the values of this domain bound to a lower and upper bound as an iterator.
     * @param lb the lower bound
     * @param ub the upper bound
     * @return the iterator for the bound values
     */
    public abstract Iterator<Integer> values(int lb, int ub);

    /**
     * Returns a new domain which is this domain united with the given one.
     * @param d1 the other domain
     * @return the union of this and the other domain
     */
    public abstract IntegerDomain cup(IntegerDomain d1);

    /**
     * Returns a new domain which is this domain intersected with the given one.
     * @param d1 the other domain
     * @return the intersection of this and the other domain
     */
    public abstract IntegerDomain cap(IntegerDomain d1);

    /**
     * Returns a new domain where every element of this domain is negated.
     * @return a new domain where every element is negated
     */
    public abstract IntegerDomain neg();

    /**
     * Returns a new domain with the absolute values of all elements in this domain.
     * @return a new domain with the absolute values
     */
    public abstract IntegerDomain abs();

    /**
     * Returns a new domain where each value of this domain is increased by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain add(final int a);

    /**
     * Returns a new domain with all elements resulting in the addition of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the added values
     */
    public abstract IntegerDomain add(final IntegerDomain d);

    /**
     * Returns a new domain where each value of this domain is decreased by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public IntegerDomain sub(final int a) {
        return add(-a);
    }

    /**
     * Returns a new domain with all elements resulting in the subtraction of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the subtracted values
     */
    public IntegerDomain sub(final IntegerDomain d) {
        return add(d.neg());
    }

    /**
     * Returns a new domain where each value of this domain is multiplied by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain mul(int a);

    /**
     * Returns a new domain with all elements resulting in the multiplication of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the multiplied values
     */
    public abstract IntegerDomain mul(final IntegerDomain d);

    /**
     * Returns a new domain where each value of this domain is divided by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain div(int a);

    /**
     * Returns a new domain with all elements resulting in the division of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the divided values
     */
    public abstract IntegerDomain div(final IntegerDomain d);

    /**
     * Returns a new domain where each value of this domain is taken modulo by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain mod(int a);

    /**
     * Returns a new domain with all elements resulting in the modulation of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the moduled values
     */
    public abstract IntegerDomain mod(final IntegerDomain d);

    /**
     * Returns a new domain with all elements when taking the minimum of this domain with another domain.
     * @param d the other domain
     * @return a new domain with the min values
     */
    public abstract IntegerDomain min(final IntegerDomain d);

    /**
     * Returns a new domain with all elements when taking the maximum of this domain with another domain.
     * @param d the other domain
     * @return a new domain with the max values
     */
    public abstract IntegerDomain max(final IntegerDomain d);

    /**
     * Returns the lower bound of this domain.
     * @return the lower bound
     */
    public int lb() {
        return lb;
    }

    /**
     * Returns the upper bound of this domain.
     * @return the upper bound
     */
    public int ub() {
        return ub;
    }

    public Iterator<Integer> iterator() {
        return values(lb, ub);
    }

    protected static IntegerDomain create(final SortedSet<Integer> domain) {
        final int lb = domain.first();
        final int ub = domain.last();
        boolean createRange = false;
        if (domain.size() <= MAX_SET_SIZE) {
            boolean sparse = false;
            for (int value = lb; value <= ub; value++) {
                if (!domain.contains(value)) {
                    sparse = true;
                    break;
                }
            }
            if (!sparse) {
                createRange = true;
            }
        } else {
            createRange = true;
        }
        if (createRange) {
            return new IntegerRangeDomain(lb, ub);
        }
        return new IntegerSetDomain(domain);
    }

    static protected IntegerDomain mulRanges(final IntegerDomain a, final IntegerDomain b) {
        final int b00 = a.lb * b.lb;
        final int b01 = a.lb * b.ub;
        final int b10 = a.ub * b.lb;
        final int b11 = a.ub * b.ub;
        final int lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
        final int ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
        return new IntegerRangeDomain(lb0, ub0);
    }

    static protected IntegerDomain divRanges(final IntegerDomain a, final IntegerDomain b) {
        final int b00 = div(a.lb, b.lb);
        final int b01 = div(a.lb, b.ub);
        final int b10 = div(a.ub, b.lb);
        final int b11 = div(a.ub, b.ub);
        int lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
        int ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
        if (b.lb <= 1 && 1 <= b.ub) {
            lb0 = Math.min(lb0, Math.min(a.lb, a.ub));
            ub0 = Math.max(ub0, Math.max(a.lb, a.ub));
        }
        if (b.lb <= -1 && -1 <= b.ub) {
            lb0 = Math.min(lb0, Math.min(-a.lb, -a.ub));
            ub0 = Math.max(ub0, Math.max(-a.lb, -a.ub));
        }
        return new IntegerRangeDomain(lb0, ub0);
    }

    static protected int div(final int x, final int y) {
        return x < 0 && x % y != 0 ? x / y - 1 : x / y;
    }

    protected static class Iter implements Iterator<Integer> {
        int value;
        int ub;

        /**
         * Constructs a new bound iterator.
         * @param lb the lower bound
         * @param ub the upper bound
         */
        public Iter(final int lb, final int ub) {
            this.value = lb;
            this.ub = ub;
        }

        @Override
        public boolean hasNext() {
            return value <= ub;
        }

        @Override
        public Integer next() {
            return value++;
        }

        @Override
        public void remove() {
        }
    }
}
