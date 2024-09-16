// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.propositions.ExtendedProposition;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.PropositionBackpack;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SatSolversTest {
    private static final SATSolverConfig STAND_CONFIG = SATSolverConfig.builder()
            .useAtMostClauses(false)
            .build();
    private static final SATSolverConfig CARD_CONFIG = SATSolverConfig.builder()
            .useAtMostClauses(true)
            .build();
    private static final SATSolverConfig STAND_PROOF_CONFIG = SATSolverConfig.builder()
            .proofGeneration(true)
            .useAtMostClauses(false)
            .build();
    private static final SATSolverConfig CARD_PROOF_CONFIG = SATSolverConfig.builder()
            .proofGeneration(true)
            .useAtMostClauses(true)
            .build();

    private static FormulaFactory f;
    private static SolverSerializer serializer;
    private static List<Formula> formula;
    private static SortedSet<Variable> variables;
    private static Path tempFile;

    @BeforeAll
    public static void init() throws ParserException, IOException {
        f = FormulaFactory.caching();
        serializer = SolverSerializer.withoutPropositions(f);
        tempFile = Files.createTempFile("temp", "pb");
        final Formula whole = FormulaReader.readFormula(f, Paths.get("../test_files/formulas/largest_formula.txt").toFile());
        variables = whole.variables(f);
        formula = whole.stream().collect(Collectors.toList());
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSatSolverSimple(final boolean compress) throws IOException {
        final var solverBefore = SATSolver.newSolver(f, STAND_CONFIG);
        solverBefore.add(formula);
        serializer.serializeSolverToFile(solverBefore, tempFile, compress);
        final var solverAfter = SolverSerializer.withoutPropositions(FormulaFactory.nonCaching()).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        compareSolverModels(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testCardSolverSimple(final boolean compress) throws IOException {
        final var solverBefore = SATSolver.newSolver(f, CARD_CONFIG);
        solverBefore.add(formula);
        serializer.serializeSolverToFile(solverBefore, tempFile, compress);
        final var solverAfter = SolverSerializer.withoutPropositions(FormulaFactory.nonCaching()).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        compareSolverModels(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSatSolverSolved(final boolean compress) throws IOException {
        final var solverBefore = SATSolver.newSolver(f, STAND_CONFIG);
        solverBefore.add(formula);
        solverBefore.sat();
        serializer.serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.nonCaching();
        final var solverAfter = SolverSerializer.withoutPropositions(ff).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        compareSolverModels(solverBefore, solverAfter);
        solverBefore.add(f.variable("v3025").negate(f));
        solverAfter.add(f.variable("v3025").negate(ff));
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        compareSolverModels(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testCardSolverSolved(final boolean compress) throws IOException {
        final var solverBefore = SATSolver.newSolver(f, CARD_CONFIG);
        solverBefore.add(formula);
        solverBefore.sat();
        serializer.serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.caching();
        final var solverAfter = SolverSerializer.withoutPropositions(ff).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        compareSolverModels(solverBefore, solverAfter);
        solverBefore.add(f.variable("v3025").negate(f));
        solverAfter.add(f.variable("v3025").negate(ff));
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        compareSolverModels(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSatSolverWithProof(final boolean compress) throws IOException, ParserException {
        final var solverBefore = SATSolver.newSolver(f, STAND_PROOF_CONFIG);
        solverBefore.add(formula);
        serializer.serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.caching();
        final var solverAfter = SolverSerializer.withoutPropositions(ff).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        final var p = new PropositionalParser(f);
        final var pp = new PropositionalParser(ff);
        solverBefore.add(p.parse("v1668 & v1671"));
        solverAfter.add(pp.parse("v1668 & v1671"));
        assertThat(solverBefore.sat()).isEqualTo(false);
        assertThat(solverAfter.sat()).isEqualTo(false);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testCardSolverWithProof(final boolean compress) throws IOException, ParserException {
        final var solverBefore = SATSolver.newSolver(f, CARD_PROOF_CONFIG);
        solverBefore.add(formula);
        serializer.serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.nonCaching();
        final var solverAfter = SolverSerializer.withoutPropositions(ff).deserializeSatSolverFromFile(tempFile, compress);
        final PropositionalParser p = new PropositionalParser(f);
        final PropositionalParser pp = new PropositionalParser(ff);
        solverBefore.add(p.parse("v1668 & v1671"));
        solverAfter.add(pp.parse("v1668 & v1671"));
        solverBefore.sat();
        solverAfter.sat();
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        solverBefore.add(p.parse("v1668 & v1671"));
        solverAfter.add(pp.parse("v1668 & v1671"));
        assertThat(solverBefore.sat()).isEqualTo(false);
        assertThat(solverAfter.sat()).isEqualTo(false);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSatSolverWithStandardPropositions(final boolean compress) throws IOException, ParserException {
        final var solverBefore = SATSolver.newSolver(f, STAND_PROOF_CONFIG);
        for (int i = 0; i < formula.size(); i++) {
            solverBefore.add(new StandardProposition("Prop " + i, formula.get(i)));
        }
        SolverSerializer.withStandardPropositions(f).serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.caching();
        final var solverAfter = SolverSerializer.withStandardPropositions(ff).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        final PropositionalParser p = new PropositionalParser(f);
        final PropositionalParser pp = new PropositionalParser(ff);
        solverBefore.add(new StandardProposition("Test", p.parse("v1668 & v1671")));
        solverAfter.add(new StandardProposition("Test", pp.parse("v1668 & v1671")));
        assertThat(solverBefore.sat()).isEqualTo(false);
        assertThat(solverAfter.sat()).isEqualTo(false);
        assertThat(solverBefore.satCall().unsatCore()).isEqualTo(solverAfter.satCall().unsatCore());
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testCardSolverWithStandardPropositions(final boolean compress) throws IOException, ParserException {
        final var solverBefore = SATSolver.newSolver(f, CARD_PROOF_CONFIG);
        for (int i = 0; i < formula.size(); i++) {
            solverBefore.add(new StandardProposition("Prop " + i, formula.get(i)));
        }
        SolverSerializer.withStandardPropositions(f).serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.caching();
        final var solverAfter = SolverSerializer.withStandardPropositions(ff).deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        final PropositionalParser p = new PropositionalParser(f);
        final PropositionalParser pp = new PropositionalParser(ff);
        solverBefore.add(new StandardProposition("Test", p.parse("v1668 & v1671")));
        solverAfter.add(new StandardProposition("Test", pp.parse("v1668 & v1671")));
        assertThat(solverBefore.sat()).isEqualTo(false);
        assertThat(solverAfter.sat()).isEqualTo(false);
        assertThat(solverBefore.satCall().unsatCore()).isEqualTo(solverAfter.satCall().unsatCore());
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSatSolverWithCustomPropositions(final boolean compress) throws IOException, ParserException {
        final var solverBefore = SATSolver.newSolver(f, STAND_PROOF_CONFIG);
        for (int i = 0; i < formula.size(); i++) {
            solverBefore.add(new ExtendedProposition<>(new CustomBackpack(i), formula.get(i)));
        }
        SolverSerializer.withCustomPropositions(f, CustomBackpack.serializer, CustomBackpack.deserializer)
                .serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.caching();
        final var solverAfter = SolverSerializer
                .withCustomPropositions(ff, CustomBackpack.serializer, CustomBackpack.deserializer)
                .deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        final PropositionalParser p = new PropositionalParser(f);
        final PropositionalParser pp = new PropositionalParser(ff);
        solverBefore.add(new ExtendedProposition<>(new CustomBackpack(42), p.parse("v1668 & v1671")));
        solverAfter.add(new ExtendedProposition<>(new CustomBackpack(42), pp.parse("v1668 & v1671")));
        assertThat(solverBefore.sat()).isEqualTo(false);
        assertThat(solverAfter.sat()).isEqualTo(false);
        assertThat(solverBefore.satCall().unsatCore()).isEqualTo(solverAfter.satCall().unsatCore());
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testCardSolverWithCustomPropositions(final boolean compress) throws IOException, ParserException {
        final var solverBefore = SATSolver.newSolver(f, CARD_PROOF_CONFIG);
        for (int i = 0; i < formula.size(); i++) {
            solverBefore.add(new ExtendedProposition<>(new CustomBackpack(i), formula.get(i)));
        }
        SolverSerializer.withCustomPropositions(f, CustomBackpack.serializer, CustomBackpack.deserializer)
                .serializeSolverToFile(solverBefore, tempFile, compress);
        final FormulaFactory ff = FormulaFactory.caching();
        final var solverAfter = SolverSerializer
                .withCustomPropositions(ff, CustomBackpack.serializer, CustomBackpack.deserializer)
                .deserializeSatSolverFromFile(tempFile, compress);
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
        final PropositionalParser p = new PropositionalParser(f);
        final PropositionalParser pp = new PropositionalParser(ff);
        solverBefore.add(new ExtendedProposition<>(new CustomBackpack(42), p.parse("v1668 & v1671")));
        solverAfter.add(new ExtendedProposition<>(new CustomBackpack(42), pp.parse("v1668 & v1671")));
        assertThat(solverBefore.sat()).isEqualTo(false);
        assertThat(solverAfter.sat()).isEqualTo(false);
        assertThat(solverBefore.satCall().unsatCore()).isEqualTo(solverAfter.satCall().unsatCore());
        SolverComparator.compareSolverStates(solverBefore, solverAfter);
    }

    private static void compareSolverModels(final SATSolver solver1, final SATSolver solver2) {
        solver1.sat();
        solver2.sat();
        final var model1 = solver1.satCall().model(variables).positiveVariables();
        final var model2 = solver2.satCall().model(variables).positiveVariables();
        assertThat(model2).isEqualTo(model1);
    }

    private static class CustomBackpack implements PropositionBackpack {
        private final int i;

        private CustomBackpack(final int i) {
            this.i = i;
        }

        static Function<Proposition, byte[]> serializer = proposition -> {
            int integer = ((CustomBackpack) ((ExtendedProposition) proposition).getBackpack()).i;
            byte[] formulaBytes = Formulas.serializeFormula(proposition.getFormula()).toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + formulaBytes.length);
            buffer.putInt(integer);
            buffer.put(formulaBytes);
            return buffer.array();
        };

        static Function<byte[], Proposition> deserializer = byteArray -> {
            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            int integer = buffer.getInt();
            byte[] formulaBytes = new byte[buffer.limit() - 4];
            buffer.get(formulaBytes);
            try {
                return new ExtendedProposition<>(new CustomBackpack(integer),
                        Formulas.deserializeFormula(f, ProtoBufFormulas.PBFormulas.parseFrom(formulaBytes)));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        };

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final CustomBackpack that = (CustomBackpack) o;
            return i == that.i;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(i);
        }

        @Override
        public String toString() {
            return "CustomBackpack{" +
                    "i=" + i +
                    '}';
        }
    }
}
