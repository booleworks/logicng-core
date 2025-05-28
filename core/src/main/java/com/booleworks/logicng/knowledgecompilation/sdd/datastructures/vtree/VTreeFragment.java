package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddGlobalTransformations;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.TransformationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddSizeFunction;

import java.util.ArrayList;
import java.util.List;

public class VTreeFragment {
    private final static Move[] MOVES_LEFT = new Move[]{
            Move.RR, Move.SWR, Move.LR, Move.SWL,
            Move.RR, Move.SWR, Move.LR, Move.SWL,
            Move.RR, Move.SWR, Move.LR
    };
    private final static Move[] MOVES_RIGHT = new Move[]{
            Move.LR, Move.SWL, Move.RR, Move.SWR,
            Move.LR, Move.SWL, Move.RR, Move.SWR,
            Move.LR, Move.SWL, Move.RR
    };
    private final boolean isLeft;
    private final Sdd sdd;
    private VTree vTree;
    private int moveIndex;
    private int appliedIndex;
    private TransformationResult appliedTransRes;
    private final List<TransformationResult> transformations;

    public VTreeFragment(final boolean isLeft, final VTree vTree, final Sdd sdd) {
        if ((isLeft && !VTreeUtil.isLeftFragment(vTree)) || (!isLeft && !VTreeUtil.isRightFragment(vTree))) {
            throw new IllegalArgumentException("VTree is not a proper fragment");
        }
        this.vTree = vTree;
        this.sdd = sdd;
        this.isLeft = isLeft;
        this.moveIndex = 0;
        this.appliedIndex = 0;
        this.transformations = new ArrayList<>();
        this.appliedTransRes = TransformationResult.identity(vTree, sdd.getVTree());
    }

    public boolean hasNext() {
        return moveIndex < MOVES_LEFT.length;
    }

    public LngResult<TransformationResult> next(final ComputationHandler handler) {
        assert hasNext();
        final Move move = isLeft ? MOVES_LEFT[moveIndex] : MOVES_RIGHT[moveIndex];
        final LngResult<TransformationResult> transResult;
        switch (move) {
            case LR: {
                transResult = SddGlobalTransformations.rotateLeft(vTree.asInternal(), sdd, handler);
                break;
            }
            case RR: {
                transResult = SddGlobalTransformations.rotateRight(vTree.asInternal(), sdd, handler);
                break;
            }
            case SWL: {
                final LngResult<TransformationResult> tr =
                        SddGlobalTransformations.swap(vTree.asInternal().getLeft().asInternal(), sdd, handler);
                if (!tr.isSuccess()) {
                    transResult = tr;
                } else {
                    final TransformationResult t = tr.getResult();
                    final VTree transPoint =
                            sdd.vTreeInternal(t.getTransformationPoint(), vTree.asInternal().getRight());
                    transResult = LngResult.of(new TransformationResult(t.getTranslations(), transPoint, t.getRoot()));
                }
                break;
            }
            case SWR: {
                final LngResult<TransformationResult> tr =
                        SddGlobalTransformations.swap(vTree.asInternal().getRight().asInternal(), sdd, handler);
                if (!tr.isSuccess()) {
                    transResult = tr;
                } else {
                    final TransformationResult t = tr.getResult();
                    final VTree transPoint =
                            sdd.vTreeInternal(vTree.asInternal().getLeft(), t.getTransformationPoint());
                    transResult = LngResult.of(new TransformationResult(t.getTranslations(), transPoint, t.getRoot()));
                }
                break;
            }
            case SW: {
                transResult = SddGlobalTransformations.swap(vTree.asInternal(), sdd, handler);
                break;
            }
            default:
                throw new RuntimeException("Unreachable");
        }
        if (!transResult.isSuccess()) {
            return transResult;
        }
        vTree = transResult.getResult().getTransformationPoint();
        moveIndex++;
        transformations.add(transResult.getResult());
        return transResult;
    }

    public void rollback(final int moveIndex) {
        assert moveIndex >= appliedIndex;
        while (this.moveIndex > moveIndex) {
            this.moveIndex--;
            sdd.getVTreeStack().pop();
            transformations.remove(transformations.size() - 1);
        }
        sdd.recalculateVTrees();
    }

    public TransformationResult apply() {
        sdd.getVTreeStack().removeInactive(moveIndex - appliedIndex);
        appliedIndex = moveIndex;

        if (transformations.isEmpty()) {
            return appliedTransRes;
        } else {
            final TransformationResult collapsed =
                    TransformationResult.collapse(vTree, appliedTransRes, transformations);
            transformations.clear();
            appliedTransRes = collapsed;
            return collapsed;
        }
    }

    public long getSddSize() {
        return sdd.apply(new SddSizeFunction(sdd.getVTree().getPinnedNodes()));
    }

    public boolean isLeft() {
        return isLeft;
    }

    public Sdd getSdd() {
        return sdd;
    }

    public int getMoveIndex() {
        return moveIndex;
    }

    public VTree getVTree() {
        return vTree;
    }

    private enum Move {
        LR,
        RR,
        SW,
        SWL,
        SWR,
    }
}
