package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.io.parsers.CspParser;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ParserBenchmark {
    @Test
    public void parseExampleFiles() throws ParserException, IOException {
        System.out.println("File, milli seconds, micro seconds");
        for (final File file : Objects.requireNonNull(new File("../test_files/csp/azucar").listFiles())) {
            final FormulaFactory f = FormulaFactory.caching();
            final CspFactory cf = new CspFactory(f);
            final CspParser p = new CspParser(cf);
            final long startParse = System.nanoTime();
            CspReader.readCsp(p, file);
            final long endParse = System.nanoTime();

            System.out.println(file.getName() + ", " + (endParse - startParse) / 1_000_000 + ", " +
                    ((endParse - startParse) / 1000));
        }
    }
}
