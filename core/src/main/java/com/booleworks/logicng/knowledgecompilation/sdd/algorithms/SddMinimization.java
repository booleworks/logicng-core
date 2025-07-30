package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SddMinimizationEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddMinimizationConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.TransformationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeFragment;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.function.Supplier;

public final class SddMinimization {
    private SddMinimization() {
    }

    public static LngResult<TransformationResult> minimize(final SddMinimizationConfig config) {
        switch (config.getAlgorithm()) {
            case BOTTOM_UP:
                return minimize(config.getSdd(), config::operationHandler, config.iterationHandler());
            case DEC_THRESHOLD:
                return minimizeDecrementalSizeThreshold(config.getSdd(), config::operationHandler,
                        config.iterationHandler());
        }
        throw new RuntimeException("Unreachable");
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
            final LngResult<TransformationResult> passRes = localSearchPass(0, sdd, searchHandler, handler);
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
            final SddMinimizationEvent event = new SddMinimizationEvent(newSize, false);
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

    public static LngResult<TransformationResult> minimizeDecrementalSizeThreshold(final Sdd sdd,
                                                                                   final Supplier<ComputationHandler> searchHandler,
                                                                                   final ComputationHandler handler) {
        int size = sdd.getActiveSize();
        int threshold = VTreeUtil.varCount(sdd.getVTree().getRoot()) / 2;
        TransformationResult transformation = TransformationResult.identity(sdd.getVTree().getRoot(), sdd.getVTree());
        if (!handler.shouldResume(ComputationStartedEvent.SDD_MINIMIZATION)) {
            return LngResult.partial(transformation, ComputationStartedEvent.SDD_MINIMIZATION);
        }
        while (true) {
            final LngResult<TransformationResult> passRes = localSearchPass(threshold, sdd, searchHandler, handler);
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
            final SddMinimizationEvent event = new SddMinimizationEvent(newSize, false);
            if (!handler.shouldResume(event)) {
                return LngResult.partial(transformation, event);
            }
            if (newSize == size && threshold <= 2) {
                break;
            }
            threshold /= 2;
            size = newSize;
        }
        return LngResult.of(transformation);
    }

    public static LngResult<TransformationResult> localSearchPass(final int threshold, final Sdd sdd,
                                                                  final Supplier<ComputationHandler> searchHandlers,
                                                                  final ComputationHandler handler) {
        return localSearchPass(sdd.getVTree().getRoot(), threshold, sdd, searchHandlers, handler);
    }

    private static LngResult<TransformationResult> localSearchPass(final VTree vTree, final int threshold,
                                                                   final Sdd sdd,
                                                                   final Supplier<ComputationHandler> searchHandlers,
                                                                   final ComputationHandler handler) {
        if (vTree.isLeaf() || VTreeUtil.varCount(vTree) < threshold) {
            return LngResult.of(TransformationResult.identity(vTree, sdd.getVTree()));
        }
        final LngResult<TransformationResult> leftRes =
                localSearchPass(vTree.asInternal().getLeft(), threshold, sdd, searchHandlers, handler);
        if (!leftRes.isSuccess()) {
            return leftRes;
        }
        final TransformationResult left = leftRes.getResult();
        final LngResult<TransformationResult> rightRes =
                localSearchPass(vTree.asInternal().getRight(), threshold, sdd, searchHandlers, handler);
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

    private static LngResult<TransformationResult> reverseSearchPass(final Sdd sdd,
                                                                     final Supplier<ComputationHandler> searchHandlers,
                                                                     final ComputationHandler handler) {
        return reverseSearchPass(sdd.getVTree().getRoot(), sdd, searchHandlers, handler);
    }

    private static LngResult<TransformationResult> reverseSearchPass(final VTree vTree, final Sdd sdd,
                                                                     final Supplier<ComputationHandler> searchHandlers,
                                                                     final ComputationHandler handler) {
        if (vTree.isLeaf()) {
            return LngResult.of(TransformationResult.identity(vTree, sdd.getVTree()));
        }
        final LngResult<TransformationResult> currentRes =
                bestLocalState(new SearchState(vTree, sdd), searchHandlers, handler);
        if (!currentRes.isSuccess()) {
            return currentRes;
        }
        final TransformationResult current = currentRes.getResult();
        if (current.getTransformationPoint().isLeaf()) {
            return currentRes;
        }

        final LngResult<TransformationResult> leftRes =
                reverseSearchPass(current.getTransformationPoint().asInternal().getLeft(), sdd, searchHandlers,
                        handler);
        if (!leftRes.isSuccess() && !leftRes.isPartial()) {
            return leftRes;
        }
        final TransformationResult left = leftRes.getResult();
        if (leftRes.isPartial()) {
            return LngResult.partial(
                    TransformationResult.collapse(left.getTransformationPoint().getParent(), current, left),
                    leftRes.getCancelCause());
        }

        final LngResult<TransformationResult> rightRes =
                reverseSearchPass(current.getTransformationPoint().asInternal().getRight(), sdd,
                        searchHandlers,
                        handler);
        if (!rightRes.isSuccess() && !rightRes.isPartial()) {
            return rightRes;
        }
        final TransformationResult right = leftRes.getResult();
        final VTree lca = sdd.getVTree().lcaOf(left.getTransformationPoint(), right.getTransformationPoint());
        final TransformationResult result = TransformationResult.collapse(lca, current, right);
        if (rightRes.isPartial()) {
            return LngResult.partial(result, rightRes.getCancelCause());
        }
        return LngResult.of(result);
    }

    /**
     * Calculates the smallest variant of all vtree fragments of a vtree node
     * within the limitations of the provided handlers.
     * @param state          the vtree node as a search state
     * @param searchHandlers handlers that are used for vtree fragment
     *                       transformations, respectively.
     * @param handler        that is used to control the minimization as a whole.
     * @return a result value with the optimal result or the best result found
     * until a {@code searchHandler} aborted a transformation, or a partial
     * result if the search was aborted by {@code handler}.
     */
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

    /**
     * Calculates the smallest variant of a vtree fragment within the
     * limitations of the provided handlers.
     * @param fragment       the vtree fragment
     * @param searchHandlers handlers that are used for vtree fragment
     *                       transformations, respectively.
     * @param userHandler    handler that is used to control the minimization as
     *                       a whole.
     * @return a result value with the optimal result or the best result found
     * until a {@code searchHandler} aborted a transformation, or a partial
     * result if the search was aborted by {@code userHandler}.
     */
    public static LngResult<Pair<Long, TransformationResult>> bestState(final VTreeFragment fragment,
                                                                        final Supplier<ComputationHandler> searchHandlers,
                                                                        final ComputationHandler userHandler) {
        long bestSize = fragment.getSdd().getActiveSize();
        int indexOfBest = 0;
        TransformationResult transOfBest =
                TransformationResult.identity(fragment.getVTree(), fragment.getSdd().getVTree());
        while (fragment.hasNext()) {
            final SearchHandler handler = new SearchHandler(userHandler, searchHandlers.get());
            final LngResult<TransformationResult> result = fragment.next(handler);
            if (handler.abortedBySearchHandler) {
                fragment.rollback(indexOfBest);
                fragment.getSdd().garbageCollectAll();
                return LngResult.of(new Pair<>(bestSize, transOfBest));
            } else if (!result.isSuccess()) {
                fragment.rollback(indexOfBest);
                fragment.getSdd().garbageCollectAll();
                return LngResult.partial(new Pair<>(bestSize, transOfBest), result.getCancelCause());
            }
            final int activeSize = fragment.getSdd().getActiveSize();
            if (activeSize <= bestSize) {
                bestSize = activeSize;
                indexOfBest = fragment.getMoveIndex();
                transOfBest = fragment.apply();
                fragment.getSdd().garbageCollectAll();
                final SddMinimizationEvent event = new SddMinimizationEvent(bestSize, true);
                if (!userHandler.shouldResume(event)) {
                    return LngResult.partial(new Pair<>(bestSize, transOfBest), event);
                }
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

    /**
     * SearchHandler is computation handler used for a transformation step of a
     * vtree fragment.
     * <p>
     * It composes two handlers: An {@code userHandler} and a {@code searchHandler}.
     * The former is used to control and abort the whole minimization computation.
     * The latter is used to abort only a specific transformation step. If it
     * aborts the computation only the current fragment rotation is aborted but
     * the minimization continues at another position in the vtree. This is
     * useful for not getting stuck at only one transformation.
     * <p>
     * The {@code userHandler} is always queried before the {@code searchHandler}. An abortion by {@code searchHandler}
     * is indicated by the boolean {@code abortedBySearchHandler}.
     */
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
