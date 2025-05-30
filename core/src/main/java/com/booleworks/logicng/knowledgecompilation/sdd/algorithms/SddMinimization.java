package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SddMinimizationStepEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddMinimizationConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.TransformationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeFragment;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;
import com.booleworks.logicng.util.Pair;

import java.util.function.Supplier;

public class SddMinimization {

    public static LngResult<TransformationResult> minimize(final Sdd node, final SddMinimizationConfig config) {
        return minimize(node, config::operationHandler, config.iterationHandler());
    }

    public static LngResult<TransformationResult> minimize(final Sdd sdd,
                                                           final Supplier<ComputationHandler> searchHandler,
                                                           final ComputationHandler handler) {
        int size = sdd.getActiveSize();
        TransformationResult transformation = TransformationResult.identity(sdd.getVTree().getRoot(), sdd.getVTree());
        if (!handler.shouldResume(ComputationStartedEvent.SDD_MINIMIZATION)) {
            return LngResult.partial(transformation, ComputationStartedEvent.SDD_MINIMIZATION);
        }
        while (true) {
            final LngResult<TransformationResult> passRes = localSearchPass(sdd, searchHandler, handler);
            if (!passRes.isSuccess() && !passRes.isPartial()) {
                return LngResult.partial(transformation, ComputationStartedEvent.SDD_MINIMIZATION);
            }
            final TransformationResult pass = passRes.getPartialResult();
            transformation = TransformationResult.collapse(sdd.getVTree().getRoot(), transformation, pass);
            final int newSize = sdd.getActiveSize();
            sdd.garbageCollectAll();
            if (passRes.isPartial()) {
                return LngResult.partial(transformation, ComputationStartedEvent.SDD_MINIMIZATION);
            }
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
        if (!rightRes.isSuccess() && !rightRes.isPartial()) {
            return rightRes;
        }
        final TransformationResult right = rightRes.getPartialResult();
        final VTree lca = sdd.getVTree().lcaOf(left.getTransformationPoint(), right.getTransformationPoint());
        if (rightRes.isPartial()) {
            return LngResult.partial(TransformationResult.collapse(lca, left, right), rightRes.getCancelCause());
        }
        final LngResult<TransformationResult> parentRes =
                bestLocalState(new SearchState(lca, sdd), searchHandlers, handler);
        if (parentRes.isPartial() || parentRes.isSuccess()) {
            final TransformationResult parent = parentRes.getPartialResult();
            final TransformationResult collapsed =
                    TransformationResult.collapse(parent.getTransformationPoint(), left, right, parent);
            if (parentRes.isPartial()) {
                return LngResult.partial(collapsed, parentRes.getCancelCause());
            } else {
                return LngResult.of(collapsed);
            }
        } else {
            return parentRes;
        }
    }

    public static LngResult<TransformationResult> bestLocalState(final SearchState state,
                                                                 final Supplier<ComputationHandler> searchHandlers,
                                                                 final ComputationHandler handler) {
        if (state.leftLinear == null && state.rightLinear == null) {
            return LngResult.of(TransformationResult.identity(state.vTree, state.sdd.getVTree()));
        } else if (state.leftLinear == null) {
            final LngResult<Pair<Long, TransformationResult>> result
                    = bestState(state.rightLinear, searchHandlers, handler);
            if (!result.isSuccess()) {
                return result.map(Pair::getSecond);
            }
            return LngResult.of(result.getResult().getSecond());
        } else if (state.rightLinear == null) {
            final LngResult<Pair<Long, TransformationResult>> result
                    = bestState(state.leftLinear, searchHandlers, handler);
            if (!result.isSuccess()) {
                return result.map(Pair::getSecond);
            }
            return LngResult.of(result.getResult().getSecond());
        } else {
            final VTreeRoot baseRoot = new VTreeRoot(state.sdd.getVTree());
            final LngResult<Pair<Long, TransformationResult>> left =
                    bestState(state.leftLinear, searchHandlers, handler);
            if (!left.isSuccess()) {
                return left.map(Pair::getSecond);
            }
            state.sdd.getVTreeStack().push(baseRoot);
            state.sdd.getVTreeStack().bumpGeneration();
            state.sdd.getVTreeStack().invalidateOldGenerations();
            final LngResult<Pair<Long, TransformationResult>> right =
                    bestState(state.rightLinear, searchHandlers, handler);
            if (!right.isSuccess() && !right.isPartial()) {
                return LngResult.canceled(right.getCancelCause());
            }
            final Pair<Long, TransformationResult> lr = left.getResult();
            final Pair<Long, TransformationResult> rr = right.getPartialResult();
            final TransformationResult minimal;
            if (lr.getFirst() <= rr.getFirst()) {
                state.sdd.getVTreeStack().pop();
                state.sdd.getVTreeStack().bumpGeneration();
                state.sdd.getVTreeStack().invalidateOldGenerations();
                minimal = lr.getSecond();
            } else {
                state.sdd.getVTreeStack().removeInactive(1);
                minimal = rr.getSecond();
            }
            if (right.isPartial()) {
                return LngResult.partial(minimal, right.getCancelCause());
            } else {
                return LngResult.of(minimal);
            }
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
                fragment.rollback(indexOfBest);
                return LngResult.of(new Pair<>(bestSize, transOfBest));
            } else if (!result.isSuccess()) {
                fragment.rollback(indexOfBest);
                return LngResult.partial(new Pair<>(bestSize, transOfBest), result.getCancelCause());
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
