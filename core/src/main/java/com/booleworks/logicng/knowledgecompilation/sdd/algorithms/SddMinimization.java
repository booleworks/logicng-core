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

import java.util.Set;

/**
 * A class for minimizing SDDs by performing multiple levels of local searches.
 * <p>
 * There are 4 levels:
 * <ul>
 *     <li>{@link SddMinimization#minimize}: Combines multiple local passes
 *     until no improvements are detected anymore. A
 *     {@link SddMinimizationStrategy} controls the specific behaviour of this
 *     search.</li>
 *     <li>{@link SddMinimization#localSearchPass}: Traverses the vtree in
 *     post-order and computes the local optimum for a selection of vtree nodes.
 *     </li>
 *     <li>{@link SddMinimization#bestLocalState}: Computes the local optimum
 *     of a vtree node by testing all permutations of the node as a left
 *     fragment and as a right fragment.</li>
 *     <li>{@link SddMinimization#bestState}: Computes the local optimum of a
 *     vtree fragment by iterating over all permutations of it.
 *     </li>
 * </ul>
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddMinimization {
    private SddMinimization() {
    }

    /**
     * Minimizes the SDD.
     * <p>
     * This function performs a local search gradually transforming the vtree
     * to find a local optimum.
     * <p>
     * The search provides a partial results if it is aborted early.
     * @param config the minimization configuration
     * @return the transformation information of the search. Will be a partial
     * value if the search was aborted.
     */
    public static LngResult<TransformationResult> minimize(final SddMinimizationConfig config) {
        final SddMinimizationConfig configCopy = new SddMinimizationConfig(config);
        return minimize(configCopy.getSdd(), configCopy.getStrategy().toInstance(), configCopy.getSearchHandler());
    }

    /**
     * Minimizes the SDD.
     * <p>
     * This function performs a local search gradually transforming the vtree
     * to find a local optimum.
     * <p>
     * The search provides a partial results if it is aborted early.
     * @param sdd                     the SDD container
     * @param sddMinimizationStrategy the minimization strategy
     * @param handler                 the search handler
     * @return the transformation information of the search. Will be a partial
     * value if the search was aborted.
     */
    public static LngResult<TransformationResult> minimize(final Sdd sdd,
                                                           final SddMinimizationStrategy sddMinimizationStrategy,
                                                           final SearchHandler handler) {
        int size = sdd.getActiveSize();
        TransformationResult transformation = TransformationResult.identity(sdd.getVTree().getRoot(), sdd.getVTree());
        if (!handler.shouldResume(ComputationStartedEvent.SDD_MINIMIZATION)) {
            return LngResult.partial(transformation, ComputationStartedEvent.SDD_MINIMIZATION);
        }
        while (sddMinimizationStrategy.shouldResume(sdd, size)) {
            final Set<VTree> selection = sddMinimizationStrategy.selectVTrees(sdd);
            final LngResult<TransformationResult> passRes = localSearchPass(selection, sdd, handler);
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
            size = newSize;
        }
        return LngResult.of(transformation);
    }

    /**
     * Computes the local optimum for all vtree nodes in {@code selection} using
     * post-order traversal.
     * @param selection the selected vtree nodes
     * @param sdd       the SDD container
     * @param handler   the computation handler
     * @return the transformation information of the local pass. Will be a
     * partial value if a hard abort happened.
     */
    public static LngResult<TransformationResult> localSearchPass(final Set<VTree> selection, final Sdd sdd,
                                                                  final SearchHandler handler) {
        return localSearchPass(sdd.getVTree().getRoot(), selection, sdd, handler);
    }

    /**
     * Computes the local optimum for all vtree nodes in {@code selection} using
     * post-order traversal.
     * @param selection the selected vtree nodes
     * @param handler   the computation handler
     * @return the transformation information of the local pass. Will be a
     * partial value if a hard abort happened.
     */
    public static LngResult<TransformationResult> localSearchPass(final VTree vTree, final Set<VTree> selection,
                                                                  final Sdd sdd, final SearchHandler handler) {
        if (vTree.isLeaf()) {
            return LngResult.of(TransformationResult.identity(vTree, sdd.getVTree()));
        }
        final LngResult<TransformationResult> leftRes =
                localSearchPass(vTree.asInternal().getLeft(), selection, sdd, handler);
        if (!leftRes.isSuccess()) {
            return leftRes;
        }
        final TransformationResult left = leftRes.getResult();
        final LngResult<TransformationResult> rightRes =
                localSearchPass(vTree.asInternal().getRight(), selection, sdd, handler);
        if (!rightRes.isSuccess() && !rightRes.isPartial()) {
            return rightRes;
        }
        final TransformationResult right = rightRes.getPartialResult();
        final VTree lca = sdd.getVTree().lcaOf(left.getTransformationPoint(), right.getTransformationPoint());
        if (rightRes.isPartial()) {
            return LngResult.partial(TransformationResult.collapse(lca, left, right), rightRes.getCancelCause());
        }
        if (selection != null && !selection.contains(vTree)) {
            return LngResult.of(TransformationResult.collapse(lca, left, right));
        }
        final LngResult<TransformationResult> parentRes = bestLocalState(lca, sdd, handler);
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

    /**
     * Calculates the smallest variant of all vtree fragments of a vtree node
     * within the limitations of the provided handler.
     * @param vtree   the vtree node
     * @param handler the search handler
     * @return the optimal result or the best result found until a soft abort,
     * or a partial result util a hard abort.
     */
    public static LngResult<TransformationResult> bestLocalState(final VTree vtree, final Sdd sdd,
                                                                 final SearchHandler handler) {
        final boolean isLeft = VTreeUtil.isLeftFragment(vtree);
        final boolean isRight = VTreeUtil.isRightFragment(vtree);
        if (!isLeft && !isRight) {
            return LngResult.of(TransformationResult.identity(vtree, sdd.getVTree()));
        } else if (!isLeft && isRight) {
            final LngResult<Pair<Long, TransformationResult>> result
                    = bestState(new VTreeFragment(false, vtree, sdd), handler);
            if (!result.isSuccess()) {
                return result.map(Pair::getSecond);
            }
            return LngResult.of(result.getResult().getSecond());
        } else if (isLeft && !isRight) {
            final LngResult<Pair<Long, TransformationResult>> result =
                    bestState(new VTreeFragment(true, vtree, sdd), handler);
            if (!result.isSuccess()) {
                return result.map(Pair::getSecond);
            }
            return LngResult.of(result.getResult().getSecond());
        } else {
            final VTreeRoot baseRoot1 = new VTreeRoot(sdd.getVTree());
            final VTreeRoot baseRoot2 = new VTreeRoot(sdd.getVTree());
            sdd.getVTreeStack().push(baseRoot1);
            sdd.getVTreeStack().bumpGeneration();
            final LngResult<Pair<Long, TransformationResult>> left =
                    bestState(new VTreeFragment(true, vtree, sdd), handler);
            if (!left.isSuccess()) {
                if (left.isPartial()) {
                    sdd.getVTreeStack().removeInactive(1);
                }
                return left.map(Pair::getSecond);
            }
            sdd.getVTreeStack().push(baseRoot2);
            sdd.getVTreeStack().bumpGeneration();
            final LngResult<Pair<Long, TransformationResult>> right =
                    bestState(new VTreeFragment(false, vtree, sdd), handler);
            if (!right.isSuccess() && !right.isPartial()) {
                return LngResult.canceled(right.getCancelCause());
            }
            final Pair<Long, TransformationResult> lr = left.getResult();
            final Pair<Long, TransformationResult> rr = right.getPartialResult();
            final TransformationResult minimal;
            if (lr.getFirst() <= rr.getFirst()) {
                sdd.getVTreeStack().pop();
                sdd.getVTreeStack().removeInactive(1);
                sdd.getVTreeStack().bumpGeneration();
                minimal = lr.getSecond();
            } else {
                sdd.getVTreeStack().removeInactive(2);
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
     * @param fragment the vtree fragment
     * @param handler  the search handler
     * @return the optimal result or the best result found until a soft abort,
     * or a partial result util a hard abort.
     */
    public static LngResult<Pair<Long, TransformationResult>> bestState(final VTreeFragment fragment,
                                                                        final SearchHandler handler) {
        long bestSize = fragment.getSdd().getActiveSize();
        int indexOfBest = 0;
        TransformationResult transOfBest =
                TransformationResult.identity(fragment.getVTree(), fragment.getSdd().getVTree());
        while (fragment.hasNext()) {
            final LngResult<TransformationResult> result = fragment.next(handler);
            if (!result.isSuccess() && handler.abortedBySearchHandler) {
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
                if (!handler.shouldResume(event)) {
                    return LngResult.partial(new Pair<>(bestSize, transOfBest), event);
                }
            }
        }
        fragment.rollback(indexOfBest);
        return LngResult.of(new Pair<>(bestSize, transOfBest));
    }

    /**
     * A utility class for identifying the fragments of a
     */
    public static class LocalFragments {
        private final VTree vTree;
        private final VTreeFragment leftLinear;
        private final VTreeFragment rightLinear;
        private final Sdd sdd;

        public LocalFragments(final VTree vTree, final Sdd sdd) {
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
     * It composes two handlers: An {@code userHandler} and a
     * {@code searchHandler}.  The former is used to control and abort the whole
     * minimization computation (hard abort).  The latter is used to abort only
     * a specific transformation step (soft abort).  If it aborts the
     * computation only the current fragment rotation is aborted but the
     * minimization continues at another position in the vtree.  This is useful
     * for not getting stuck at only one transformation.
     * <p>
     * The {@code userHandler} is always queried before the
     * {@code searchHandler}. An abort by {@code searchHandler} is indicated
     * by the boolean {@code abortedBySearchHandler}.
     */
    public static class SearchHandler implements ComputationHandler {
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
                abortedBySearchHandler = false;
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
