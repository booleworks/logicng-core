package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.io.graphical.GraphicalColor;
import com.booleworks.logicng.io.graphical.GraphicalEdge;
import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.io.graphical.GraphicalNode;
import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

public final class VTreeDotExport {
    private VTreeDotExport() {
    }

    private static final GraphicalNodeStyle INNER_NODE_STYLE =
            GraphicalNodeStyle.circle(GraphicalColor.BLACK, GraphicalColor.BLACK, null);
    private static final GraphicalNodeStyle LEAF_NODE_STYLE =
            GraphicalNodeStyle.rectangle(GraphicalColor.BLACK, GraphicalColor.BLACK, null);
    private static final GraphicalEdgeStyle EDGE_STYLE = GraphicalEdgeStyle.solid(GraphicalColor.BLACK);

    public static GraphicalRepresentation exportDot(final VTree vTree, final Sdd sdd) {
        final GraphicalRepresentation gr = new GraphicalRepresentation(false, false);
        drawRec(vTree, null, gr, sdd);
        return gr;
    }

    private static void drawRec(final VTree vTree, final GraphicalNode parent, final GraphicalRepresentation gr,
                                final Sdd sdd) {
        final String id = String.valueOf(vTree.getId());
        final GraphicalNode current;
        if (vTree.isLeaf()) {
            current = new GraphicalNode(id, sdd.indexToVariable(vTree.asLeaf().getVariable()).toString(), true,
                    LEAF_NODE_STYLE);
        } else {
            current = new GraphicalNode(id, id, false, INNER_NODE_STYLE);
        }
        gr.addNode(current);
        if (parent != null) {
            final GraphicalEdge edge = new GraphicalEdge(parent, current, EDGE_STYLE);
            gr.addEdge(edge);
        }
        if (!vTree.isLeaf()) {
            drawRec(vTree.asInternal().getLeft(), current, gr, sdd);
            drawRec(vTree.asInternal().getRight(), current, gr, sdd);
        }
    }
}
