package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddGlobalTransformations;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddGlobalTransformationEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.TransformationResult;

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
        if (!handler.shouldResume(move.asStartEvent())) {
            return LngResult.canceled(move.asStartEvent());
        }
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
            rollbackAbortedStep();
            return transResult;
        }
        vTree = transResult.getResult().getTransformationPoint();
        moveIndex++;
        transformations.add(transResult.getResult());
        if (!handler.shouldResume(move.asCompletedEvent())) {
            return LngResult.canceled(move.asCompletedEvent());
        }
        return transResult;
    }

    private void rollbackAbortedStep() {
        sdd.getVTreeStack().pop();
        sdd.getVTreeStack().bumpGeneration();
    }

    public void rollback(final int moveIndex) {
        assert moveIndex >= appliedIndex;
        while (this.moveIndex > moveIndex) {
            this.moveIndex--;
            sdd.getVTreeStack().pop();
            transformations.remove(transformations.size() - 1);
        }
        sdd.getVTreeStack().bumpGeneration();
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
        SWR;

        LngEvent asStartEvent() {
            switch (this) {
                case LR:
                    return SddGlobalTransformationEvent.START_LEFT_ROTATION;
                case RR:
                    return SddGlobalTransformationEvent.START_RIGHT_ROTATION;
                case SW:
                case SWL:
                case SWR:
                    return SddGlobalTransformationEvent.START_SWAP;
            }
            throw new IllegalStateException("Unreachable");
        }

        LngEvent asCompletedEvent() {
            switch (this) {
                case LR:
                    return SddGlobalTransformationEvent.COMPLETED_LEFT_ROTATION;
                case RR:
                    return SddGlobalTransformationEvent.COMPLETED_RIGHT_ROTATION;
                case SW:
                case SWL:
                case SWR:
                    return SddGlobalTransformationEvent.COMPLETED_SWAP;
            }
            throw new IllegalStateException("Unreachable");
        }
    }
}
