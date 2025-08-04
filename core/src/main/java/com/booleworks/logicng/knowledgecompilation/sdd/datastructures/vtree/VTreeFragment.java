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

/**
 * A class for exploring local transformations on a vtree fragment.
 * <p>
 * A vtree fragment is an internal vtree node where either at least the left
 * child is an internal node (left fragment) or the right child is an internal
 * node (right) fragment.
 * <p>
 * Given a fragment for example ((a b) c) it is possible to explore all possible
 * structures of this fragment with 12 global vtree operations (left rotate,
 * right rotate, and swapping). Combined with global vtree transformations, one
 * can explore different vtree configurations and the effect on the SDDs based
 * on the vtree.  This can be used to minimize the SDD.
 * <p>
 * <strong>Remark:</strong> This class heavily use global transformations.
 * Please read the documentation ({@link SddGlobalTransformations}) if you want
 * to get a proper understanding.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VTreeFragment {
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

    /**
     * Create a new state for the transformations on this fragment.
     * @param isLeft whether the fragment is a left fragment
     * @param vTree  the vtree node which is a fragment
     * @param sdd    the SDD container
     */
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

    /**
     * Return whether there is an unexplored state left for this vtree fragment.
     * @return whether there is an unexplored state left for this vtree fragment
     */
    public boolean hasNext() {
        return moveIndex < MOVES_LEFT.length;
    }

    /**
     * Performs the next transformation to obtain the next state of this
     * fragment.
     * @param handler the computation handler
     * @return the transformation result or the canceling cause if the
     * computation was aborted by the handler.
     */
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

    /**
     * Rolls back to the state at {@code moveIndex} and unpins all unused nodes.
     * <p>
     * This function does not perform garbage collection. You can manually call
     * {@link Sdd#garbageCollectAll()} afterward, if you want to clean up the
     * unused nodes.
     * @param moveIndex the index of the state
     */
    public void rollback(final int moveIndex) {
        assert moveIndex >= appliedIndex;
        while (this.moveIndex > moveIndex) {
            this.moveIndex--;
            sdd.getVTreeStack().pop();
            transformations.remove(transformations.size() - 1);
        }
        sdd.getVTreeStack().bumpGeneration();
    }

    /**
     * Removes all old states until the current state and unpins all unused
     * nodes.
     * <p>
     * This function does not perform garbage collection. You can manually call
     * {@link Sdd#garbageCollectAll()} afterward, if you want to clean up the
     * unused nodes.
     * @return a collapsed/merged transformation result for all transformations
     * done from the start state until the current state.
     */
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

    /**
     * Returns whether this fragment is a left fragment.
     * @return whether this fragment is a left fragment
     */
    public boolean isLeft() {
        return isLeft;
    }

    /**
     * Returns the SDD container of this fragment.
     * @return the SDD container of this fragment
     */
    public Sdd getSdd() {
        return sdd;
    }

    /**
     * Returns the current state index.
     * @return the current state index
     */
    public int getMoveIndex() {
        return moveIndex;
    }

    /**
     * Returns the current vtree.
     * @return the current vtree
     */
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
