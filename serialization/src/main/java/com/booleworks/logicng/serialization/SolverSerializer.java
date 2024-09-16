// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static com.booleworks.logicng.serialization.Collections.deserializeIntVec;
import static com.booleworks.logicng.serialization.Collections.serializeBoolVec;
import static com.booleworks.logicng.serialization.Collections.serializeIntVec;
import static com.booleworks.logicng.serialization.ReflectionHelper.getField;
import static com.booleworks.logicng.serialization.ReflectionHelper.setField;
import static com.booleworks.logicng.serialization.SolverDatastructures.deserializeHeap;
import static com.booleworks.logicng.serialization.SolverDatastructures.deserializeIntQueue;
import static com.booleworks.logicng.serialization.SolverDatastructures.deserializeLongQueue;
import static com.booleworks.logicng.serialization.SolverDatastructures.serializeIntQueue;
import static com.booleworks.logicng.serialization.SolverDatastructures.serializeLongQueue;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.serialization.ProtoBufSatSolver.PbSatSolver;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbClause;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbClauseVector;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbProofInformation;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbVariableVector;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbWatcherVector;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbWatcherVectorVector;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngVariable;
import com.booleworks.logicng.solvers.datastructures.LngWatcher;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.solvers.sat.LngCoreSolver.ProofInformation;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A serializer/deserializer for LogicNG SAT solvers.
 * @version 3.0.0
 * @since 2.5.0
 */
public class SolverSerializer {
    private final Function<byte[], Proposition> deserializer;
    private final Function<Proposition, byte[]> serializer;
    private final FormulaFactory f;

    private SolverSerializer(final FormulaFactory f, final Function<Proposition, byte[]> serializer,
                             final Function<byte[], Proposition> deserializer) {
        this.deserializer = deserializer;
        this.serializer = serializer;
        this.f = f;
    }

    /**
     * Generates a new solver serializer for a SAT solver which does not serialize propositions
     * of the proof information.
     * @param f the formula factory
     * @return the solver serializer
     */
    public static SolverSerializer withoutPropositions(final FormulaFactory f) {
        return new SolverSerializer(f, null, null);
    }

    /**
     * Generates a new solver serializer for a SAT solver which does serialize proof information
     * with only standard propositions.
     * @param f the formula factory
     * @return the solver serializer
     */
    public static SolverSerializer withStandardPropositions(final FormulaFactory f) {
        final Function<Proposition, byte[]> serializer = (final Proposition p) -> {
            if (!(p instanceof StandardProposition)) {
                throw new IllegalArgumentException("Can only serialize Standard propositions");
            }
            return Propositions.serializePropositions((StandardProposition) p).toByteArray();
        };
        final Function<byte[], Proposition> deserializer = (final byte[] bs) -> {
            try {
                return Propositions.deserializePropositions(f, ProtoBufPropositions.PbStandardProposition.newBuilder().mergeFrom(bs).build());
            } catch (final InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Can only deserialize Standard propositions");
            }
        };
        return new SolverSerializer(f, serializer, deserializer);
    }

    /**
     * Generates a new solver serializer for a SAT solver which does serialize proof information
     * with custom propositions.  In this case you have to provide your own serializer and deserializer
     * for your propositions.
     * @param f            the formula factory
     * @param serializer   the serializer for the custom propositions
     * @param deserializer the deserializer for the custom propositions
     * @return the solver serializer
     */
    public static SolverSerializer withCustomPropositions(
            final FormulaFactory f,
            final Function<Proposition, byte[]> serializer,
            final Function<byte[], Proposition> deserializer
    ) {
        return new SolverSerializer(f, serializer, deserializer);
    }

    /**
     * Serializes a SAT solver to a file.
     * @param solver   the SAT solver
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @throws IOException if there is a problem writing the file
     */
    public void serializeSolverToFile(final SatSolver solver, final Path path, final boolean compress) throws IOException {
        try (final OutputStream outputStream = compress ? new GZIPOutputStream(Files.newOutputStream(path)) : Files.newOutputStream(path)) {
            serializeSolverToStream(solver, outputStream);
        }
    }

    /**
     * Serializes a SAT solver to a stream.
     * @param solver the SAT solver
     * @param stream the stream
     * @throws IOException if there is a problem writing to the stream
     */
    public void serializeSolverToStream(final SatSolver solver, final OutputStream stream) throws IOException {
        serializeSolver(solver).writeTo(stream);
    }

    /**
     * Serializes a SAT solver to a protocol buffer.
     * @param solver the SAT solver
     * @return the protocol buffer
     */
    public PbSatSolver serializeSolver(final SatSolver solver) {
        return serialize(solver);
    }

    /**
     * Deserializes a Sat solver from a file.
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @return the solver
     * @throws IOException if there is a problem reading the file
     */
    public SatSolver deserializeSatSolverFromFile(final Path path, final boolean compress) throws IOException {
        try (final InputStream inputStream = compress ? new GZIPInputStream(Files.newInputStream(path)) : Files.newInputStream(path)) {
            return deserializeSatSolverFromStream(inputStream);
        }
    }

    /**
     * Deserializes a SAT solver from a stream.
     * @param stream the stream
     * @return the solver
     * @throws IOException if there is a problem reading from the stream
     */
    public SatSolver deserializeSatSolverFromStream(final InputStream stream) throws IOException {
        return deserializeSatSolver(PbSatSolver.newBuilder().mergeFrom(stream).build());
    }

    /**
     * Deserializes a SAT solver from a protocol buffer.
     * @param bin the protocol buffer
     * @return the solver
     */
    public SatSolver deserializeSatSolver(final PbSatSolver bin) {
        return deserialize(bin);
    }

    PbSatSolver serialize(final SatSolver solver) {
        final var core = solver.getUnderlyingSolver();
        final LngVector<LngClause> clauses = getField(core, "clauses");
        final LngVector<LngClause> learnts = getField(core, "learnts");
        final IdentityHashMap<LngClause, Integer> clauseMap = generateClauseMap(clauses, learnts);
        final PbSatSolver.Builder builder = PbSatSolver.newBuilder();

        builder.setConfig(SatSolverConfigs.serializeSatSolverConfig(getField(core, "config")));
        builder.setInSatCall(getField(core, "inSatCall"));
        builder.putAllName2Idx(getField(core, "name2idx"));
        builder.setValidStates(serializeIntVec(getField(core, "validStates")));
        builder.setNextStateId(getField(core, "nextStateId"));
        builder.setOk(getField(core, "ok"));
        builder.setQhead(getField(core, "qhead"));
        builder.setUnitClauses(serializeIntVec(getField(core, "unitClauses")));
        builder.setClauses(serializeClauseVec(clauses, clauseMap));
        builder.setLearnts(serializeClauseVec(learnts, clauseMap));
        builder.setWatches(serializeWatches(getField(core, "watches"), clauseMap));
        builder.setVars(serializeVarVec(getField(core, "vars"), clauseMap));
        builder.setOrderHeap(SolverDatastructures.serializeHeap(getField(core, "orderHeap")));
        builder.setTrail(serializeIntVec(getField(core, "trail")));
        builder.setTrailLim(serializeIntVec(getField(core, "trailLim")));
        builder.setModel(serializeBoolVec(getField(core, "model")));
        builder.setAssumptionConflict(serializeIntVec(getField(core, "assumptionsConflict")));
        builder.setAssumptions(serializeIntVec(getField(core, "assumptions")));
        builder.addAllAssumptionPropositions(serializeProps(getField(core, "assumptionPropositions")));
        builder.setSeen(serializeBoolVec(getField(core, "seen")));
        builder.setAnalyzeBtLevel(getField(core, "analyzeBtLevel"));
        builder.setClaInc(getField(core, "claInc"));
        builder.setVarInc(getField(core, "varInc"));
        builder.setVarDecay(getField(core, "varDecay"));
        builder.setClausesLiterals(getField(core, "clausesLiterals"));
        builder.setLearntsLiterals(getField(core, "learntsLiterals"));

        final LngVector<LngIntVector> pgProof = getField(core, "pgProof");
        if (pgProof != null) {
            builder.setPgProof(Collections.serializeVec(pgProof));
        }
        final LngVector<ProofInformation> pgOriginalClauses = getField(core, "pgOriginalClauses");
        if (pgOriginalClauses != null) {
            for (final ProofInformation oc : pgOriginalClauses) {
                builder.addPgOriginalClauses(serialize(oc));
            }
        }

        builder.setComputingBackbone(getField(core, "computingBackbone"));
        final Stack<Integer> backboneCandidates = getField(core, "backboneCandidates");
        if (backboneCandidates != null) {
            builder.setBackboneCandidates(serializeStack(backboneCandidates));
        }
        final LngIntVector backboneAssumptions = getField(core, "backboneAssumptions");
        if (backboneAssumptions != null) {
            builder.setBackboneAssumptions(serializeIntVec(backboneAssumptions));
        }
        final HashMap<Integer, Tristate> backboneMap = getField(core, "backboneMap");
        if (backboneMap != null) {
            builder.putAllBackboneMap(serializeBbMap(backboneMap));
        }

        builder.setSelectionOrder(serializeIntVec(getField(core, "selectionOrder")));
        builder.setSelectionOrderIdx(getField(core, "selectionOrderIdx"));

        builder.setWatchesBin(serializeWatches(getField(core, "watchesBin"), clauseMap));
        builder.setPermDiff(serializeIntVec(getField(core, "permDiff")));
        builder.setLastDecisionLevel(serializeIntVec(getField(core, "lastDecisionLevel")));
        builder.setLbdQueue(serializeLongQueue(getField(core, "lbdQueue")));
        builder.setTrailQueue(serializeIntQueue(getField(core, "trailQueue")));
        builder.setMyflag(getField(core, "myflag"));
        builder.setAnalyzeLBD(getField(core, "analyzeLbd"));
        builder.setNbClausesBeforeReduce(getField(core, "nbClausesBeforeReduce"));
        builder.setConflicts(getField(core, "conflicts"));
        builder.setConflictsRestarts(getField(core, "conflictsRestarts"));
        builder.setSumLBD(getField(core, "sumLbd"));
        builder.setCurRestart(getField(core, "curRestart"));

        return builder.build();
    }

    SatSolver deserialize(final PbSatSolver bin) {
        final Map<Integer, LngClause> clauseMap = new TreeMap<>();
        final var core = new LngCoreSolver(f, SatSolverConfigs.deserializeSatSolverConfig(bin.getConfig()));
        setField(core, "inSatCall", bin.getInSatCall());
        setField(core, "name2idx", new TreeMap<>(bin.getName2IdxMap()));
        final Map<Integer, String> idx2name = new TreeMap<>();
        bin.getName2IdxMap().forEach((k, v) -> idx2name.put(v, k));
        setField(core, "idx2name", idx2name);
        setField(core, "validStates", deserializeIntVec(bin.getValidStates()));
        setField(core, "nextStateId", bin.getNextStateId());
        setField(core, "ok", bin.getOk());
        setField(core, "qhead", bin.getQhead());
        setField(core, "unitClauses", deserializeIntVec(bin.getUnitClauses()));
        setField(core, "clauses", deserializeClauseVec(bin.getClauses(), clauseMap));
        setField(core, "learnts", deserializeClauseVec(bin.getLearnts(), clauseMap));
        setField(core, "watches", deserializeWatches(bin.getWatches(), clauseMap));
        setField(core, "vars", deserializeVarVec(bin.getVars(), clauseMap));
        setField(core, "orderHeap", deserializeHeap(bin.getOrderHeap(), core));
        setField(core, "trail", deserializeIntVec(bin.getTrail()));
        setField(core, "trailLim", deserializeIntVec(bin.getTrailLim()));
        setField(core, "model", Collections.deserializeBooVec(bin.getModel()));
        setField(core, "assumptionsConflict", deserializeIntVec(bin.getAssumptionConflict()));
        setField(core, "assumptions", deserializeIntVec(bin.getAssumptions()));
        setField(core, "assumptionPropositions", deserializeProps(bin.getAssumptionPropositionsList()));
        setField(core, "seen", Collections.deserializeBooVec(bin.getSeen()));
        setField(core, "analyzeBtLevel", bin.getAnalyzeBtLevel());
        setField(core, "claInc", bin.getClaInc());
        setField(core, "varInc", bin.getVarInc());
        setField(core, "varDecay", bin.getVarDecay());
        setField(core, "clausesLiterals", bin.getClausesLiterals());
        setField(core, "learntsLiterals", bin.getLearntsLiterals());

        if (bin.hasPgProof()) {
            setField(core, "pgProof", Collections.deserializeVec(bin.getPgProof()));
        }
        if (bin.getPgOriginalClausesCount() > 0) {
            final LngVector<ProofInformation> originalClauses = new LngVector<>(bin.getPgOriginalClausesCount());
            for (final PbProofInformation pi : bin.getPgOriginalClausesList()) {
                originalClauses.push(deserialize(pi));
            }
            setField(core, "pgOriginalClauses", originalClauses);
        }

        setField(core, "computingBackbone", bin.getComputingBackbone());
        if (bin.hasBackboneCandidates()) {
            setField(core, "backboneCandidates", deserializeStack(bin.getBackboneCandidates()));
        }
        if (bin.hasBackboneAssumptions()) {
            setField(core, "backboneAssumptions", deserializeIntVec(bin.getBackboneAssumptions()));
        }
        setField(core, "backboneMap", deserializeBbMap(bin.getBackboneMapMap()));

        setField(core, "selectionOrder", deserializeIntVec(bin.getSelectionOrder()));
        setField(core, "selectionOrderIdx", bin.getSelectionOrderIdx());
        setField(core, "watchesBin", deserializeWatches(bin.getWatchesBin(), clauseMap));
        setField(core, "permDiff", deserializeIntVec(bin.getPermDiff()));
        setField(core, "lastDecisionLevel", deserializeIntVec(bin.getLastDecisionLevel()));
        setField(core, "lbdQueue", deserializeLongQueue(bin.getLbdQueue()));
        setField(core, "trailQueue", deserializeIntQueue(bin.getTrailQueue()));
        setField(core, "myflag", bin.getMyflag());
        setField(core, "analyzeLbd", bin.getAnalyzeLBD());
        setField(core, "nbClausesBeforeReduce", bin.getNbClausesBeforeReduce());
        setField(core, "conflicts", bin.getConflicts());
        setField(core, "conflictsRestarts", bin.getConflictsRestarts());
        setField(core, "sumLbd", bin.getSumLBD());
        setField(core, "curRestart", bin.getCurRestart());
        return new SatSolver(f, core);
    }

    private static IdentityHashMap<LngClause, Integer> generateClauseMap(final LngVector<LngClause> clauses, final LngVector<LngClause> learnts) {
        final IdentityHashMap<LngClause, Integer> clauseMap = new IdentityHashMap<>();
        for (final LngClause clause : clauses) {
            clauseMap.put(clause, clauseMap.size());
        }
        for (final LngClause learnt : learnts) {
            clauseMap.put(learnt, clauseMap.size());
        }
        return clauseMap;
    }

    private static PbClauseVector serializeClauseVec(final LngVector<LngClause> vec,
                                                     final IdentityHashMap<LngClause, Integer> clauseMap) {
        final PbClauseVector.Builder builder = PbClauseVector.newBuilder();
        for (final LngClause clause : vec) {
            builder.addElement(SolverDatastructures.serializeClause(clause, clauseMap.get(clause)));
        }
        return builder.build();
    }

    private static LngVector<LngClause> deserializeClauseVec(final PbClauseVector bin, final Map<Integer, LngClause> clauseMap) {
        final LngVector<LngClause> vec = new LngVector<>(bin.getElementCount());
        for (int i = 0; i < bin.getElementCount(); i++) {
            final PbClause binClause = bin.getElement(i);
            final LngClause clause = SolverDatastructures.deserializeClause(binClause);
            clauseMap.put(binClause.getId(), clause);
            vec.push(clause);
        }
        return vec;
    }

    private static PbWatcherVectorVector serializeWatches(final LngVector<LngVector<LngWatcher>> vec,
                                                          final IdentityHashMap<LngClause, Integer> clauseMap) {
        final PbWatcherVectorVector.Builder builder = PbWatcherVectorVector.newBuilder();
        for (final LngVector<LngWatcher> watchList : vec) {
            final PbWatcherVector.Builder watchBuilder = PbWatcherVector.newBuilder();
            for (final LngWatcher watch : watchList) {
                watchBuilder.addElement(SolverDatastructures.serializeWatcher(watch, clauseMap));
            }
            builder.addElement(watchBuilder.build());
        }
        return builder.build();
    }

    private static LngVector<LngVector<LngWatcher>> deserializeWatches(final PbWatcherVectorVector bin,
                                                                       final Map<Integer, LngClause> clauseMap) {
        final LngVector<LngVector<LngWatcher>> vec = new LngVector<>(bin.getElementCount());
        for (int i = 0; i < bin.getElementCount(); i++) {
            final PbWatcherVector binWatch = bin.getElement(i);
            final LngVector<LngWatcher> watch = new LngVector<>(binWatch.getElementCount());
            for (int j = 0; j < binWatch.getElementCount(); j++) {
                watch.push(SolverDatastructures.deserializeWatcher(binWatch.getElement(j), clauseMap));
            }
            vec.push(watch);
        }
        return vec;
    }

    private static PbVariableVector serializeVarVec(final LngVector<LngVariable> vec,
                                                    final IdentityHashMap<LngClause, Integer> clauseMap) {
        final PbVariableVector.Builder builder = PbVariableVector.newBuilder();
        for (final LngVariable var : vec) {
            builder.addElement(SolverDatastructures.serializeVariable(var, clauseMap));
        }
        return builder.build();
    }

    private static LngVector<LngVariable> deserializeVarVec(final PbVariableVector bin, final Map<Integer, LngClause> clauseMap) {
        final LngVector<LngVariable> vec = new LngVector<>(bin.getElementCount());
        for (int i = 0; i < bin.getElementCount(); i++) {
            vec.push(SolverDatastructures.deserializeVariable(bin.getElement(i), clauseMap));
        }
        return vec;
    }

    public static ProtoBufCollections.PbIntVector serializeStack(final Stack<Integer> stack) {
        if (stack == null) {
            return null;
        }
        final ProtoBufCollections.PbIntVector.Builder vec = ProtoBufCollections.PbIntVector.newBuilder();
        for (final Integer integer : stack) {
            vec.addElement(integer);
        }
        vec.setSize(stack.size());
        return vec.build();
    }

    public static Stack<Integer> deserializeStack(final ProtoBufCollections.PbIntVector vec) {
        final Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < vec.getSize(); i++) {
            stack.push(vec.getElement(i));
        }
        return stack;
    }

    private static HashMap<Integer, ProtoBufSolverDatastructures.PbTristate> serializeBbMap(final Map<Integer, Tristate> map) {
        final HashMap<Integer, ProtoBufSolverDatastructures.PbTristate> ser = new HashMap<>();
        map.forEach((k, v) -> ser.put(k, SolverDatastructures.serializeTristate(v)));
        return ser;
    }

    private static HashMap<Integer, Tristate> deserializeBbMap(final Map<Integer, ProtoBufSolverDatastructures.PbTristate> map) {
        if (map.isEmpty()) {
            return null;
        }
        final HashMap<Integer, Tristate> ser = new HashMap<>();
        map.forEach((k, v) -> ser.put(k, SolverDatastructures.deserializeTristate(v)));
        return ser;
    }

    private PbProofInformation serialize(final ProofInformation pi) {
        final PbProofInformation.Builder builder = PbProofInformation.newBuilder().setClause(serializeIntVec(pi.getClause()));
        if (pi.getProposition() != null) {
            builder.setProposition(ByteString.copyFrom(serializer.apply(pi.getProposition())));
        }
        return builder.build();
    }

    private ProofInformation deserialize(final PbProofInformation bin) {
        final Proposition prop = bin.hasProposition() ? deserializer.apply(bin.getProposition().toByteArray()) : null;
        return new ProofInformation(deserializeIntVec(bin.getClause()), prop);
    }

    private List<ByteString> serializeProps(final LngVector<Proposition> props) {
        final List<ByteString> res = new ArrayList<>();
        for (final Proposition prop : props) {
            res.add(ByteString.copyFrom(serializer.apply(prop)));
        }
        return res;
    }

    private LngVector<Proposition> deserializeProps(final List<ByteString> bin) {
        return new LngVector<>(bin.stream()
                .map(it -> deserializer.apply(it.toByteArray()))
                .collect(Collectors.toList()));
    }
}
