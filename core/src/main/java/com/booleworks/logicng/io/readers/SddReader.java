package com.booleworks.logicng.io.readers;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;

public class SddReader {
    private SddReader() {
    }

    public static Pair<SddNode, VTreeRoot> readSdd(final File sddInput, final File vTreeInput, final Sdd sf)
            throws ParserException, IOException {
        final VTreeRoot root = sf.constructRoot(readVTree(vTreeInput, sf));
        final SddNode node = readSdd(sddInput, root, sf);
        return new Pair<>(node, root);
    }

    public static SddNode readSdd(final File sddInput, final VTreeRoot root, final Sdd sf)
            throws IOException, ParserException {
        final HashMap<Integer, SddNode> sddNodes = new HashMap<>();
        SddNode last = null;
        try (final BufferedReader br = new BufferedReader(new FileReader(sddInput))) {
            int lineNumber = 1;
            while (br.ready()) {
                final String line = br.readLine();
                if (line.startsWith("L ")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 4) {
                        throw new ParserException(
                                "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                null);
                    }
                    final int nodeId = Integer.parseInt(comps[1]);
                    final int varId = Integer.parseInt(comps[3]);
                    final Variable variable = sf.getFactory().variable(String.format("v%d", Math.abs(varId)));
                    final VTreeLeaf leaf = sf.vTreeLeaf(variable);
                    final SddNode node = sf.terminal(leaf, varId > 0, root);
                    sddNodes.put(nodeId, node);
                    last = node;
                } else if (line.startsWith("T ")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 2) {
                        throw new ParserException(
                                "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                null);
                    }
                    final int nodeId = Integer.parseInt(comps[1]);
                    final SddNode node = sf.verum();
                    sddNodes.put(nodeId, node);
                    last = node;
                } else if (line.startsWith("F ")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 2) {
                        throw new ParserException(
                                "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                null);
                    }
                    final int nodeId = Integer.parseInt(comps[1]);
                    final SddNode node = sf.falsum();
                    sddNodes.put(nodeId, node);
                    last = node;
                } else if (line.startsWith("D ")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 4) {
                        throw new ParserException(
                                "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                null);
                    }
                    final int nodeId = Integer.parseInt(comps[1]);
                    final int elementCount = Integer.parseInt(comps[3]);
                    if (comps.length < elementCount * 2 + 4) {
                        throw new ParserException(
                                "Missing argument in line " + lineNumber + " in " + sddInput.getPath(),
                                null);
                    }
                    final TreeSet<SddElement> elements = new TreeSet<>();
                    for (int i = 4; i < elementCount * 2 + 4; i += 2) {
                        final SddNode prime = sddNodes.get(Integer.parseInt(comps[i]));
                        final SddNode sub = sddNodes.get(Integer.parseInt(comps[i + 1]));
                        elements.add(new SddElement(prime, sub));
                    }
                    final SddNode node = sf.decomposition(elements, root);
                    sddNodes.put(nodeId, node);
                    last = node;
                }
                lineNumber++;
            }
        }
        return last;
    }

    public static VTree readVTree(final File file, final Sdd sf)
            throws IOException, ParserException {
        final HashMap<Integer, VTree> fileIdToVtree = new HashMap<>();
        VTree last = null;
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            int lineNumber = 1;
            while (br.ready()) {
                final String line = br.readLine();
                if (line.startsWith("L ")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 3) {
                        throw new ParserException("Missing argument in line " + lineNumber + " in " + file.getPath(),
                                null);
                    }
                    final int nodeId = Integer.parseInt(comps[1]);
                    final Variable variable = sf.getFactory().variable(String.format("v%s", comps[2]));
                    final VTree vTree = sf.vTreeLeaf(variable);
                    fileIdToVtree.put(nodeId, vTree);
                    last = vTree;
                } else if (line.startsWith("I ")) {
                    final String[] comps = line.split(" ");
                    if (comps.length < 4) {
                        throw new ParserException("Missing argument in line " + lineNumber + " in " + file.getPath(),
                                null);
                    }
                    final int nodeId = Integer.parseInt(comps[1]);
                    final VTree left = fileIdToVtree.get(Integer.parseInt(comps[2]));
                    final VTree right = fileIdToVtree.get(Integer.parseInt(comps[3]));
                    final VTree vTree = sf.vTreeInternal(left, right);
                    fileIdToVtree.put(nodeId, vTree);
                    last = vTree;
                }
                lineNumber++;
            }
        }
        if (last == null) {
            throw new ParserException("Empty VTree Definition", null);
        }
        return last;
    }
}
