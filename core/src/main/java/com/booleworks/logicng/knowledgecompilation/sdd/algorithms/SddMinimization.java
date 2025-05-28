package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SddMinimizationStepEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.TransformationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeFragment;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;
import com.booleworks.logicng.util.Pair;

import java.util.function.Supplier;

public class SddMinimization {

    public static LngResult<TransformationResult> minimize(final Sdd sdd,
                                                           final Supplier<ComputationHandler> searchHandler,
                                                           final ComputationHandler handler) {
        int size = sdd.getActiveSize();
        if (!handler.shouldResume(ComputationStartedEvent.SDD_MINIMIZATION)) {
            return LngResult.canceled(ComputationStartedEvent.SDD_MINIMIZATION);
        }
        TransformationResult transformation = TransformationResult.identity(sdd.getVTree().getRoot(), sdd.getVTree());
        while (true) {
            final LngResult<TransformationResult> passRes = localSearchPass(sdd, searchHandler, handler);
            if (!passRes.isSuccess()) {
                return passRes;
            }
            final TransformationResult pass = passRes.getResult();
            transformation = TransformationResult.collapse(sdd.getVTree().getRoot(), transformation, pass);
            final int newSize = sdd.getActiveSize();
            sdd.garbageCollectAll();
            final SddMinimizationStepEvent event = new SddMinimizationStepEvent(newSize);
            if (!handler.shouldResume(event)) {
                return LngResult.partial(transformation, event);
            }
            if (newSize == size) {
                break;
            }
            size = newSize;
        }
        return LngResult.of(transformation);
    }

    public static LngResult<TransformationResult> localSearchPass(final Sdd sdd,
                                                                  final Supplier<ComputationHandler> searchHandlers,
                                                                  final ComputationHandler handler) {
        return localSearchPass(sdd.getVTree().getRoot(), sdd, searchHandlers, handler);
    }

    private static LngResult<TransformationResult> localSearchPass(final VTree vTree, final Sdd sdd,
                                                                   final Supplier<ComputationHandler> searchHandlers,
                                                                   final ComputationHandler handler) {
        if (vTree.isLeaf()) {
            return LngResult.of(TransformationResult.identity(vTree, sdd.getVTree()));
        }
        final LngResult<TransformationResult> leftRes =
                localSearchPass(vTree.asInternal().getLeft(), sdd, searchHandlers, handler);
        if (!leftRes.isSuccess()) {
            return leftRes;
        }
        final TransformationResult left = leftRes.getResult();
        final LngResult<TransformationResult> rightRes =
                localSearchPass(vTree.asInternal().getRight(), sdd, searchHandlers, handler);
        if (!rightRes.isSuccess()) {
            return rightRes;
        }
        final TransformationResult right = rightRes.getResult();
        final VTree lca = sdd.getVTree().lcaOf(left.getTransformationPoint(), right.getTransformationPoint());
        final LngResult<TransformationResult> parentRes =
                bestLocalState(new SearchState(lca, sdd), searchHandlers, handler);
        if (!parentRes.isSuccess()) {
            return parentRes;
        }
        final TransformationResult parent = parentRes.getResult();
        final TransformationResult collapsed =
                TransformationResult.collapse(parent.getTransformationPoint(), left, right, parent);
        return LngResult.of(collapsed);
    }

    public static LngResult<TransformationResult> bestLocalState(final SearchState state,
                                                                 final Supplier<ComputationHandler> searchHandlers,
                                                                 final ComputationHandler handler) {
        if (state.leftLinear == null && state.rightLinear == null) {
            return LngResult.of(TransformationResult.identity(state.vTree, state.sdd.getVTree()));
        } else if (state.leftLinear == null) {
            final LngResult<Pair<Long, TransformationResult>> result
                    = bestState(state.rightLinear, searchHandlers, handler);
            if (!result.isSuccess() && !result.isPartial()) {
                return LngResult.canceled(result.getCancelCause());
            }
            return LngResult.of(getAnyResult(result).getSecond());
        } else if (state.rightLinear == null) {
            final LngResult<Pair<Long, TransformationResult>> result
                    = bestState(state.leftLinear, searchHandlers, handler);
            if (!result.isSuccess() && !result.isPartial()) {
                return LngResult.canceled(result.getCancelCause());
            }
            return LngResult.of(getAnyResult(result).getSecond());
        } else {
            final VTreeRoot baseRoot = new VTreeRoot(state.sdd.getVTree());
            final LngResult<Pair<Long, TransformationResult>> left
                    = bestState(state.leftLinear, searchHandlers, handler);
            if (!left.isSuccess() && !left.isPartial()) {
                return LngResult.canceled(left.getCancelCause());
            }
            state.sdd.getVTreeStack().push(baseRoot);
            state.sdd.recalculateVTrees();
            final LngResult<Pair<Long, TransformationResult>> right
                    = bestState(state.rightLinear, searchHandlers, handler);
            if (!right.isSuccess() && !right.isPartial()) {
                return LngResult.canceled(right.getCancelCause());
            }
            final Pair<Long, TransformationResult> lr = getAnyResult(left);
            final Pair<Long, TransformationResult> rr = getAnyResult(right);
            if (lr.getFirst() <= rr.getFirst()) {
                state.sdd.getVTreeStack().pop();
                state.sdd.recalculateVTrees();
                return LngResult.of(lr.getSecond());
            } else {
                state.sdd.getVTreeStack().removeInactive(1);
                return LngResult.of(rr.getSecond());
            }
        }
    }

    private static <R> R getAnyResult(final LngResult<R> result) {
        if (result.isPartial()) {
            return result.getPartialResult();
        } else {
            return result.getResult();
        }
    }

    public static LngResult<Pair<Long, TransformationResult>> bestState(final VTreeFragment fragment,
                                                                        final Supplier<ComputationHandler> searchHandlers,
                                                                        final ComputationHandler userHandler) {
        long bestSize = fragment.getSddSize();
        int indexOfBest = 0;
        TransformationResult transOfBest =
                TransformationResult.identity(fragment.getVTree(), fragment.getSdd().getVTree());
        while (fragment.hasNext()) {
            final SearchHandler handler = new SearchHandler(userHandler, searchHandlers.get());
            final LngResult<TransformationResult> result = fragment.next(handler);
            if (handler.abortedBySearchHandler) {
                return LngResult.partial(new Pair<>(bestSize, transOfBest), result.getCancelCause());
            } else if (!result.isSuccess()) {
                return LngResult.canceled(result.getCancelCause());
            }
            if (fragment.getSddSize() <= bestSize) {
                bestSize = fragment.getSddSize();
                indexOfBest = fragment.getMoveIndex();
                transOfBest = fragment.apply();
            }
        }
        fragment.rollback(indexOfBest);
        return LngResult.of(new Pair<>(bestSize, transOfBest));
    }

    public static class SearchState {
        private final VTree vTree;
        private final VTreeFragment leftLinear;
        private final VTreeFragment rightLinear;
        private final Sdd sdd;

        public SearchState(final VTree vTree, final Sdd sdd) {
            this.sdd = sdd;
            this.vTree = vTree;
            if (VTreeUtil.isLeftFragment(vTree)) {
                leftLinear = new VTreeFragment(true, vTree, sdd);
            } else {
                leftLinear = null;
            }
            if (VTreeUtil.isRightFragment(vTree)) {
                rightLinear = new VTreeFragment(false, vTree, sdd);
            } else {
                rightLinear = null;
            }
        }
    }

    private static class SearchHandler implements ComputationHandler {
        private final ComputationHandler userHandler;
        private final ComputationHandler searchHandler;
        private boolean abortedBySearchHandler = false;

        public SearchHandler(final ComputationHandler userHandler, final ComputationHandler searchHandler) {
            this.userHandler = userHandler;
            this.searchHandler = searchHandler;
        }

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (!userHandler.shouldResume(event)) {
                return false;
            }
            if (!searchHandler.shouldResume(event)) {
                abortedBySearchHandler = true;
                return false;
            }
            return true;
        }
    }
}
