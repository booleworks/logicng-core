package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.functions.CspPropagation;
import com.booleworks.logicng.csp.io.parsers.CspParser;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class EncodingBenchmark {
    static final List<String> SKIP = List.of("arithm-sat_coe_azucar.csp", "nqueens-200.csp");

    @Test
    public void encodingPerformance() throws ParserException, IOException {
        System.out.println("File, Time Enc, Vars, Clauses");
        for (final File file : Objects.requireNonNull(new File("../test_files/csp/azucar").listFiles())) {
            if (SKIP.contains(file.getName())) {
                continue;
            }
            final var order = encode(file, CspEncodingContext.order());
            final var coe2 = encode(file, CspEncodingContext.compactOrder(2));
            final var coe5 = encode(file, CspEncodingContext.compactOrder(5));
            final var coe10 = encode(file, CspEncodingContext.compactOrder(10));
            final var coe50 = encode(file, CspEncodingContext.compactOrder(50));
            final var coe100 = encode(file, CspEncodingContext.compactOrder(100));
            System.out.println(
                    "(OE) " + file.getName() + ": " + order.timeEnc + ", " + order.vars + ", " + order.clauses);
            System.out.println(
                    "(COE2) " + file.getName() + ": " + coe2.timeEnc + ", " + coe2.vars + ", " + coe2.clauses);
            System.out.println(
                    "(COE5) " + file.getName() + ": " + coe5.timeEnc + ", " + coe5.vars + ", " + coe5.clauses);
            System.out.println(
                    "(COE10) " + file.getName() + ": " + coe10.timeEnc + ", " + coe10.vars + ", " + coe10.clauses);
            System.out.println(
                    "(COE50) " + file.getName() + ": " + coe50.timeEnc + ", " + coe50.vars + ", " + coe50.clauses);
            System.out.println(
                    "(COE100) " + file.getName() + ": " + coe100.timeEnc + ", " + coe100.vars + ", " + coe100.clauses);
            System.out.println();
        }
    }

    @Test
    public void propagatedEncodingPerformance() throws ParserException, IOException {
        System.out.println("\t Time Prop, Time Enc, Solve, Vars, Clauses");
        for (final File file : Objects.requireNonNull(new File("../test_files/csp/azucar").listFiles())) {
            if (SKIP.contains(file.getName())) {
                continue;
            }
            final var order = propagateAndEncode(file, CspEncodingContext.order());
            final var coe2 = propagateAndEncode(file, CspEncodingContext.compactOrder(2));
            final var coe5 = propagateAndEncode(file, CspEncodingContext.compactOrder(5));
            final var coe10 = propagateAndEncode(file, CspEncodingContext.compactOrder(10));
            final var coe50 = propagateAndEncode(file, CspEncodingContext.compactOrder(50));
            final var coe100 = propagateAndEncode(file, CspEncodingContext.compactOrder(100));
            System.out.println(
                    "(OE) " + file.getName() + ": " + order.timeProp + ", " + order.timeEnc + ", " + order.vars + ", "
                            + order.clauses);
            System.out.println(
                    "(COE2) " + file.getName() + ": " + coe2.timeProp + ", " + coe2.timeEnc + ", " + coe2.vars + ", "
                            + coe2.clauses);
            System.out.println(
                    "(COE5) " + file.getName() + ": " + coe5.timeProp + ", " + coe5.timeEnc + ", " + coe5.vars + ", "
                            + coe5.clauses);
            System.out.println(
                    "(COE10) " + file.getName() + ": " + coe10.timeProp + ", " + coe10.timeEnc + ", " + coe10.vars
                            + ", " + coe10.clauses);
            System.out.println(
                    "(COE50) " + file.getName() + ": " + coe100.timeProp + ", " + coe50.timeEnc + ", " + coe50.vars
                            + ", " + coe50.clauses);
            System.out.println(
                    "(COE100) " + file.getName() + ": " + order.timeProp + ", " + coe100.timeEnc + ", " + coe100.vars
                            + ", " + coe100.clauses);
            System.out.println();
        }
    }

    @Test
    public void solvingPerformance() throws ParserException, IOException {
        System.out.println("File, Time Enc, Vars, Clauses");
        for (final File file : Objects.requireNonNull(new File("../test_files/csp/azucar").listFiles())) {
            if (SKIP.contains(file.getName())) {
                continue;
            }
            final var order = encodeAndSolve(file, CspEncodingContext.order());
            final var coe2 = encodeAndSolve(file, CspEncodingContext.compactOrder(2));
            final var coe5 = encodeAndSolve(file, CspEncodingContext.compactOrder(5));
            final var coe10 = encodeAndSolve(file, CspEncodingContext.compactOrder(10));
            final var coe50 = encodeAndSolve(file, CspEncodingContext.compactOrder(50));
            final var coe100 = encodeAndSolve(file, CspEncodingContext.compactOrder(100));
            System.out.println("(OE) " + file.getName() + ": " + order);
            System.out.println("(COE2) " + file.getName() + ": " + coe2);
            System.out.println("(COE5) " + file.getName() + ": " + coe5);
            System.out.println("(COE10) " + file.getName() + ": " + coe10);
            System.out.println("(COE50) " + file.getName() + ": " + coe50);
            System.out.println("(COE100) " + file.getName() + ": " + coe100);
            System.out.println();
        }
    }

    private EncodeStats encode(final File file, final CspEncodingContext context)
            throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final CspParser p = new CspParser(cf);
        final Formula formula = CspReader.readCsp(p, file);
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final EncodingResult result =
                EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
        final long startEncoding = System.currentTimeMillis();
        cf.encodeCsp(csp, context, result);
        final long endEncoding = System.currentTimeMillis();

        final int vars = solver.getUnderlyingSolver().nVars();
        final int cls = solver.getUnderlyingSolver().getClauses().size();
        return new EncodeStats(endEncoding - startEncoding, 0, cls, vars);
    }

    private EncodeStats propagateAndEncode(final File file, final CspEncodingContext context)
            throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final CspParser p = new CspParser(cf);
        final Formula formula = CspReader.readCsp(p, file);
        final Csp csp = cf.buildCsp(formula);

        final long startProp = System.currentTimeMillis();
        final Csp propCsp = CspPropagation.propagate(csp, cf);
        final long endProp = System.currentTimeMillis();

        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final EncodingResult result =
                EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
        final long startEncoding = System.currentTimeMillis();
        cf.encodeCsp(propCsp, context, result);
        final long endEncoding = System.currentTimeMillis();

        final int vars = solver.getUnderlyingSolver().nVars();
        final int cls = solver.getUnderlyingSolver().getClauses().size();
        return new EncodeStats(endEncoding - startEncoding, endProp - startProp, cls, vars);
    }

    private long encodeAndSolve(final File file, final CspEncodingContext context) throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final CspParser p = new CspParser(cf);
        final Formula formula = CspReader.readCsp(p, file);
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final EncodingResult result =
                EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);

        final long startSolving = System.currentTimeMillis();
        final LngResult<Boolean> res = solver.satCall().handler(new TimeoutHandler(60000)).sat();
        final long endSolving = System.currentTimeMillis();

        if (res.isSuccess()) {
            return endSolving - startSolving;
        } else {
            return -1;
        }
    }

    private static class EncodeStats {
        long timeEnc;
        long timeProp;
        int clauses;
        int vars;

        public EncodeStats(final long timeEnc, final long timeProp, final int clauses, final int vars) {
            this.timeEnc = timeEnc;
            this.timeProp = timeProp;
            this.clauses = clauses;
            this.vars = vars;
        }
    }
}
