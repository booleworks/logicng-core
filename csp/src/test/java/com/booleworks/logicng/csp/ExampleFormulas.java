package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.io.parsers.ParserException;

import java.io.IOException;

public class ExampleFormulas {
    public static Formula arithmJavaCreamSolver(final CspFactory cf) throws ParserException, IOException {
        return CspReader.readCsp(cf, "../test_files/csp/azucar/arithm-java_cream_solver.csp");
    }

    public static Formula arithmSatCoeAzucar(final CspFactory cf) throws ParserException, IOException {
        return CspReader.readCsp(cf, "../test_files/csp/azucar/arithm-sat_coe_azucar.csp");
    }
}
