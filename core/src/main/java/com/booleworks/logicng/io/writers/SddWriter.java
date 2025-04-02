package com.booleworks.logicng.io.writers;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SddWriter {
    private SddWriter() {
    }

    public static void writeSdd(final File sddDesitination, final File vTreeDesitionation, final SddNode sdd,
                                final VTreeRoot root) throws IOException {
        final SddExportState state = new SddExportState();
        writeVTree(vTreeDesitionation, root.getVTree(sdd), state.vState);
        exportSdd(sdd, root, state);
        try (
                final BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(Files.newOutputStream(sddDesitination.toPath()), StandardCharsets.UTF_8))
        ) {
            writer.append(String.format("sdd %d", state.size));
            writer.newLine();
            for (final String line : state.lines) {
                writer.append(line);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private static int exportSdd(final SddNode sdd, final VTreeRoot root, final SddExportState state)
            throws IOException {
        if (state.nodeToId.containsKey(sdd)) {
            return state.nodeToId.get(sdd);
        }
        if (sdd.isDecomposition()) {
            final SddNodeDecomposition decomp = sdd.asDecomposition();
            final List<Integer> children = new ArrayList<>(decomp.getElements().size() * 2);
            for (final SddElement element : decomp.getElements()) {
                final int prime = exportSdd(element.getPrime(), root, state);
                final int sub = exportSdd(element.getSub(), root, state);
                children.add(prime);
                children.add(sub);
            }
            final int id = state.nodeId++;
            final int vTreeId = root.getPosition(root.getVTree(sdd));
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("D %d %d %d", id, vTreeId, decomp.getElements().size()));
            for (final int cId : children) {
                sb.append(" ");
                sb.append(cId);
            }
            state.lines.add(sb.toString());
            state.size++;
            state.nodeToId.put(sdd, id);
            return id;
        } else {
            final SddNodeTerminal terminal = sdd.asTerminal();
            final int id = state.nodeId++;
            if (terminal.getTerminal().getType() == FType.FALSE) {
                state.lines.add(String.format("F %d", id));
            } else if (terminal.getTerminal().getType() == FType.TRUE) {
                state.lines.add(String.format("T %d", id));
            } else {
                final int vTreeId = root.getPosition(root.getVTree(sdd));
                final int variableId = state.vState.varToId.get(((Literal) terminal.getTerminal()).variable());
                final int literalId = ((Literal) terminal.getTerminal()).getPhase() ? variableId : -variableId;
                state.lines.add(String.format("L %d %d %d", id, vTreeId, literalId));
            }
            state.size++;
            state.nodeToId.put(sdd, id);
            return id;
        }
    }

    /**
     * Exports a VTree in text representation into a file
     * <p>
     * File syntax:
     * <ul>
     *     <li>First line: "vtree {number of nodes}"</li>
     *     <li>"L {id of leaf} {id of variable}"</li>
     *     <li>"I {id of node} {id of left child} {id of right child}"</li>
     * </ul>
     * <p>
     * Note that the id of the variables and nodes may be different to the LNG
     * internal ids.
     * @param file  destination
     * @param vTree VTree that gets exported
     */
    public static void writeVTree(final File file, final VTree vTree) throws IOException {
        final VTreeExportState exportState = new VTreeExportState();
        writeVTree(file, vTree, exportState);
    }

    private static void writeVTree(final File file, final VTree vTree, final VTreeExportState exportState)
            throws IOException {
        exportVTreeRec(vTree, exportState);
        try (
                final BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))
        ) {
            writer.append(String.format("vtree %d", exportState.size));
            writer.newLine();
            for (final String line : exportState.leafLines) {
                writer.append(line);
                writer.newLine();
            }
            for (final String line : exportState.nodeLines) {
                writer.append(line);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private static int exportVTreeRec(final VTree vTree, final VTreeExportState state) {
        state.size++;
        if (vTree.isLeaf()) {
            final int id = state.nodeId++;
            final int varId = state.varId++;
            state.leafLines.add(String.format("L %d %d", id, varId));
            state.varToId.put(vTree.asLeaf().getVariable(), varId);
            return id;
        } else {
            final VTreeInternal it = vTree.asInternal();
            final int left = exportVTreeRec(it.getLeft(), state);
            final int right = exportVTreeRec(it.getRight(), state);
            final int id = state.nodeId++;
            state.nodeLines.add(String.format("I %d %d %d", id, left, right));
            return id;
        }
    }

    private static class VTreeExportState {
        int varId = 1;
        int nodeId = 0;
        int size = 0;
        HashMap<Variable, Integer> varToId = new HashMap<>();
        ArrayList<String> leafLines = new ArrayList<>();
        ArrayList<String> nodeLines = new ArrayList<>();
    }

    private static class SddExportState {
        VTreeExportState vState = new VTreeExportState();
        int nodeId = 0;
        int size = 0;
        HashMap<SddNode, Integer> nodeToId = new HashMap<>();
        ArrayList<String> lines = new ArrayList<>();
    }
}
