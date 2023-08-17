// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.logicng.formulas.cache.TransformationCacheEntry.PLAISTED_GREENBAUM_POS;
import static org.logicng.formulas.cache.TransformationCacheEntry.PLAISTED_GREENBAUM_VARIABLE;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.implementation.cached.CachingFormulaFactory;
import org.logicng.formulas.implementation.noncaching.NonCachingFormulaFactory;
import org.logicng.transformations.StatefulFormulaTransformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transformation of a formula into CNF due to Plaisted &amp; Greenbaum.  Results in this implementation will always be
 * cached.
 * <p>
 * ATTENTION: if you mix formulas from different formula factories this can lead to clashes in the naming of newly
 * introduced variables.
 * @version 3.0.0
 * @since 1.0
 */
public final class PlaistedGreenbaumTransformation extends StatefulFormulaTransformation<PlaistedGreenbaumTransformation.PGState> {

    public static final int DEFAULT_BOUNDARY = 12;

    private final int boundaryForFactorization;
    private final CNFFactorization factorization;

    /**
     * Constructor for a Plaisted &amp; Greenbaum transformation with conversion to nnf and the default
     * factorization  bound of 12.
     * @param f the formula factory to generate new formulas
     */
    public PlaistedGreenbaumTransformation(final CachingFormulaFactory f) {
        this(f, DEFAULT_BOUNDARY);
    }

    /**
     * Constructor for a Plaisted &amp; Greenbaum transformation.
     * @param f                        the formula factory to generate new formulas
     * @param boundaryForFactorization the boundary of number of atoms up to which classical factorization is used
     */
    public PlaistedGreenbaumTransformation(final CachingFormulaFactory f, final int boundaryForFactorization) {
        super(f);
        this.boundaryForFactorization = boundaryForFactorization;
        this.factorization = new CNFFactorization(f);
    }

    /**
     * Constructor for a Plaisted &amp; Greenbaum transformation with conversion to nnf and the default
     * factorization  bound of 12.
     * @param f     the formula factory to generate new formulas
     * @param state the mutable state for a PG transformation
     */
    public PlaistedGreenbaumTransformation(final NonCachingFormulaFactory f, final PGState state) {
        this(f, DEFAULT_BOUNDARY, state);
    }

    /**
     * Constructor for a Plaisted &amp; Greenbaum transformation.
     * @param f                        the formula factory to generate new formulas
     * @param boundaryForFactorization the boundary of number of atoms up to which classical factorization is used
     * @param state                    the mutable state for a PG transformation
     */
    public PlaistedGreenbaumTransformation(final NonCachingFormulaFactory f, final int boundaryForFactorization, final PGState state) {
        super(f, state);
        this.boundaryForFactorization = boundaryForFactorization;
        this.factorization = new CNFFactorization(f);
    }

    @Override
    protected PGState initStateForCachingFactory(final CachingFormulaFactory f) {
        return new PGState(f);
    }

    /**
     * Returns the auxiliary variable for a given formula.  Either the formula is already a variable, has already an
     * auxiliary variable or a new one is generated.
     * @param formula the formula
     * @return the old or new auxiliary variable
     */
    private Literal pgVariable(final Formula formula) {
        if (formula.type() == FType.LITERAL) {
            return (Literal) formula;
        }
        Literal var = state.literal(formula);
        if (var == null) {
            var = f.newCNFVariable();
            state.literalCache.put(formula, var);
        }
        return var;
    }

    @Override
    public Formula apply(final Formula formula) {
        final Formula nnf = formula.nnf(f);
        if (nnf.isCNF()) {
            return nnf;
        }
        Formula pg;
        if (nnf.numberOfAtoms() < boundaryForFactorization) {
            pg = nnf.transform(factorization);
        } else {
            pg = computeTransformation(nnf);
            final Assignment topLevel = new Assignment(state.literal(nnf));
            pg = pg.restrict(topLevel, f);
        }
        state.literalCache.put(formula, state.literal(nnf));
        return pg;
    }

    private Formula computeTransformation(final Formula formula) {
        switch (formula.type()) {
            case LITERAL:
                return f.verum();
            case OR:
            case AND:
                final List<Formula> nops = new ArrayList<>();
                nops.add(computePosPolarity(formula));
                for (final Formula op : formula) {
                    nops.add(computeTransformation(op));
                }
                return f.and(nops);
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.type());
        }
    }

    private Formula computePosPolarity(final Formula formula) {
        Formula result = state.pos(formula);
        if (result != null) {
            return result;
        }
        final Literal pgVar = pgVariable(formula);
        switch (formula.type()) {
            case AND: {
                final List<Formula> nops = new ArrayList<>();
                for (final Formula op : formula) {
                    nops.add(f.clause(pgVar.negate(f), pgVariable(op)));
                }
                result = f.and(nops);
                state.posCache.put(formula, result);
                return result;
            }
            case OR: {
                final List<Literal> nops = new ArrayList<>();
                nops.add(pgVar.negate(f));
                for (final Formula op : formula) {
                    nops.add(pgVariable(op));
                }
                result = f.clause(nops);
                state.posCache.put(formula, result);
                return result;
            }
            default:
                throw new IllegalArgumentException("Unknown or unexpected formula type. Expected AND or OR formula type only.");
        }
    }

    @Override
    public String toString() {
        return String.format("PlaistedGreenbaumTransformation{boundary=%d}", boundaryForFactorization);
    }

    public static final class PGState {
        private final Map<Formula, Formula> posCache;
        private final Map<Formula, Formula> literalCache;

        public PGState() {
            this.posCache = new HashMap<>();
            this.literalCache = new HashMap<>();
        }

        public PGState(final CachingFormulaFactory f) {
            this.posCache = f.getTransformationCacheForType(PLAISTED_GREENBAUM_POS);
            this.literalCache = f.getTransformationCacheForType(PLAISTED_GREENBAUM_VARIABLE);
        }

        public PGState(final Map<Formula, Formula> posCache, final Map<Formula, Formula> literalCache) {
            this.posCache = posCache;
            this.literalCache = literalCache;
        }

        private Formula pos(final Formula formula) {
            return posCache.get(formula);
        }

        private Literal literal(final Formula formula) {
            return (Literal) literalCache.get(formula);
        }
    }
}
