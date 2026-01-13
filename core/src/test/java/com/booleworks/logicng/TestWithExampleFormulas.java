// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Constant;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;

public abstract class TestWithExampleFormulas {
    protected final CachingFormulaFactory f = FormulaFactory.caching();
    protected final CachingFormulaFactory g = FormulaFactory.caching();

    // Constants
    protected final Constant TRUE = f.verum();
    protected final Constant FALSE = f.falsum();

    // Literals
    protected final Variable A = f.variable("a");
    protected final Variable B = f.variable("b");
    protected final Variable C = f.variable("c");
    protected final Variable D = f.variable("d");
    protected final Variable X = f.variable("x");
    protected final Variable Y = f.variable("y");
    protected final Literal NA = f.literal("a", false);
    protected final Literal NB = f.literal("b", false);
    protected final Literal NX = f.literal("x", false);
    protected final Literal NY = f.literal("y", false);

    // Disjunctions
    protected final Formula OR1 = f.or(X, Y);
    protected final Formula OR2 = f.or(NX, NY);
    protected final Formula OR3 = f.or(f.and(A, B), f.and(NA, NB));

    // Conjunctions
    protected final Formula AND1 = f.and(A, B);
    protected final Formula AND2 = f.and(NA, NB);
    protected final Formula AND3 = f.and(OR1, OR2);

    // Negations
    protected final Formula NOT1 = f.not(AND1);
    protected final Formula NOT2 = f.not(OR1);

    // Implications
    protected final Formula IMP1 = f.implication(A, B);
    protected final Formula IMP2 = f.implication(NA, NB);
    protected final Formula IMP3 = f.implication(AND1, OR1);
    protected final Formula IMP4 = f.implication(f.equivalence(A, B), f.equivalence(NX, NY));

    // Equivalences
    protected final Formula EQ1 = f.equivalence(A, B);
    protected final Formula EQ2 = f.equivalence(NA, NB);
    protected final Formula EQ3 = f.equivalence(AND1, OR1);
    protected final Formula EQ4 = f.equivalence(IMP1, IMP2);

    // PBCs
    private final Literal[] literals = new Literal[]{A, B, X};
    private final int[] coefficients = new int[]{2, -4, 3};
    protected final Formula PBC1 = f.pbc(CType.EQ, 2, literals, coefficients);
    protected final Formula PBC2 = f.pbc(CType.GT, 2, literals, coefficients);
    protected final Formula PBC3 = f.pbc(CType.GE, 2, literals, coefficients);
    protected final Formula PBC4 = f.pbc(CType.LT, 2, literals, coefficients);
    protected final Formula PBC5 = f.pbc(CType.LE, 2, literals, coefficients);

    public static Formula parse(final FormulaFactory f, final String formula) {
        try {
            return new PropositionalParser(f).parse(formula);
        } catch (final ParserException e) {
            throw new RuntimeException(e);
        }
    }
}
