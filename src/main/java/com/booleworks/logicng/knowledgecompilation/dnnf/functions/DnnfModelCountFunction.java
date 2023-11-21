// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A DNNF function which counts models.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class DnnfModelCountFunction implements DnnfFunction<BigInteger> {

    private final FormulaFactory f;
    private final Map<Formula, BigInteger> cache;

    public DnnfModelCountFunction(final FormulaFactory f) {
        this.f = f;
        cache = f instanceof CachingFormulaFactory ? ((CachingFormulaFactory) f).getFunctionCacheForType(FunctionCacheEntry.DNNF_MODELCOUNT) : new HashMap<>();
    }

    @Override
    public BigInteger apply(final SortedSet<Variable> originalVariables, final Formula formula) {
        final BigInteger cached = cache.get(formula);
        final BigInteger result = cached != null ? cached : count(formula);
        final SortedSet<Variable> dontCareVariables = new TreeSet<>();
        final SortedSet<Variable> dnnfVariables = formula.variables(f);
        for (final Variable originalVariable : originalVariables) {
            if (!dnnfVariables.contains(originalVariable)) {
                dontCareVariables.add(originalVariable);
            }
        }
        final BigInteger factor = BigInteger.valueOf(2).pow(dontCareVariables.size());
        return result.multiply(factor);
    }

    private BigInteger count(final Formula dnnf) {
        BigInteger c = cache.get(dnnf);
        if (c == null) {
            switch (dnnf.type()) {
                case LITERAL:
                case TRUE:
                    c = BigInteger.ONE;
                    break;
                case AND:
                    c = BigInteger.ONE;
                    for (final Formula op : dnnf) {
                        c = c.multiply(count(op));
                    }
                    break;
                case OR:
                    final int allVariables = dnnf.variables(f).size();
                    c = BigInteger.ZERO;
                    for (final Formula op : dnnf) {
                        final BigInteger opCount = count(op);
                        final BigInteger factor = BigInteger.valueOf(2L).pow(allVariables - op.variables(f).size());
                        c = c.add(opCount.multiply(factor));
                    }
                    break;
                case FALSE:
                    c = BigInteger.ZERO;
                    break;
            }
            cache.put(dnnf, c);
        }
        return c;
    }
}
