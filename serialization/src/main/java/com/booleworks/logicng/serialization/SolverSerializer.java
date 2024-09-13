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

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.serialization.ProtoBufSatSolver.PBSatSolver;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBClause;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBClauseVector;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBProofInformation;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBVariableVector;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBWatcherVector;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBWatcherVectorVector;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.datastructures.LNGClause;
import com.booleworks.logicng.solvers.datastructures.LNGVariable;
import com.booleworks.logicng.solvers.datastructures.LNGWatcher;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver.ProofInformation;
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
                return Propositions.deserializePropositions(f, ProtoBufPropositions.PBStandardProposition.newBuilder().mergeFrom(bs).build());
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
    public void serializeSolverToFile(final SATSolver solver, final Path path, final boolean compress) throws IOException {
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
    public void serializeSolverToStream(final SATSolver solver, final OutputStream stream) throws IOException {
        serializeSolver(solver).writeTo(stream);
    }

    /**
     * Serializes a SAT solver to a protocol buffer.
     * @param solver the SAT solver
     * @return the protocol buffer
     */
    public PBSatSolver serializeSolver(final SATSolver solver) {
        return serialize(solver);
    }

    /**
     * Deserializes a Sat solver from a file.
     * @param path     the file path
     * @param compress a flag whether the file should be compressed (zip)
     * @return the solver
     * @throws IOException if there is a problem reading the file
     */
    public SATSolver deserializeSatSolverFromFile(final Path path, final boolean compress) throws IOException {
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
    public SATSolver deserializeSatSolverFromStream(final InputStream stream) throws IOException {
        return deserializeSatSolver(PBSatSolver.newBuilder().mergeFrom(stream).build());
    }

    /**
     * Deserializes a SAT solver from a protocol buffer.
     * @param bin the protocol buffer
     * @return the solver
     */
    public SATSolver deserializeSatSolver(final PBSatSolver bin) {
        return deserialize(bin);
    }

    PBSatSolver serialize(final SATSolver solver) {
        final var core = solver.underlyingSolver();
        final LNGVector<LNGClause> clauses = getField(core, "clauses");
        final LNGVector<LNGClause> learnts = getField(core, "learnts");
        final IdentityHashMap<LNGClause, Integer> clauseMap = generateClauseMap(clauses, learnts);
        final PBSatSolver.Builder builder = PBSatSolver.newBuilder();

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

        final LNGVector<LNGIntVector> pgProof = getField(core, "pgProof");
        if (pgProof != null) {
            builder.setPgProof(Collections.serializeVec(pgProof));
        }
        final LNGVector<ProofInformation> pgOriginalClauses = getField(core, "pgOriginalClauses");
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
        final LNGIntVector backboneAssumptions = getField(core, "backboneAssumptions");
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
        builder.setAnalyzeLBD(getField(core, "analyzeLBD"));
        builder.setNbClausesBeforeReduce(getField(core, "nbClausesBeforeReduce"));
        builder.setConflicts(getField(core, "conflicts"));
        builder.setConflictsRestarts(getField(core, "conflictsRestarts"));
        builder.setSumLBD(getField(core, "sumLBD"));
        builder.setCurRestart(getField(core, "curRestart"));

        return builder.build();
    }

    SATSolver deserialize(final PBSatSolver bin) {
        final Map<Integer, LNGClause> clauseMap = new TreeMap<>();
        final var core = new LNGCoreSolver(f, SatSolverConfigs.deserializeSatSolverConfig(bin.getConfig()));
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
            final LNGVector<ProofInformation> originalClauses = new LNGVector<>(bin.getPgOriginalClausesCount());
            for (final PBProofInformation pi : bin.getPgOriginalClausesList()) {
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
        setField(core, "analyzeLBD", bin.getAnalyzeLBD());
        setField(core, "nbClausesBeforeReduce", bin.getNbClausesBeforeReduce());
        setField(core, "conflicts", bin.getConflicts());
        setField(core, "conflictsRestarts", bin.getConflictsRestarts());
        setField(core, "sumLBD", bin.getSumLBD());
        setField(core, "curRestart", bin.getCurRestart());
        return new SATSolver(f, core);
    }

    private static IdentityHashMap<LNGClause, Integer> generateClauseMap(final LNGVector<LNGClause> clauses, final LNGVector<LNGClause> learnts) {
        final IdentityHashMap<LNGClause, Integer> clauseMap = new IdentityHashMap<>();
        for (final LNGClause clause : clauses) {
            clauseMap.put(clause, clauseMap.size());
        }
        for (final LNGClause learnt : learnts) {
            clauseMap.put(learnt, clauseMap.size());
        }
        return clauseMap;
    }

    private static PBClauseVector serializeClauseVec(final LNGVector<LNGClause> vec,
                                                     final IdentityHashMap<LNGClause, Integer> clauseMap) {
        final PBClauseVector.Builder builder = PBClauseVector.newBuilder();
        for (final LNGClause clause : vec) {
            builder.addElement(SolverDatastructures.serializeClause(clause, clauseMap.get(clause)));
        }
        return builder.build();
    }

    private static LNGVector<LNGClause> deserializeClauseVec(final PBClauseVector bin, final Map<Integer, LNGClause> clauseMap) {
        final LNGVector<LNGClause> vec = new LNGVector<>(bin.getElementCount());
        for (int i = 0; i < bin.getElementCount(); i++) {
            final PBClause binClause = bin.getElement(i);
            final LNGClause clause = SolverDatastructures.deserializeClause(binClause);
            clauseMap.put(binClause.getId(), clause);
            vec.push(clause);
        }
        return vec;
    }

    private static PBWatcherVectorVector serializeWatches(final LNGVector<LNGVector<LNGWatcher>> vec,
                                                          final IdentityHashMap<LNGClause, Integer> clauseMap) {
        final PBWatcherVectorVector.Builder builder = PBWatcherVectorVector.newBuilder();
        for (final LNGVector<LNGWatcher> watchList : vec) {
            final PBWatcherVector.Builder watchBuilder = PBWatcherVector.newBuilder();
            for (final LNGWatcher watch : watchList) {
                watchBuilder.addElement(SolverDatastructures.serializeWatcher(watch, clauseMap));
            }
            builder.addElement(watchBuilder.build());
        }
        return builder.build();
    }

    private static LNGVector<LNGVector<LNGWatcher>> deserializeWatches(final PBWatcherVectorVector bin,
                                                                       final Map<Integer, LNGClause> clauseMap) {
        final LNGVector<LNGVector<LNGWatcher>> vec = new LNGVector<>(bin.getElementCount());
        for (int i = 0; i < bin.getElementCount(); i++) {
            final PBWatcherVector binWatch = bin.getElement(i);
            final LNGVector<LNGWatcher> watch = new LNGVector<>(binWatch.getElementCount());
            for (int j = 0; j < binWatch.getElementCount(); j++) {
                watch.push(SolverDatastructures.deserializeWatcher(binWatch.getElement(j), clauseMap));
            }
            vec.push(watch);
        }
        return vec;
    }

    private static PBVariableVector serializeVarVec(final LNGVector<LNGVariable> vec,
                                                    final IdentityHashMap<LNGClause, Integer> clauseMap) {
        final PBVariableVector.Builder builder = PBVariableVector.newBuilder();
        for (final LNGVariable var : vec) {
            builder.addElement(SolverDatastructures.serializeVariable(var, clauseMap));
        }
        return builder.build();
    }

    private static LNGVector<LNGVariable> deserializeVarVec(final PBVariableVector bin, final Map<Integer, LNGClause> clauseMap) {
        final LNGVector<LNGVariable> vec = new LNGVector<>(bin.getElementCount());
        for (int i = 0; i < bin.getElementCount(); i++) {
            vec.push(SolverDatastructures.deserializeVariable(bin.getElement(i), clauseMap));
        }
        return vec;
    }

    public static ProtoBufCollections.PBIntVector serializeStack(final Stack<Integer> stack) {
        if (stack == null) {
            return null;
        }
        final ProtoBufCollections.PBIntVector.Builder vec = ProtoBufCollections.PBIntVector.newBuilder();
        for (final Integer integer : stack) {
            vec.addElement(integer);
        }
        vec.setSize(stack.size());
        return vec.build();
    }

    public static Stack<Integer> deserializeStack(final ProtoBufCollections.PBIntVector vec) {
        final Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < vec.getSize(); i++) {
            stack.push(vec.getElement(i));
        }
        return stack;
    }

    private static HashMap<Integer, ProtoBufSolverDatastructures.PBTristate> serializeBbMap(final Map<Integer, Tristate> map) {
        final HashMap<Integer, ProtoBufSolverDatastructures.PBTristate> ser = new HashMap<>();
        map.forEach((k, v) -> ser.put(k, SolverDatastructures.serializeTristate(v)));
        return ser;
    }

    private static HashMap<Integer, Tristate> deserializeBbMap(final Map<Integer, ProtoBufSolverDatastructures.PBTristate> map) {
        if (map.isEmpty()) {
            return null;
        }
        final HashMap<Integer, Tristate> ser = new HashMap<>();
        map.forEach((k, v) -> ser.put(k, SolverDatastructures.deserializeTristate(v)));
        return ser;
    }

    private PBProofInformation serialize(final ProofInformation pi) {
        final PBProofInformation.Builder builder = PBProofInformation.newBuilder().setClause(serializeIntVec(pi.clause()));
        if (pi.proposition() != null) {
            builder.setProposition(ByteString.copyFrom(serializer.apply(pi.proposition())));
        }
        return builder.build();
    }

    private ProofInformation deserialize(final PBProofInformation bin) {
        final Proposition prop = bin.hasProposition() ? deserializer.apply(bin.getProposition().toByteArray()) : null;
        return new ProofInformation(deserializeIntVec(bin.getClause()), prop);
    }

    private List<ByteString> serializeProps(final LNGVector<Proposition> props) {
        final List<ByteString> res = new ArrayList<>();
        for (final Proposition prop : props) {
            res.add(ByteString.copyFrom(serializer.apply(prop)));
        }
        return res;
    }

    private LNGVector<Proposition> deserializeProps(final List<ByteString> bin) {
        return new LNGVector<>(bin.stream()
                .map(it -> deserializer.apply(it.toByteArray()))
                .collect(Collectors.toList()));
    }
}
