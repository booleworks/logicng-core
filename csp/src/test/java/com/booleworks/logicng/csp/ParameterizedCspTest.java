package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ParameterizedCspTest {
    public static Collection<CspFactory> cspFactories() {
        final List<CspFactory> factories = new ArrayList<>();
        factories.add(new CspFactory(FormulaFactory.caching()));
        factories.add(new CspFactory(FormulaFactory.nonCaching()));
        return factories;
    }

    public static Collection<Arguments> algorithmsAndFactories() {
        final List<Arguments> args = new ArrayList<>();
        args.add(Arguments.arguments(new CspFactory(FormulaFactory.caching()), CspEncodingContext.order()));
        args.add(Arguments.arguments(new CspFactory(FormulaFactory.nonCaching()), CspEncodingContext.order()));
        args.add(Arguments.arguments(new CspFactory(FormulaFactory.caching()), CspEncodingContext.compactOrder(5)));
        args.add(Arguments.arguments(new CspFactory(FormulaFactory.nonCaching()), CspEncodingContext.compactOrder(5)));
        return args;
    }

}
