package com.booleworks.logicng.csp.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CspValueHookTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("algorithms")
    public void testHooks1(final CspEncodingContext context) throws ParserException, IOException {
        testHooks(context, "../test_files/csp/simple1.csp");
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void testHooks2(final CspEncodingContext context) throws ParserException, IOException {
        testHooks(context, "../test_files/csp/simple3.csp");
    }

    private void testHooks(final CspEncodingContext context, final String inputPath)
            throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final Formula formula = CspReader.readCsp(cf, inputPath);
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = EncodingResult.resultForSatSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        final Map<IntegerVariable, Map<Variable, Integer>> hooks =
                CspValueHook.encodeValueHooks(csp, context, result, cf);
        final List<Variable> hookVars =
                hooks.values().stream().flatMap(m -> m.keySet().stream()).collect(Collectors.toList());

        final List<CspAssignment> models =
                CspModelEnumeration.enumerate(solver, csp.getVisibleIntegerVariables(), hookVars, context, cf);
        for (final CspAssignment m : models) {
            for (final var h : hooks.entrySet()) {
                final int real_value = m.getIntegerAssignments().get(h.getKey());
                for (final var v : h.getValue().entrySet()) {
                    if (v.getValue() == real_value) {
                        assertThat(m.positiveBooleans().contains(v.getKey())).isTrue();
                    } else {
                        assertThat(m.negativeBooleans().contains(v.getKey().negate(f))).isTrue();
                    }
                }
            }
        }

    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void testProjection1(final CspEncodingContext context) throws ParserException, IOException {
        testProjection(context, "../test_files/csp/simple1.csp");
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void testProjection2(final CspEncodingContext context) throws ParserException, IOException {
        testProjection(context, "../test_files/csp/simple3.csp");
    }

    private void testProjection(final CspEncodingContext context, final String inputPath)
            throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final Formula formula = CspReader.readCsp(cf, inputPath);
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = EncodingResult.resultForSatSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);

        final List<IntegerVariable> relevantVars =
                csp.getVisibleIntegerVariables().stream().filter(csp.getInternalIntegerVariables()::contains)
                        .collect(Collectors.toList());
        final Set<Variable> relevantSatVars = context.getSatVariables(relevantVars);
        final Map<IntegerVariable, Set<Integer>> allowedValues = getAllowedValues(csp, cf);

        for (final IntegerVariable v : relevantVars) {
            final IntegerDomain d = v.getDomain();
            final Iterator<Integer> vals = d.iterator();
            while (vals.hasNext()) {
                final int value = vals.next();
                final Model model =
                        solver.satCall().addFormulas(CspValueHook.calculateValueProjection(v, value, context, cf))
                                .model(relevantSatVars);
                if (allowedValues.containsKey(v) && allowedValues.get(v).contains(value)) {
                    final CspAssignment intModel = cf.decode(model.toAssignment(), relevantVars, context);
                    assertThat(intModel.getIntegerAssignments().get(v)).isEqualTo(value);
                } else {
                    assertThat(model).isNull();
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void testRestrictedSolving(final CspEncodingContext context)
            throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final Formula formula = CspReader.readCsp(cf, "../test_files/csp/simple3.csp");
        final Csp csp = cf.buildCsp(formula);
        final Map<IntegerVariable, Integer> restr = Map.of(cf.getVariable("c"), 7, cf.getVariable("b"), 5);
        final CspAssignment model = CspSolving.model(csp, restr, context, cf);
        System.out.println(model);
    }

    public Map<IntegerVariable, Set<Integer>> getAllowedValues(final Csp csp, final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final CspEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = EncodingResult.resultForSatSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);

        final List<CspAssignment> models =
                CspModelEnumeration.enumerate(solver, csp.getVisibleIntegerVariables(), Set.of(), context, cf);
        final Map<IntegerVariable, Set<Integer>> allowedValues = new HashMap<>();
        for (final CspAssignment model : models) {
            model.getIntegerAssignments().forEach((key, value) ->
                    allowedValues.compute(key, (k, s) -> {
                        if (s == null) {
                            s = new HashSet<>();
                        }
                        s.add(value);
                        return s;
                    }));
        }
        return allowedValues;
    }
}
