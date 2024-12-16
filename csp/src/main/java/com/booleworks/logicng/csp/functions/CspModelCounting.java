package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderEncodingContext;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.modelcounting.ModelCounter;

import java.math.BigInteger;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class grouping functions for counting models of CSP problems.
 */
public class CspModelCounting {

    public static LngResult<BigInteger> count(final Csp csp, final CspFactory cf, final ComputationHandler handler) {
        final OrderEncodingContext context = CspEncodingContext.order_model_count();

        final List<Formula> encoded = cf.encodeCsp(csp, context);

        final SortedSet<Variable> allVars = new TreeSet<>(context.getSatVariables(csp.getInternalIntegerVariables()));
        allVars.addAll(context.getSatVariables(context.getSimplifyIntVariables()));
        allVars.addAll(csp.getInternalBooleanVariables());
        allVars.addAll(context.getSimplifyBoolVariables());
        return ModelCounter.count(cf.getFormulaFactory(), encoded, allVars, handler);
    }
}

