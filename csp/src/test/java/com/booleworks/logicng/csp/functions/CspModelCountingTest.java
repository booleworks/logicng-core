package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderEncodingContext;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class CspModelCountingTest {
    @Test
    public void test() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);

        final Formula formula = CspReader.readCsp(cf, "../test_files/csp/simple2.csp");
        final Csp csp = cf.buildCsp(formula);

        models(csp, cf);
    }

    private void models(final Csp csp, final CspFactory cf) {
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final EncodingResult result =
                EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        final List<CspAssignment> models = CspModelEnumeration.enumerate(solver, csp, context, cf);
    }
}
