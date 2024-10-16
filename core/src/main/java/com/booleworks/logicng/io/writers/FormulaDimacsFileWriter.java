// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.writers;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.functions.LiteralsFunction;
import com.booleworks.logicng.functions.VariablesFunction;
import com.booleworks.logicng.predicates.CnfPredicate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A dimacs file writer for a formula. Writes the internal data structure of the
 * formula to a dimacs file.
 * @version 2.4.0
 * @since 1.2
 */
public final class FormulaDimacsFileWriter {

    private static final String CNF_EXTENSION = ".cnf";
    private static final String MAP_EXTENSION = ".map";

    /**
     * Private constructor.
     */
    private FormulaDimacsFileWriter() {
        // Intentionally left empty.
    }

    /**
     * Writes a given formula's internal data structure as a dimacs file. Must
     * only be called with a formula which is in CNF.
     * @param fileName     the file name of the dimacs file to write, will be
     *                     extended by suffix {@code .cnf} if not already
     *                     present
     * @param formula      the formula in CNF
     * @param writeMapping indicates whether an additional file for translating
     *                     the ids to variable names shall be written
     * @throws IOException              if there was a problem writing the file
     * @throws IllegalArgumentException if the formula was not in CNF
     */
    public static void write(final String fileName, final Formula formula, final boolean writeMapping)
            throws IOException {
        final LiteralsFunction lf = new LiteralsFunction(formula.getFactory(), null);
        final VariablesFunction vf = new VariablesFunction(formula.getFactory(), null);
        final File file = new File(fileName.endsWith(CNF_EXTENSION) ? fileName : fileName + CNF_EXTENSION);
        final SortedMap<Variable, Long> var2id = new TreeMap<>();
        long i = 1;
        for (final Variable var : new TreeSet<>(formula.apply(vf))) {
            var2id.put(var, i++);
        }
        if (!formula.holds(new CnfPredicate(formula.getFactory(), null))) {
            throw new IllegalArgumentException("Cannot write a non-CNF formula to dimacs.  Convert to CNF first.");
        }
        final List<Formula> parts = new ArrayList<>();
        if (formula.getType() == FType.LITERAL || formula.getType() == FType.OR) {
            parts.add(formula);
        } else {
            for (final Formula part : formula) {
                parts.add(part);
            }
        }
        final StringBuilder sb = new StringBuilder("p cnf ");
        final int partsSize = formula.getType() == FType.FALSE ? 1 : parts.size();
        sb.append(var2id.size()).append(" ").append(partsSize).append(System.lineSeparator());

        for (final Formula part : parts) {
            for (final Literal lit : part.apply(lf)) {
                sb.append(lit.getPhase() ? "" : "-").append(var2id.get(lit.variable())).append(" ");
            }
            sb.append(String.format(" 0%n"));
        }
        if (formula.getType().equals(FType.FALSE)) {
            sb.append(String.format("0%n"));
        }
        try (final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            writer.append(sb);
            writer.flush();
        }
        if (writeMapping) {
            final String mappingFileName =
                    (fileName.endsWith(CNF_EXTENSION) ? fileName.substring(0, fileName.length() - 4) : fileName) +
                            MAP_EXTENSION;
            writeMapping(new File(mappingFileName), var2id);
        }
    }

    private static void writeMapping(final File mappingFile, final SortedMap<Variable, Long> var2id)
            throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<Variable, Long> entry : var2id.entrySet()) {
            sb.append(entry.getKey()).append(";").append(entry.getValue()).append(System.lineSeparator());
        }
        try (final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(mappingFile.toPath()), StandardCharsets.UTF_8))) {
            writer.append(sb);
            writer.flush();
        }
    }
}
