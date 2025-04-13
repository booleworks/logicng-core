package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.io.graphical.GraphicalColor;
import com.booleworks.logicng.io.graphical.GraphicalEdge;
import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.io.graphical.GraphicalNode;
import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

public class VTreeDotExport implements SddFunction<GraphicalRepresentation> {
    private static final GraphicalNodeStyle INNER_NODE_STYLE =
            GraphicalNodeStyle.circle(GraphicalColor.BLACK, GraphicalColor.BLACK, null);
    private static final GraphicalNodeStyle LEAF_NODE_STYLE =
            GraphicalNodeStyle.rectangle(GraphicalColor.BLACK, GraphicalColor.BLACK, null);
    private static final GraphicalEdgeStyle EDGE_STYLE = GraphicalEdgeStyle.solid(GraphicalColor.BLACK);
    final VTree vTree;

    public VTreeDotExport(final VTree vTree) {
        this.vTree = vTree;
    }

    @Override
    public LngResult<GraphicalRepresentation> apply(final SddFactory sf, final ComputationHandler handler) {
        final GraphicalRepresentation gr = new GraphicalRepresentation(false, false);
        drawRec(vTree, null, gr);
        return LngResult.of(gr);
    }

    private void drawRec(final VTree vTree, final GraphicalNode parent, final GraphicalRepresentation gr) {
        final String id = String.valueOf(vTree.getId());
        final GraphicalNode current = vTree.isLeaf() ?
                                      new GraphicalNode(id, vTree.asLeaf().getVariable().toString(), true,
                                              LEAF_NODE_STYLE)
                                                     : new GraphicalNode(id, id, false, INNER_NODE_STYLE);
        gr.addNode(current);
        if (parent != null) {
            final GraphicalEdge edge = new GraphicalEdge(parent, current, EDGE_STYLE);
            gr.addEdge(edge);
        }
        if (!vTree.isLeaf()) {
            drawRec(vTree.asInternal().getLeft(), current, gr);
            drawRec(vTree.asInternal().getRight(), current, gr);
        }
    }
}
