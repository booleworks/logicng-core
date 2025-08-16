package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * An interface for minimization strategies.
 */
public interface SddMinimizationStrategy {
    /**
     * Select the vtrees that should be optimized in the next local pass.
     * @param sdd the SDD container
     * @return the vtrees that should be optimized in the next local pass
     */
    Set<VTree> selectVTrees(Sdd sdd);

    /**
     * Returns whether the search should be continued.
     * @param sdd     the SDD container
     * @param newSize the new size of the SDDs
     * @return whether the search should be continued.
     */
    boolean shouldResume(Sdd sdd, long newSize);

    /**
     * Existing minimization strategies
     */
    enum Strategies {
        /**
         * Bottom-Up strategy
         */
        BOTTOM_UP,

        /**
         * Decreasing threshold strategy
         */
        DEC_THRESHOLD,

        /**
         * Windows strategy
         */
        WINDOW;

        /**
         * Get an instance of the strategy
         * @return an instance of the strategy
         */
        public SddMinimizationStrategy toInstance() {
            switch (this) {
                case BOTTOM_UP:
                    return new BottomUpSddMinimizationStragegy();
                case DEC_THRESHOLD:
                    return new DecreasingThresholdSddMinimizationStragegy();
                case WINDOW:
                    return new WindowSddMinimizationStragegy();
                default:
                    throw new RuntimeException("Unknown minimization strategy");
            }
        }
    }

    /**
     * The bottom-up strategy.
     * <p>
     * Computes always the local optimum for all vtree nodes.
     */
    final class BottomUpSddMinimizationStragegy implements SddMinimizationStrategy {
        long lastSize = -1;

        private BottomUpSddMinimizationStragegy() {
        }

        @Override
        public Set<VTree> selectVTrees(final Sdd sdd) {
            return VTreeUtil.nodesInOrder(sdd.getVTree().getRoot(), new HashSet<>());
        }

        @Override
        public boolean shouldResume(final Sdd sdd, final long newSize) {
            if (lastSize != -1 && lastSize == newSize) {
                return false;
            }
            lastSize = newSize;
            return true;
        }
    }

    /**
     * The decreasing threshold strategy.
     * <p>
     * Computes first the local optimum for vtree with 50+% of the variables,
     * then for 25+%, 12+%, ..., 0+%.
     */
    final class DecreasingThresholdSddMinimizationStragegy implements SddMinimizationStrategy {
        int threshold = -1;
        int maxSize = -1;
        long lastSize = -1;

        private DecreasingThresholdSddMinimizationStragegy() {
        }

        @Override
        public Set<VTree> selectVTrees(final Sdd sdd) {
            return selectVTreesBySize(threshold, maxSize, sdd);
        }

        @Override
        public boolean shouldResume(final Sdd sdd, final long newSize) {
            if (lastSize == -1) {
                maxSize = VTreeUtil.varCount(sdd.getVTree().getRoot());
                threshold = maxSize / 2;
            } else {
                if (newSize == lastSize && threshold <= 2) {
                    return false;
                }
                threshold /= 2;
            }
            lastSize = newSize;
            return true;
        }
    }

    /**
     * The window strategy.
     */
    final class WindowSddMinimizationStragegy implements SddMinimizationStrategy {
        int maxSize = -1;
        long lastSize = -1;
        int windowSize = 10;
        int ub = 0;
        int lb = 0;

        private WindowSddMinimizationStragegy() {
        }

        @Override
        public Set<VTree> selectVTrees(final Sdd sdd) {
            return selectVTreesBySize(lb, ub, sdd);
        }

        @Override
        public boolean shouldResume(final Sdd sdd, final long newSize) {
            if (lastSize == -1) {
                maxSize = VTreeUtil.varCount(sdd.getVTree().getRoot());
            }
            if (lb == 0) {
                if (newSize == lastSize && windowSize == maxSize) {
                    return false;
                }
                lastSize = newSize;
                windowSize = Math.min(windowSize * 2, maxSize);
                ub = maxSize;
                lb = Math.max(maxSize - windowSize, 0);
            }
            ub = Math.max(ub - windowSize, 0);
            lb = Math.max(lb - windowSize, 0);
            return true;
        }
    }

    private static Map<VTree, Set<SddNode>> groupNodesByVTree(final Sdd sdd) {
        final LinkedHashMap<VTree, Set<SddNode>> vtree2nodes = new LinkedHashMap<>();
        for (final SddNodeDecomposition node : sdd.getDecompositionNodes()) {
            final VTree vtree = sdd.vTreeOf(node);
            final Set<SddNode> nodes = vtree2nodes.computeIfAbsent(vtree, (key) -> new HashSet<>());
            nodes.add(node);
        }
        return vtree2nodes;
    }

    private static Set<VTree> selectVTreesBySize(final int lowerBound, final int upperBound, final Sdd sdd) {
        final Stack<VTree> stack = new Stack<>();
        final Set<VTree> result = new HashSet<>();
        stack.push(sdd.getVTree().getRoot());
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            final int count = VTreeUtil.varCount(current);
            if (count >= lowerBound && count <= upperBound) {
                result.add(current);
            }
            if (!current.isLeaf()) {
                stack.push(current.asInternal().getLeft());
                stack.push(current.asInternal().getRight());
            }
        }
        return result;
    }

}

