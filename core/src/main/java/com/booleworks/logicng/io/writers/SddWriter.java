package com.booleworks.logicng.io.writers;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A collection of function for exporting SDDs and vtrees into files.
 */
public class SddWriter {
    private SddWriter() {
    }

    /**
     * Exports the VTree and an SDD node of an SDD in text representation into a file.
     * <p>
     * The first part will be the exported VTree using the format of
     * {@link SddWriter#writeVTree(File, Sdd) writeVTree}.
     * The second part is the exported SDD node following the following syntax:
     * <ul>
     *     <li>First line: {@code sdd {number of nodes} {id of root}}</li>
     *     <li>{@code D {id of node} {number of sdd elements} [ {id of prime node} {id of sub node} ]+}</li>
     *     <li>{@code L {id of leaf} {name of variable}}</li>
     *     <li>{@code I {id of node} {id of left child} {id of right child}}"</li>
     * </ul>
     * <p>
     * Remark: The ids of the nodes may be different to the ids used by LNG internally.
     * @param destination the destination file
     * @param node        the node to export
     * @param sdd         the sdd of the node
     * @throws IOException if there was a problem writing the file
     */
    public static void writeSdd(final File destination, final SddNode node, final Sdd sdd) throws IOException {
        try (
                final Writer writer = new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                        StandardCharsets.UTF_8)
        ) {
            writeSdd(writer, node, sdd);
        }
    }

    /**
     * Exports the VTree and an SDD node of an SDD to text representation.
     * <p>
     * The first part will be the exported VTree using the format of
     * {@link SddWriter#writeVTree(File, Sdd) writeVTree}.
     * The second part is the exported SDD node following the following syntax:
     * <ul>
     *     <li>First line: {@code sdd {number of nodes} {id of root}}</li>
     *     <li>{@code D {id of node} {number of sdd elements} [ {id of prime node} {id of sub node} ]+}</li>
     *     <li>{@code L {id of leaf} {name of variable}}</li>
     *     <li>{@code I {id of node} {id of left child} {id of right child}}"</li>
     * </ul>
     * <p>
     * Remark: The ids of the nodes may be different to the ids used by LNG internally.
     * @param writer the destination
     * @param node   the node to export
     * @param sdd    the sdd of the node
     * @throws IOException if there was a problem writing the file
     */
    public static void writeSdd(final Writer writer, final SddNode node, final Sdd sdd) throws IOException {
        try (
                final BufferedWriter w = new BufferedWriter(writer)
        ) {
            final SddExportState state = new SddExportState();
            writeVTree(sdd, state.vState, w);
            w.newLine();
            final int rootId = exportSdd(node, sdd, state);
            w.append(String.format("sdd %d %d", state.size, rootId));
            w.newLine();
            for (final String line : state.lines) {
                w.append(line);
                w.newLine();
            }
            w.flush();
        }
    }

    private static int exportSdd(final SddNode node, final Sdd sdd, final SddExportState state) {
        if (state.nodeToId.containsKey(node)) {
            return state.nodeToId.get(node);
        }
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final List<Integer> children = new ArrayList<>(decomp.getElementsUnsafe().size() * 2);
            for (final SddElement element : decomp) {
                final int prime = exportSdd(element.getPrime(), sdd, state);
                final int sub = exportSdd(element.getSub(), sdd, state);
                children.add(prime);
                children.add(sub);
            }
            final int id = state.nodeId++;
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("D %d %d", id, decomp.getElementsUnsafe().size()));
            for (final int cId : children) {
                sb.append(" ");
                sb.append(cId);
            }
            state.lines.add(sb.toString());
            state.size++;
            state.nodeToId.put(node, id);
            return id;
        } else {
            final SddNodeTerminal terminal = node.asTerminal();
            final int id = state.nodeId++;
            if (terminal.isFalse()) {
                state.lines.add(String.format("F %d", id));
            } else if (terminal.isTrue()) {
                state.lines.add(String.format("T %d", id));
            } else {
                final int vTreeId = sdd.vTreeOf(node).getPosition();
                final int literalId = terminal.getPhase() ? 1 : -1;
                state.lines.add(String.format("L %d %d %d", id, vTreeId, literalId));
            }
            state.size++;
            state.nodeToId.put(node, id);
            return id;
        }
    }

    /**
     * Exports a VTree in text representation into a file.
     * <p>
     * File syntax:
     * <ul>
     *     <li>First line: {@code vtree {number of nodes} {id of root}}</li>
     *     <li>{@code L {id of leaf} {name of variable}}</li>
     *     <li>{@code I {id of node} {id of left child} {id of right child}}</li>
     * </ul>
     * <p>
     * Remark: The ids of the nodes may be different to the ids used by LNG internally.
     * @param file destination
     * @param sdd  the sdd which vtree gets exported
     * @throws IOException if there was a problem writing the file
     */
    public static void writeVTree(final File file, final Sdd sdd) throws IOException {
        try (
                final Writer writer =
                        new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)
        ) {
            writeVTree(writer, sdd);
        }
    }

    /**
     * Exports a VTree to text representation.
     * <p>
     * File syntax:
     * <ul>
     *     <li>First line: {@code vtree {number of nodes} {id of root}}</li>
     *     <li>{@code L {id of leaf} {name of variable}}</li>
     *     <li>{@code I {id of node} {id of left child} {id of right child}}</li>
     * </ul>
     * <p>
     * Remark: The ids of the nodes may be different to the ids used by LNG internally.
     * @param writer destination
     * @param sdd    the sdd which vtree gets exported
     * @throws IOException if there was a problem writing
     */
    public static void writeVTree(final Writer writer, final Sdd sdd) throws IOException {
        final VTreeExportState exportState = new VTreeExportState();
        try (
                final BufferedWriter w = new BufferedWriter(writer)
        ) {
            writeVTree(sdd, exportState, w);
        }
    }

    private static void writeVTree(final Sdd sdd, final VTreeExportState exportState,
                                   final BufferedWriter writer)
            throws IOException {
        final int rootId = exportVTreeRec(sdd.getVTree().getRoot(), sdd, exportState);
        writer.append(String.format("vtree %d %d", exportState.size, rootId));
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

    private static int exportVTreeRec(final VTree vTree, final Sdd sdd, final VTreeExportState state) {
        state.size++;
        if (vTree.isLeaf()) {
            final int id = vTree.getPosition();
            state.leafLines.add(String.format("L %d %s", id, sdd.indexToVariable(vTree.asLeaf().getVariable())));
            return id;
        } else {
            final VTreeInternal it = vTree.asInternal();
            final int left = exportVTreeRec(it.getLeft(), sdd, state);
            final int right = exportVTreeRec(it.getRight(), sdd, state);
            final int id = it.getPosition();
            state.nodeLines.add(String.format("I %d %d %d", id, left, right));
            return id;
        }
    }

    private static class VTreeExportState {
        int size = 0;
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
