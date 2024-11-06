package com.booleworks.logicng.csp.io.readers;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.io.javacc.ParseException;
import com.booleworks.logicng.csp.io.javacc.SExpressionParser;
import com.booleworks.logicng.csp.io.javacc.TokenMgrError;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class SExpressionConverter {
    private static void convertFile(final SExpressionParser parser, final File src, final File dest)
            throws IOException, ParserException {
        final BufferedReader br = new BufferedReader(new FileReader(src));
        final BufferedWriter wr = new BufferedWriter(new FileWriter(dest));
        while (br.ready()) {
            try {
                final String l = br.readLine();
                if (l.startsWith(";") || l.isBlank()) {
                    continue;
                }
                System.out.println(l);
                parser.ReInit(new StringReader(l));
                final Pair<Pair<Variable, IntegerVariable>, Formula> line = parser.line();
                if (line.getSecond() != null) {
                    wr.write(line.getSecond().toString());
                    wr.write('\n');
                } else if (line.getFirst().getSecond() != null) {
                    wr.write("int " + line.getFirst().getSecond().getName() + " " +
                            line.getFirst().getSecond().getDomain().toString());
                    wr.write('\n');
                }
            } catch (final TokenMgrError e) {
                throw new ParserException("lexer error", e);
            } catch (final ParseException e) {
                throw new ParserException("parser error", e);
            }
        }
        br.close();
        wr.close();
    }

    /**
     * Converts files using the common S-expression format to the LNG format for CSP.
     * <p>
     * This converter is not tested and does not support all features.
     * It is not supposed to be used without a manual validation afterward.
     * @param src  the source file or dictionary
     * @param dest the destination file or dictionary
     * @throws IOException     if IO operations fail at the src or dest
     * @throws ParserException if the parser fails to parse a file
     */
    public static void convertFiles(final File src, final File dest)
            throws IOException, ParserException {
        final SExpressionParser parser = new SExpressionParser(new StringReader(""));
        if (src.isFile()) {
            final FormulaFactory f = FormulaFactory.caching();
            final CspFactory cf = new CspFactory(f);
            parser.setFactory(cf);
            final File dest_file;
            if (dest.isDirectory()) {
                final Path dest_path = Paths.get(dest.getAbsolutePath(), src.getName());
                dest_file = new File(dest_path.toString());
            } else {
                dest_file = dest;
            }
            if (dest_file.createNewFile()) {
                convertFile(parser, src, dest_file);
            } else {
                throw new RuntimeException("File already exists");
            }
        } else if (src.isDirectory()) {
            if (!dest.isDirectory()) {
                throw new RuntimeException("Src is a directory, but Dest is not");
            }
            for (final File file : Objects.requireNonNull(src.listFiles())) {
                if (file.isFile() && !file.getName().startsWith(".")) {
                    final FormulaFactory f = FormulaFactory.caching();
                    final CspFactory cf = new CspFactory(f);
                    parser.setFactory(cf);
                    final Path dest_path = Paths.get(dest.getAbsolutePath(), file.getName());
                    final File dest_file = new File(dest_path.toString());
                    System.out.println(dest_file.toString());
                    if (dest_file.createNewFile()) {
                        convertFile(parser, file, dest_file);
                    } else {
                        throw new RuntimeException("File already exists");
                    }
                }
            }
        } else {
            throw new RuntimeException("Invalid src");
        }

    }
}
