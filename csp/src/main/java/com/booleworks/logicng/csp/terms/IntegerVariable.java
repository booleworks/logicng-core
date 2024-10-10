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

package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;

/**
 * An integer variable.
 */
public final class IntegerVariable extends Term implements IntegerHolder {
    private final String name;
    private final IntegerDomain domain;
    private final boolean aux;

    /**
     * Generates a new variable in a given domain.
     * @param name   the variable's name
     * @param domain the variable's domain
     * @param aux    auxiliary tag
     */
    public IntegerVariable(final String name, final IntegerDomain domain, final boolean aux) {
        super(Type.VAR);
        this.name = name;
        this.domain = domain;
        this.aux = aux;
    }

    /**
     * Returns whether this variable is unsatisfiable, e.g. has an empty domain.
     * @return whether this variable is unsatisfiable
     */
    public boolean isUnsatisfiable() {
        return domain.isEmpty();
    }

    /**
     * Returns whether the variable is an auxiliary variable.
     * @return whether the variable is an auxiliary variable
     */
    public boolean isAux() {
        return aux;
    }

    /**
     * Returns the name of the variable.
     * @return the name of the variable
     */
    public String getName() {
        return name;
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        variables.add(this);
    }

    @Override
    public boolean isAtom() {
        return true;
    }

    @Override
    public Decomposition calculateDecomposition(final CspFactory cf) {
        return new Decomposition(new LinearExpression(this), Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet());
    }

    @Override
    public IntegerDomain getDomain() {
        return domain;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof IntegerVariable) {
            return Objects.equals(name, ((IntegerVariable) other).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        final String s = "\"" + name + "\"";
        if (aux) {
            return s + ":[" + domain.lb() + "," + domain.ub() + "]";
        } else {
            return s;
        }
    }

    /**
     * Creates a new uncached auxiliary variable.
     * @param name   the name of the variable
     * @param domain the domain of the variable
     * @return a new uncached auxiliary variable
     */
    public static IntegerVariable auxVar(final String name, final IntegerDomain domain) {
        return new IntegerVariable(name, domain, true);
    }
}

