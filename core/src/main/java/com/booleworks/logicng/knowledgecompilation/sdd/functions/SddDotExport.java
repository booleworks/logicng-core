package com.booleworks.logicng.knowledgecompilation.sdd.functions;


import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class SddDotExport implements SddFunction<Boolean> {
    private static final String DOT = "&#9210;";
    private static final String TOP = "&#8868;";
    private static final String BOT = "&#8869;";
    private static final String NOT = "&not;";

    private final SddNode node;
    private final Writer writer;
    private final HashMap<SddNode, GraphVTreeNode> nodeCache = new HashMap<>();
    private final HashMap<SddElement, GraphVTreeGroup> groupCache = new HashMap<>();
    GraphSdd result;

    public SddDotExport(final SddNode node, final Writer writer) {
        this.node = node;
        this.writer = writer;
    }

    @Override
    public LngResult<Boolean> apply(final Sdd sf, final ComputationHandler handler) {
        nodeCache.clear();
        groupCache.clear();
        result = new GraphSdd();
        final BufferedWriter writer = new BufferedWriter(this.writer);
        if (node.isFalse()) {
            final GraphSddRank rank = new GraphSddRank(0);
            rank.elements.add(new GraphSddElement("root_falsum", GraphSddElementLabel.terminal(TOP),
                    GraphSddElementLabel.terminal(BOT)));
            result.ranks.put(0, rank);
        } else if (node.isTrue()) {
            final GraphSddRank rank = new GraphSddRank(0);
            final GraphSddElement elem =
                    new GraphSddElement("root_verum", GraphSddElementLabel.terminal(TOP),
                            GraphSddElementLabel.terminal(TOP));
            result.elements.add(elem);
            rank.elements.add(elem);
            result.ranks.put(0, rank);
        } else {
            irSddNode(node, 0, null, sf);
        }
        try {
            result.write(writer);
            writer.flush();
            writer.close();
        } catch (final IOException e) {
            return LngResult.of(false);
        }
        return LngResult.of(true);
    }

    private GraphVTreeNode irSddNode(final SddNode currentNode, final int rank, GraphVTreeGroup parentGroup,
                                     final Sdd sdd) {
        final VTree vtree = sdd.vTreeOf(currentNode);
        final GraphVTreeNode newVTreeNode = GraphVTreeNode.fromSddNode(currentNode, vtree, parentGroup);
        result.vtrees.add(newVTreeNode);
        if (parentGroup == null) {
            parentGroup = new GraphVTreeGroup("group_root", currentNode.getId());
        }
        parentGroup.nodes.add(newVTreeNode);
        if (!result.ranks.containsKey(rank)) {
            result.ranks.put(rank, new GraphVTreeRank(rank));
        }
        final GraphVTreeRank vRank = (GraphVTreeRank) result.ranks.get(rank);
        vRank.groups.add(parentGroup);

        final int elementRank = rank + 1;
        if (currentNode.isDecomposition()) {
            final SddNodeDecomposition decomp = currentNode.asDecomposition();
            for (final SddElement element : decomp.getElements()) {
                final GraphSddElement elementNode = irSddElement(element, elementRank, currentNode.getId(), sdd);
                addNewElementNode(elementNode, elementRank, newVTreeNode);
            }
        } else {
            // This only happens if the SDD consists of only a terminal
            final String sddId = String.format("root_%d", currentNode.getId());
            final String prime = terminalToUTF8(currentNode.asTerminal(), sdd);
            final GraphSddElement sddNode = new GraphSddElement(
                    sddId, GraphSddElementLabel.terminal(prime), GraphSddElementLabel.terminal(TOP));
            addNewElementNode(sddNode, elementRank, newVTreeNode);
        }
        return newVTreeNode;
    }

    private void addNewElementNode(final GraphSddElement newNode, final int rank, final GraphVTreeNode parentVTree) {
        parentVTree.nodes.add(newNode);
        result.elements.add(newNode);
        if (!result.ranks.containsKey(rank)) {
            result.ranks.put(rank, new GraphSddRank(rank));
        }
        final GraphSddRank sRank = (GraphSddRank) result.ranks.get(rank);
        sRank.elements.add(newNode);
    }

    private GraphSddElement irSddElement(final SddElement element, final int rank, final int parentId, final Sdd sdd) {
        if (!groupCache.containsKey(element)) {
            final GraphVTreeGroup newGroup = GraphVTreeGroup.fromElement(element, parentId);
            groupCache.put(element, newGroup);
        }
        final GraphVTreeGroup elementGroup = groupCache.get(element);
        final GraphSddElementLabel primeLabel = irPrimeSub(element.getPrime(), rank + 1, elementGroup, sdd);
        final GraphSddElementLabel subLabel = irPrimeSub(element.getSub(), rank + 1, elementGroup, sdd);
        return GraphSddElement.fromElement(element, primeLabel, subLabel);
    }

    private GraphSddElementLabel irPrimeSub(final SddNode currentNode, final int rank,
                                            final GraphVTreeGroup parentGroup, final Sdd sdd) {
        if (currentNode.isDecomposition()) {
            if (!nodeCache.containsKey(currentNode)) {
                nodeCache.put(currentNode, irSddNode(currentNode, rank, parentGroup, sdd));
            }
            return GraphSddElementLabel.reference(nodeCache.get(currentNode));
        } else {
            final String label = terminalToUTF8(currentNode.asTerminal(), sdd);
            return GraphSddElementLabel.terminal(label);
        }
    }

    private static String terminalToUTF8(final SddNodeTerminal terminal, final Sdd sdd) {
        if (terminal.isLiteral()) {
            final Variable v = sdd.indexToVariable(terminal.getVTree().getVariable());
            if (terminal.getPhase()) {
                return v.getName();
            } else {
                return String.format("%s%s", NOT, v.getName());
            }
        } else if (terminal.isTrue()) {
            return TOP;
        } else {
            return BOT;
        }
    }

    private static class GraphSddElementLabel {
        final GraphVTreeNode vTree;
        final String terminal;

        private GraphSddElementLabel(final GraphVTreeNode vTree, final String terminal) {
            this.vTree = vTree;
            this.terminal = terminal;
        }

        public static GraphSddElementLabel reference(final GraphVTreeNode vTree) {
            return new GraphSddElementLabel(vTree, null);
        }

        public static GraphSddElementLabel terminal(final String terminal) {
            return new GraphSddElementLabel(null, terminal);
        }

        public boolean isReference() {
            return vTree != null;
        }

        @Override
        public String toString() {
            return isReference() ? DOT : terminal;
        }
    }

    private static class GraphSddElement {
        final String id;
        final GraphSddElementLabel prime;
        final GraphSddElementLabel sub;

        GraphSddElement(final String id, final GraphSddElementLabel prime, final GraphSddElementLabel sub) {
            this.id = id;
            this.prime = prime;
            this.sub = sub;
        }

        public static GraphSddElement fromElement(final SddElement element, final GraphSddElementLabel prime,
                                                  final GraphSddElementLabel sub) {
            final String id = String.format("sdd_%d_%d", element.getPrime().getId(), element.getSub().getId());
            return new GraphSddElement(id, prime, sub);
        }

        String getId() {
            return id;
        }

        public void writeDeclaration(final BufferedWriter writer) throws IOException {
            writer.write(String.format("%s [label=\"<prime> %s |<sub> %s\"];", id, prime, sub));
        }
    }

    private static class GraphVTreeNode {
        final String id;
        final String label;
        final ArrayList<GraphSddElement> nodes = new ArrayList<>();

        GraphVTreeNode(final String id, final String label) {
            this.id = id;
            this.label = label;
        }

        public static GraphVTreeNode fromSddNode(final SddNode n, final VTree vTree,
                                                 final GraphVTreeGroup parentGroup) {
            final String id = String.format("vtree_%d_%s", n.getId(),
                    parentGroup == null ? "none" : String.valueOf(parentGroup.parentId));
            return new GraphVTreeNode(id, String.valueOf(vTree.getId()));
        }

        String getId() {
            return id;
        }

        public void writeDeclaration(final BufferedWriter writer) throws IOException {
            writer.write(String.format("%s [label=\"%s\"];", id, label));
        }
    }

    private static class GraphVTreeGroup {
        final String id;
        final int parentId;
        final ArrayList<GraphVTreeNode> nodes = new ArrayList<>();

        public GraphVTreeGroup(final String id, final int parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        public static GraphVTreeGroup fromElement(final SddElement element, final int parentId) {
            final String id = String.format("group_%d_%d", element.getPrime().getId(), element.getSub().getId());
            return new GraphVTreeGroup(id, parentId);
        }

        public void write(final BufferedWriter writer) throws IOException {
            writer.write(String.format("subgraph cluster_%s {", id));
            writer.newLine();
            for (final GraphVTreeNode node : nodes) {
                writer.write(String.format("%s;", node.getId()));
            }
            writer.newLine();
            writer.write('}');
        }
    }

    private interface IRRank {
        void write(final BufferedWriter writer) throws IOException;
    }

    private static class GraphSddRank implements IRRank {
        final int rank;
        final LinkedHashSet<GraphSddElement> elements = new LinkedHashSet<>();

        public GraphSddRank(final int rank) {
            this.rank = rank;
        }

        @Override
        public void write(final BufferedWriter writer) throws IOException {
            writer.write(String.format("subgraph cluster_rank%d {", rank));
            writer.newLine();
            for (final GraphSddElement elem : elements) {
                writer.write(String.format("%s;", elem.getId()));
            }
            writer.newLine();
            writer.write('}');
        }
    }

    private static class GraphVTreeRank implements IRRank {
        final int rank;
        final LinkedHashSet<GraphVTreeGroup> groups = new LinkedHashSet<>();

        public GraphVTreeRank(final int rank) {
            this.rank = rank;
        }

        @Override
        public void write(final BufferedWriter writer) throws IOException {
            writer.write(String.format("subgraph cluster_rank%d {", rank));
            writer.newLine();
            writer.write("style = invis;");
            writer.newLine();
            writer.write("rank = same;");
            writer.newLine();
            for (final GraphVTreeGroup group : groups) {
                group.write(writer);
                writer.newLine();
            }
            writer.write('}');
        }
    }

    private static class GraphSdd {
        LinkedHashSet<GraphVTreeNode> vtrees = new LinkedHashSet<>();
        LinkedHashSet<GraphSddElement> elements = new LinkedHashSet<>();
        HashMap<Integer, IRRank> ranks = new HashMap<>();

        public GraphSdd() {
        }

        public void write(final BufferedWriter writer) throws IOException {
            writer.write("digraph G {");
            writer.newLine();
            writer.write("graph [style = invis, rank = same]");
            writer.newLine();
            writer.write("node [shape = record]");
            writer.newLine();
            for (final GraphSddElement element : elements) {
                element.writeDeclaration(writer);
                writer.newLine();
            }
            writer.write("node [shape = circle, fontsize = 10, width = 0.32, margin = 0]");
            writer.newLine();
            for (final GraphVTreeNode vTree : vtrees) {
                vTree.writeDeclaration(writer);
                writer.newLine();
            }
            for (int i = ranks.size() - 1; i >= 0; --i) {
                ranks.get(i).write(writer);
                writer.newLine();
            }
            writeEdges(writer);
            writer.write('}');
        }

        public void writeEdges(final BufferedWriter writer) throws IOException {
            for (final GraphSddElement element : elements) {
                if (element.prime.isReference()) {
                    writer.write(
                            String.format("%s:prime:c -> %s [tailclip=false];", element.id, element.prime.vTree.id));
                    writer.newLine();
                }
                if (element.sub.isReference()) {
                    writer.write(String.format("%s:sub:c -> %s [tailclip=false];", element.id, element.sub.vTree.id));
                    writer.newLine();
                }
            }
            for (final GraphVTreeNode vtree : vtrees) {
                for (final GraphSddElement node : vtree.nodes) {
                    writer.write(String.format("%s -> %s", vtree.id, node.id));
                    writer.newLine();
                }
            }
        }
    }
}
