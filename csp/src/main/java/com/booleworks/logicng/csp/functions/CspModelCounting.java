// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderEncodingContext;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.modelcounting.ModelCounter;

import java.math.BigInteger;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Functions for counting models of CSP problems.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspModelCounting {
    protected CspModelCounting() {
    }

    /**
     * Counts the model of a CSP.
     * @param cf  the factory
     * @param csp the csp
     * @return the model count or the cancel cause produced by the handler
     */
    public static BigInteger count(final CspFactory cf, final Csp csp) {
        return count(cf, csp, NopHandler.get()).getResult();
    }

    /**
     * Counts the model of a CSP.
     * @param cf      the factory
     * @param csp     the csp
     * @param handler handler for processing events
     * @return the model count or the cancel cause produced by the handler
     */
    public static LngResult<BigInteger> count(final CspFactory cf, final Csp csp, final ComputationHandler handler) {
        final OrderEncodingContext context = CspEncodingContext.order_model_count();

        final List<Formula> encoded = cf.encodeCsp(csp, context);

        final SortedSet<Variable> allVars =
                new TreeSet<>(context.getEncodingVariables(csp.getInternalIntegerVariables()));
        allVars.addAll(context.getEncodingVariables(context.getSimplifyIntVariables()));
        allVars.addAll(csp.getInternalBooleanVariables());
        allVars.addAll(context.getSimplifyBoolVariables());
        return ModelCounter.count(cf.getFormulaFactory(), encoded, allVars, handler);
    }
}

