// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static com.booleworks.logicng.formulas.CType.EQ;
import static com.booleworks.logicng.formulas.CType.GE;
import static com.booleworks.logicng.formulas.CType.GT;
import static com.booleworks.logicng.formulas.CType.LE;
import static com.booleworks.logicng.formulas.CType.LT;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.functions.SubNodeFunction;
import com.booleworks.logicng.serialization.ProtoBufFormulas.PbComparison;
import com.booleworks.logicng.serialization.ProtoBufFormulas.PbFormulaMapping;
import com.booleworks.logicng.serialization.ProtoBufFormulas.PbFormulaType;
import com.booleworks.logicng.serialization.ProtoBufFormulas.PbFormulas;
import com.booleworks.logicng.serialization.ProtoBufFormulas.PbInternalFormula;
import com.booleworks.logicng.serialization.ProtoBufFormulas.PbInternalPseudoBooleanConstraint;
import com.booleworks.logicng.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serialization methods for LogicNG formulas.
 * @version 3.0.0
 * @since 2.5.0
 */
public interface Formulas {

    String NOT_SYMBOL = "~";

    /**
     * Serialize a formula to a file.
     * @param formula  the formula
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @throws IOException if there is a problem writing the file
     */
    static void serializeFormulaToFile(final Formula formula, final Path path, final boolean compress) throws IOException {
        try (final OutputStream outputStream = compress ? new GZIPOutputStream(Files.newOutputStream(path)) : Files.newOutputStream(path)) {
            serializeFormulaToStream(formula, outputStream);
        }
    }

    /**
     * Deserialize a formula from a file.
     * @param f        the formula factory to generate the formula
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @return the formula
     * @throws IOException if there is a problem reading the file
     */
    static Formula deserializeFormulaFromFile(final FormulaFactory f, final Path path, final boolean compress) throws IOException {
        try (final InputStream inputStream = compress ? new GZIPInputStream(Files.newInputStream(path)) : Files.newInputStream(path)) {
            return deserializeFormulaFromStream(f, inputStream);
        }
    }

    /**
     * Serialize a list of formulas to a file.
     * @param formulas the formulas
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @throws IOException if there is a problem writing the file
     */
    static void serializeFormulaListToFile(final List<Formula> formulas, final Path path, final boolean compress) throws IOException {
        try (final OutputStream outputStream = compress ? new GZIPOutputStream(Files.newOutputStream(path)) : Files.newOutputStream(path)) {
            serializeFormulaListToStream(formulas, outputStream);
        }
    }

    /**
     * Deserialize a list of formulas from a file.
     * @param f        the formula factory to generate the formulas
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @return the list of formulas
     * @throws IOException if there is a problem reading the file
     */
    static List<Formula> deserializeFormulaListFromFile(final FormulaFactory f, final Path path, final boolean compress) throws IOException {
        try (final InputStream inputStream = compress ? new GZIPInputStream(Files.newInputStream(path)) : Files.newInputStream(path)) {
            return deserializeFormulaListFromStream(f, inputStream);
        }
    }

    /**
     * Serialize a formula to a stream.
     * @param formula the formula
     * @param stream  the stream
     * @throws IOException if there is a problem writing to the stream
     */
    static void serializeFormulaToStream(final Formula formula, final OutputStream stream) throws IOException {
        serializeFormula(formula).writeTo(stream);
    }

    /**
     * Deserialize a formula from a stream.
     * @param f      the formula factory to generate the formula
     * @param stream the stream
     * @return the formula
     * @throws IOException if there is a problem reading from the stream
     */
    static Formula deserializeFormulaFromStream(final FormulaFactory f, final InputStream stream) throws IOException {
        return deserializeFormula(f, PbFormulas.newBuilder().mergeFrom(stream).build());
    }

    /**
     * Serialize a list of formulas to a stream.
     * @param formulas the formulas
     * @param stream   the stream
     * @throws IOException if there is a problem writing to the stream
     */
    static void serializeFormulaListToStream(final Collection<Formula> formulas, final OutputStream stream) throws IOException {
        serializeFormulas(formulas).writeTo(stream);
    }

    /**
     * Deserialize a list of formulas from a stream.
     * @param f      the formula factory to generate the formulas
     * @param stream the stream
     * @return the list of formulas
     * @throws IOException if there is a problem reading from the stream
     */
    static List<Formula> deserializeFormulaListFromStream(final FormulaFactory f, final InputStream stream) throws IOException {
        return deserializeFormulaList(f, PbFormulas.newBuilder().mergeFrom(stream).build());
    }

    /**
     * Serializes a formula to a protocol buffer.
     * @param formula the formula
     * @return the protocol buffer
     */
    static PbFormulas serializeFormula(final Formula formula) {
        return serializeFormulas(Collections.singletonList(formula));
    }

    /**
     * Serializes a list of formulas to a protocol buffer.
     * @param formulas the formulas
     * @return the protocol buffer
     */
    static PbFormulas serializeFormulas(final Collection<Formula> formulas) {
        final Pair<Map<Formula, Integer>, Map<Integer, PbInternalFormula>> maps;
        if (formulas.isEmpty()) {
            maps = new Pair<>(Map.of(), Map.of());
        } else {
            maps = computeMappings(formulas.iterator().next().getFactory(), formulas);
        }
        final List<Integer> ids = formulas.stream().map(maps.getFirst()::get).collect(Collectors.toList());
        return PbFormulas.newBuilder()
                .addAllId(ids)
                .setMapping(PbFormulaMapping.newBuilder().putAllMapping(maps.getSecond()).build())
                .build();
    }

    /**
     * Computes the serialization mappings for a given list of formulas.
     * @param formulas the formulas
     * @return a mapping from formula to ID and a mapping from ID to serialized formula (protocol buffer)
     * for each sub-node of the formulas.
     */
    static Pair<Map<Formula, Integer>, Map<Integer, PbInternalFormula>> computeMappings(
            final FormulaFactory f,
            final Collection<Formula> formulas
    ) {
        final Map<Formula, Integer> formula2id = new LinkedHashMap<>();
        final Map<Integer, PbInternalFormula> id2formula = new LinkedHashMap<>();
        int id = 0;
        final SubNodeFunction subNodeFunction = new SubNodeFunction(f);
        for (final Formula formula : formulas) {
            for (final Formula subnode : formula.apply(subNodeFunction)) {
                if (!formula2id.containsKey(subnode)) {
                    formula2id.put(subnode, id);
                    id2formula.put(id, serialize(subnode, formula2id));
                    id++;
                }
            }
        }
        return new Pair<>(formula2id, id2formula);
    }

    /**
     * Serializes a formula to a protocol buffer with a given formula to ID mapping.
     * @param formula    the formula
     * @param formula2id a mapping from formula to ID (must contain all sub-nodes of the formula)
     * @return the protocol buffer.
     */
    static PbInternalFormula serialize(final Formula formula, final Map<Formula, Integer> formula2id) {
        final PbInternalFormula.Builder builder = PbInternalFormula.newBuilder();
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
                builder.setType(PbFormulaType.CONST);
                builder.setValue(formula.getType() == FType.TRUE);
                break;
            case LITERAL:
                builder.setType(PbFormulaType.LITERAL);
                final Literal lit = (Literal) formula;
                builder.setValue(lit.getPhase());
                builder.setVariable(lit.getName());
                break;
            case NOT:
                builder.setType(PbFormulaType.NOT);
                final Not not = (Not) formula;
                builder.addOperand(formula2id.get(not.getOperand()));
                break;
            case EQUIV:
                builder.setType(PbFormulaType.EQUIV);
                final Equivalence eq = (Equivalence) formula;
                builder.addOperand(formula2id.get(eq.getLeft()));
                builder.addOperand(formula2id.get(eq.getRight()));
                break;
            case IMPL:
                builder.setType(PbFormulaType.IMPL);
                final Implication impl = (Implication) formula;
                builder.addOperand(formula2id.get(impl.getLeft()));
                builder.addOperand(formula2id.get(impl.getRight()));
                break;
            case OR:
                builder.setType(PbFormulaType.OR);
                final Or or = (Or) formula;
                for (final Formula op : or) {
                    builder.addOperand(formula2id.get(op));
                }
                break;
            case AND:
                builder.setType(PbFormulaType.AND);
                final And and = (And) formula;
                for (final Formula op : and) {
                    builder.addOperand(formula2id.get(op));
                }
                break;
            case PBC:
                builder.setType(PbFormulaType.PBC);
                final PbConstraint Pbc = (PbConstraint) formula;
                final PbInternalPseudoBooleanConstraint.Builder PbBuilder = PbInternalPseudoBooleanConstraint.newBuilder();
                PbBuilder.setRhs(Pbc.getRhs());
                PbBuilder.setComparator(serializeCType(Pbc.comparator()));
                Pbc.getCoefficients().forEach(PbBuilder::addCoefficient);
                Pbc.getOperands().forEach(it -> PbBuilder.addLiteral(it.toString()));
                builder.setPbConstraint(PbBuilder.build());
                break;
        }
        return builder.build();
    }

    /**
     * Deserializes a formula from a protocol buffer.
     * @param f   the formula factory to generate the formula
     * @param bin the protocol buffer
     * @return the formula
     */
    static Formula deserializeFormula(final FormulaFactory f, final PbFormulas bin) {
        return deserializeFormulaList(f, bin).get(0);
    }

    /**
     * Deserializes a list of formulas from a protocol buffer.
     * @param f   the formula factory to generate the formulas
     * @param bin the protocol buffer
     * @return the list of formulas
     */
    static List<Formula> deserializeFormulaList(final FormulaFactory f, final PbFormulas bin) {
        final Map<Integer, Formula> id2formula = deserializeFormula(f, bin.getMapping());
        return bin.getIdList().stream().map(id2formula::get).collect(Collectors.toList());
    }

    /**
     * Deserializes a mapping from integer to formulas from a protocol buffer.
     * @param f   the formula factory to generate the formulas
     * @param bin the protocol buffer
     * @return the mapping from ID to formula
     */
    static Map<Integer, Formula> deserializeFormula(final FormulaFactory f, final PbFormulaMapping bin) {
        final Map<Integer, Formula> id2formula = new TreeMap<>();
        bin.getMappingMap().forEach((k, v) -> {
            id2formula.put(k, deserialize(f, v, id2formula));
        });
        return id2formula;
    }

    /**
     * Deserializes a protocol buffer to a formula with a given ID to formula mapping.
     * @param f          the formula factory to generate the formulas
     * @param bin        the protocol buffer
     * @param id2formula a mapping from ID to formula (must contain all sub-nodes of the formula)
     * @return the formula
     */
    static Formula deserialize(final FormulaFactory f, final PbInternalFormula bin, final Map<Integer, Formula> id2formula) {
        switch (bin.getType()) {
            case CONST:
                return f.constant(bin.getValue());
            case LITERAL:
                return f.literal(bin.getVariable(), bin.getValue());
            case NOT:
                return f.not(id2formula.get(bin.getOperand(0)));
            case IMPL:
            case EQUIV:
                final FType binType = bin.getType() == PbFormulaType.IMPL ? FType.IMPL : FType.EQUIV;
                return f.binaryOperator(binType, id2formula.get(bin.getOperand(0)), id2formula.get(bin.getOperand(1)));
            case AND:
            case OR:
                final FType naryType = bin.getType() == PbFormulaType.AND ? FType.AND : FType.OR;
                return f.naryOperator(naryType, bin.getOperandList().stream().map(id2formula::get).collect(Collectors.toList()));
            case PBC:
                final int rhs = (int) bin.getPbConstraint().getRhs();
                final CType ctype = deserializeCType(bin.getPbConstraint().getComparator());
                final List<Literal> lits = bin.getPbConstraint().getLiteralList().stream()
                        .map(it -> it.startsWith(NOT_SYMBOL) ? f.literal(it.substring(1), false) : f.literal(it, true))
                        .collect(Collectors.toList());
                final List<Integer> coeffs = new ArrayList<>();
                for (final long l : bin.getPbConstraint().getCoefficientList()) {
                    coeffs.add((int) l);
                }
                return f.pbc(ctype, rhs, lits, coeffs);
            case PREDICATE:
                return null;
            default:
                throw new IllegalArgumentException("Cannot deserialize type " + bin.getType());
        }
    }

    /**
     * Serializes a pseudo-Boolean comparator.
     * @param comparison the comparator
     * @return the protocol buffer
     */
    static PbComparison serializeCType(final CType comparison) {
        switch (comparison) {
            case EQ:
                return PbComparison.EQ;
            case GT:
                return PbComparison.GT;
            case GE:
                return PbComparison.GE;
            case LT:
                return PbComparison.LT;
            case LE:
                return PbComparison.LE;
            default:
                throw new IllegalArgumentException("Unknown comparison type" + comparison);
        }
    }

    /**
     * Deserializes a pseudo-Boolean comparator.
     * @param bin the protocol buffer
     * @return the comparator
     */
    static CType deserializeCType(final PbComparison bin) {
        switch (bin) {
            case EQ:
                return EQ;
            case GT:
                return GT;
            case GE:
                return GE;
            case LT:
                return LT;
            case LE:
                return LE;
            default:
                throw new IllegalArgumentException("Unknown comparison type" + bin);
        }
    }
}
