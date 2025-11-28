package com.booleworks.logicng.io.readers;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SddReader {
    private SddReader() {
    }

    /**
     * Imports an SDD from a file.
     * <p>
     * This function parses files that where exported by
     * {@link com.booleworks.logicng.io.writers.SddWriter#writeSdd(File, SddNode, Sdd) SddWriter.writeSdd()} or files
     * that follow the same format.  It assumes that the file contains a definition for the VTree as well as for an SDD
     * node.
     * <p>
     * Warning: When using files that were not exported by LogicNG, it is possible to create SDDs that do not follow
     * the invariants usually ensured by LogicNG.  This results in undefined behaviour, crashes, and/or wrong results.
     * Be aware of that when manually editing or writing files and when parsing user-provided files.
     * <p>
     * Remark: The ids of the nodes may be different to the ids used by LNG internally.
     * @param input the input file
     * @param f     the factory
     * @return the sdd node defined as root in the input file and the corresponding SDD container
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the VTree or SDD
     */
    public static Pair<SddNode, Sdd> readSdd(final File input, final FormulaFactory f)
            throws ParserException, IOException {
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Pair<VTree, Map<Integer, VTree>> vtreeDef = readVTree(f, input, builder);
        final Sdd sdd = new Sdd(f, builder.build(vtreeDef.getFirst()));
        final SddNode node = readSdd(input, vtreeDef.getSecond(), sdd);
        return new Pair<>(node, sdd);
    }

    private static SddNode readSdd(final File sddInput, final Map<Integer, VTree> id2Vtree, final Sdd sdd)
            throws IOException, ParserException {
        final HashMap<Integer, SddNode> sddNodes = new HashMap<>();
        int remainingLines = -1;
        int rootId = -1;
        try (final BufferedReader br = new BufferedReader(new FileReader(sddInput))) {
            int lineNumber = 1;
            while (br.ready()) {
                final String line = br.readLine();
                if (line.startsWith("sdd")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 3) {
                        throw new ParserException(
                                "Missing argument in line " + lineNumber + " in " + sddInput.getPath(), null);
                    }
                    remainingLines = Integer.parseInt(comps[1]);
                    rootId = Integer.parseInt(comps[2]);
                } else if (remainingLines > 0) {
                    if (line.startsWith("L ")) {
                        final String[] comps = line.split(" ");
                        if (comps.length < 4) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + sddInput.getPath(), null);
                        }
                        final int nodeId = Integer.parseInt(comps[1]);
                        final int vtreeLeafId = Integer.parseInt(comps[2]);
                        final boolean phase = Integer.parseInt(comps[3]) > 0;
                        final VTreeLeaf vtreeLeaf = id2Vtree.get(vtreeLeafId).asLeaf();
                        final SddNode node = sdd.terminal(vtreeLeaf, phase);
                        sddNodes.put(nodeId, node);
                        remainingLines--;
                    } else if (line.startsWith("T ")) {
                        final String[] comps = line.split(" ");
                        if (comps.length < 2) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                    null);
                        }
                        final int nodeId = Integer.parseInt(comps[1]);
                        final SddNode node = sdd.verum();
                        sddNodes.put(nodeId, node);
                        remainingLines--;
                    } else if (line.startsWith("F ")) {
                        final String[] comps = line.split(" ");
                        if (comps.length < 2) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                    null);
                        }
                        final int nodeId = Integer.parseInt(comps[1]);
                        final SddNode node = sdd.falsum();
                        sddNodes.put(nodeId, node);
                        remainingLines--;
                    } else if (line.startsWith("D ")) {
                        final String[] comps = line.split(" ");
                        if (comps.length < 3) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + sddInput.getPath(), null);
                        }
                        final int nodeId = Integer.parseInt(comps[1]);
                        final int elementCount = Integer.parseInt(comps[2]);
                        if (comps.length < elementCount * 2 + 3) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + sddInput.getPath(), null);
                        }
                        final ArrayList<SddElement> elements = new ArrayList<>();
                        for (int i = 3; i < elementCount * 2 + 3; i += 2) {
                            final SddNode prime = sddNodes.get(Integer.parseInt(comps[i]));
                            final SddNode sub = sddNodes.get(Integer.parseInt(comps[i + 1]));
                            elements.add(new SddElement(prime, sub));
                        }
                        final SddNode node = sdd.decompOfCompressedPartition(elements);
                        sddNodes.put(nodeId, node);
                        remainingLines--;
                    }
                } else if (remainingLines == 0) {
                    break;
                }
                lineNumber++;
            }
        }
        if (remainingLines != 0 || rootId == -1 || !sddNodes.containsKey(rootId)) {
            throw new ParserException("Invalid or missing SDD Definition", null);
        }
        return sddNodes.get(rootId);
    }

    /**
     * Imports a VTree from a file.
     * <p>
     * This function parses files that where exported by
     * {@link com.booleworks.logicng.io.writers.SddWriter#writeVTree(File, Sdd)  SddWriter.writeVTree()} or files
     * that follow the same format.
     * <p>
     * Warning: When using files that were not exported by LogicNG, it is possible to create SDDs that do not follow
     * the invariants usually ensured by LogicNG.  This results in undefined behaviour, crashes, and/or wrong results.
     * Be aware of that when manually editing or writing files and when parsing user-provided files.
     * <p>
     * Remark: The ids of the nodes may be different to the ids used by LNG internally.
     * @param file the input file
     * @param f    the factory
     * @return a pair with the root node and the vtree builder
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the VTree
     */
    public static Pair<VTree, VTreeRoot.Builder> readVTree(final File file, final FormulaFactory f)
            throws ParserException, IOException {
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Pair<VTree, Map<Integer, VTree>> vtreeDef = readVTree(f, file, builder);
        return new Pair<>(vtreeDef.getFirst(), builder);
    }

    private static Pair<VTree, Map<Integer, VTree>> readVTree(final FormulaFactory f, final File file,
                                                              final VTreeRoot.Builder builder)
            throws IOException, ParserException {
        final HashMap<Integer, VTree> fileIdToVtree = new HashMap<>();
        int rootId = -1;
        int remainingLines = -1;
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            int lineNumber = 1;
            while (br.ready()) {
                final String line = br.readLine();
                if (line.startsWith("vtree")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 3) {
                        throw new ParserException("Missing argument in line " + lineNumber + " in " + file.getPath(),
                                null);
                    }
                    remainingLines = Integer.parseInt(comps[1]);
                    rootId = Integer.parseInt(comps[2]);
                } else if (remainingLines > 0) {
                    if (line.startsWith("L ")) {
                        final String[] comps = line.split(" ");
                        if (comps.length < 3) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + file.getPath(), null);
                        }
                        final int nodeId = Integer.parseInt(comps[1]);
                        final Variable variable = f.variable(comps[2]);
                        final VTree vTree = builder.vTreeLeaf(variable);
                        fileIdToVtree.put(nodeId, vTree);
                        remainingLines--;
                    } else if (line.startsWith("I ")) {
                        final String[] comps = line.split(" ");
                        if (comps.length < 4) {
                            throw new ParserException(
                                    "Missing argument in line " + lineNumber + " in " + file.getPath(),
                                    null);
                        }
                        final int nodeId = Integer.parseInt(comps[1]);
                        final VTree left = fileIdToVtree.get(Integer.parseInt(comps[2]));
                        final VTree right = fileIdToVtree.get(Integer.parseInt(comps[3]));
                        final VTree vTree = builder.vTreeInternal(left, right);
                        fileIdToVtree.put(nodeId, vTree);
                        remainingLines--;
                    }

                } else if (remainingLines == 0) {
                    break;
                }
                lineNumber++;
            }
        }
        if (remainingLines != 0 || rootId == -1 || !fileIdToVtree.containsKey(rootId)) {
            throw new ParserException("Invalid or missing VTree Definition", null);
        }
        return new Pair<>(fileIdToVtree.get(rootId), fileIdToVtree);
    }
}
