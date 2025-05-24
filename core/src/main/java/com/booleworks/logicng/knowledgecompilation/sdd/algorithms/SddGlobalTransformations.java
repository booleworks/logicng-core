package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SddGlobalTransformations {
    private final Sdd sdd;
    private final Map<SddNode, Action> plan;
    private final Map<SddElement, PartitionAction> partitionPlan;
    private final Map<SddNode, SddNode> cache;
    private final Map<SddNode, SddNode> translations;

    private SddGlobalTransformations(final Sdd sdd) {
        this.sdd = sdd;
        plan = new HashMap<>();
        partitionPlan = new HashMap<>();
        cache = new HashMap<>();
        translations = new HashMap<>();
    }

    public static LngResult<Map<SddNode, SddNode>> rotateRight(final VTreeInternal rotationPoint, final Sdd sdd,
                                                               final ComputationHandler handler) {
        return new SddGlobalTransformations(sdd).rotateRight(rotationPoint, handler);
    }

    public static LngResult<Map<SddNode, SddNode>> rotateLeft(final VTreeInternal rotationPoint, final Sdd sdd,
                                                              final ComputationHandler handler) {
        return new SddGlobalTransformations(sdd).rotateLeft(rotationPoint, handler);
    }

    public static LngResult<Map<SddNode, SddNode>> swap(final VTreeInternal swapPoint, final Sdd sdd,
                                                        final ComputationHandler handler) {
        return new SddGlobalTransformations(sdd).swap(swapPoint, handler);
    }

    private LngResult<Map<SddNode, SddNode>> rotateRight(final VTreeInternal rotationPoint,
                                                         final ComputationHandler handler) {
        if (!VTreeUtil.isLeftFragment(rotationPoint)) {
            throw new IllegalArgumentException("Expected left linear vtree fragment for right rotation");
        }
        final VTreeInternal parentInner = rotationPoint.asInternal();
        final VTreeInternal leftInner = parentInner.getLeft().asInternal();
        final List<SddNode> pinnedNodes = sdd.getVTree().getPinnedNodes();
        precomputePlanRot(pinnedNodes, parentInner, leftInner, Action.PARTITION_RR);
        final VTree rotatedVTreeNode = VTreeRotate.rotateRight(parentInner, sdd);
        final VTree rotatedVTree =
                VTreeUtil.substituteNode(sdd.getVTree().getRoot(), rotationPoint, rotatedVTreeNode, sdd);
        final VTreeRoot rotatedRoot = sdd.constructRoot(rotatedVTree);
        sdd.getVTreeStack().push(rotatedRoot);
        final LngEvent event = executePlan(pinnedNodes, handler);
        if (event != null) {
            return LngResult.canceled(event);
        }
        return LngResult.of(translations);
    }

    private LngResult<Map<SddNode, SddNode>> rotateLeft(final VTreeInternal rotationPoint,
                                                        final ComputationHandler handler) {
        if (!VTreeUtil.isRightFragment(rotationPoint)) {
            throw new IllegalArgumentException("Expected left linear vtree fragment for right rotation");
        }
        final VTreeInternal parentInner = rotationPoint.asInternal();
        final VTreeInternal rightInner = parentInner.getRight().asInternal();
        final List<SddNode> pinnedNodes = sdd.getVTree().getPinnedNodes();
        precomputePlanRot(pinnedNodes, parentInner, rightInner, Action.PARTITION_LR);
        final VTree rotatedVTreeNode = VTreeRotate.rotateLeft(parentInner, sdd);
        final VTree rotatedVTree =
                VTreeUtil.substituteNode(sdd.getVTree().getRoot(), rotationPoint, rotatedVTreeNode, sdd);
        final VTreeRoot rotatedRoot = sdd.constructRoot(rotatedVTree);
        sdd.getVTreeStack().push(rotatedRoot);
        final LngEvent event = executePlan(pinnedNodes, handler);
        if (event != null) {
            return LngResult.canceled(event);
        }
        return LngResult.of(translations);
    }

    private LngResult<Map<SddNode, SddNode>> swap(final VTreeInternal swapPoint, final ComputationHandler handler) {
        final List<SddNode> pinnedNodes = sdd.getVTree().getPinnedNodes();
        precomputePlanSwap(pinnedNodes, swapPoint);
        final VTree swappedVTreeNode = VTreeSwap.swapChildren(swapPoint, sdd);
        final VTree swappedVTree =
                VTreeUtil.substituteNode(sdd.getVTree().getRoot(), swapPoint, swappedVTreeNode, sdd);
        final VTreeRoot swappedRoot = sdd.constructRoot(swappedVTree);
        sdd.getVTreeStack().push(swappedRoot);
        final LngEvent event = executePlan(pinnedNodes, handler);
        if (event != null) {
            return LngResult.canceled(event);
        }
        return LngResult.of(translations);
    }

    private LngEvent executePlan(final Collection<SddNode> nodes, final ComputationHandler handler) {
        for (final SddNode pinnedNode : nodes) {
            final LngResult<SddNode> r = executePlan(pinnedNode, handler);
            if (!r.isSuccess()) {
                return r.getCancelCause();
            }
            sdd.pin(r.getResult());
            translations.put(pinnedNode, r.getResult());
        }
        return null;
    }

    private LngResult<SddNode> executePlan(final SddNode node, final ComputationHandler handler) {
        final Action action = plan.get(node);
        if (action == null) {
            return LngResult.of(node);
        }
        final SddNode cached = cache.get(node);
        if (cached != null) {
            return LngResult.of(cached);
        }
        switch (action) {
            case PARTITION_RR: {
                final ArrayList<TreeSet<SddElement>> sets = new ArrayList<>();
                for (final SddElement outer : node.asDecomposition().getElements()) {
                    final TreeSet<SddElement> newElements = new TreeSet<>();
                    executeRepartitioning(outer, sdd, newElements);
                    sets.add(newElements);
                }
                final LngResult<TreeSet<SddElement>> rotatedElements =
                        SddCartesianProduct.cartesianProduct(sets, true, sdd, handler);
                if (!rotatedElements.isSuccess()) {
                    return LngResult.canceled(rotatedElements.getCancelCause());
                }
                final LngResult<SddNode> rotatedNode =
                        Util.getNodeOfPartition(rotatedElements.getResult(), sdd, handler);
                if (!rotatedNode.isSuccess()) {
                    return rotatedNode;
                }
                cache.put(node, rotatedNode.getResult());
                return LngResult.of(rotatedNode.getResult());
            }
            case PARTITION_LR: {
                final TreeSet<SddElement> newElements = new TreeSet<>();
                for (final SddElement outer : node.asDecomposition().getElements()) {
                    executeRepartitioning(outer, sdd, newElements);
                }
                return Util.getNodeOfPartition(newElements, sdd, handler);
            }
            case PARTITION_SW: {
                final ArrayList<TreeSet<SddElement>> sets = new ArrayList<>();
                for (final SddElement outer : node.asDecomposition().getElements()) {
                    final TreeSet<SddElement> newElements = new TreeSet<>();
                    executeRepartitioning(outer, sdd, newElements);
                    sets.add(newElements);
                }
                final LngResult<TreeSet<SddElement>> swappedElements =
                        SddCartesianProduct.cartesianProduct(sets, false, sdd, handler);
                if (!swappedElements.isSuccess()) {
                    return LngResult.canceled(swappedElements.getCancelCause());
                }
                final SddNode swappedNode = sdd.decomposition(swappedElements.getResult());
                cache.put(node, swappedNode);
                return LngResult.of(swappedNode);
            }
            case DESCENT_PRIME: {
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final LngResult<SddNode> transformedPrimeRes = executePlan(element.getPrime(), handler);
                    if (!transformedPrimeRes.isSuccess()) {
                        return transformedPrimeRes;
                    }
                    final SddNode transformedPrime = transformedPrimeRes.getResult();
                    if (transformedPrime == element.getPrime()) {
                        elements.add(element);
                    } else {
                        elements.add(new SddElement(transformedPrime, element.getSub()));
                    }
                }
                final SddNode newNode = sdd.decomposition(elements);
                cache.put(node, newNode);
                return LngResult.of(newNode);
            }
            case DESCENT_SUB: {
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final LngResult<SddNode> transformedSubRes = executePlan(element.getSub(), handler);
                    if (!transformedSubRes.isSuccess()) {
                        return transformedSubRes;
                    }
                    final SddNode transformedSub = transformedSubRes.getResult();
                    if (transformedSub == element.getSub()) {
                        elements.add(element);
                    } else {
                        elements.add(new SddElement(element.getPrime(), transformedSub));
                    }
                }
                final SddNode newNode = sdd.decomposition(elements);
                cache.put(node, newNode);
                return LngResult.of(newNode);
            }
            case REREGISTER: {
                final SddNode updated = reregister(node.asDecomposition());
                return LngResult.of(updated);
            }
            default:
                throw new IllegalArgumentException("Unsupported Action at this stage");
        }
    }

    private void executeRepartitioning(final SddElement outer, final Sdd sdd, final Set<SddElement> dst) {
        final PartitionAction action = partitionPlan.get(outer);
        assert action != null;
        switch (action) {
            case RR_abC_aBC: {
                for (final SddElement inner : outer.getPrime().asDecomposition().getElements()) {
                    final SddNode bc = sdd.conjunctionUnsafe(inner.getSub(), outer.getSub());
                    dst.add(new SddElement(inner.getPrime(), bc));
                }
                break;
            }
            case RR_bC_BC: {
                final SddNode a = sdd.verum();
                final SddNode bc = sdd.conjunctionUnsafe(outer.getPrime(), outer.getSub());
                dst.add(new SddElement(a, bc));
                break;
            }
            case RR_aC_aC: {
                final SddNode a = outer.getPrime();
                final SddNode bc = outer.getSub();
                dst.add(new SddElement(a, bc));
                final SddNode aNeg = sdd.negate(a);
                dst.add(new SddElement(aNeg, sdd.falsum()));
                break;
            }
            case LR_aBC_abC: {
                for (final SddElement inner : outer.getSub().asDecomposition().getElements()) {
                    final SddNode ab = sdd.conjunctionUnsafe(outer.getPrime(), inner.getPrime());
                    dst.add(new SddElement(ab, inner.getSub()));
                }
                break;
            }
            case LR_a_a:
            case LR_aC_aC: {
                dst.add(new SddElement(reregister(outer.getPrime()), reregister(outer.getSub())));
                break;
            }
            case LR_aB_ab: {
                final SddNode ab = sdd.conjunctionUnsafe(outer.getPrime(), outer.getSub());
                dst.add(new SddElement(ab, sdd.verum()));
                final SddNode bNeg = sdd.negate(outer.getSub());
                final SddNode abNeg = sdd.conjunctionUnsafe(outer.getPrime(), bNeg);
                dst.add(new SddElement(abNeg, sdd.falsum()));
                break;
            }
            case SWAP: {
                final SddNode negSub = sdd.negate(outer.getSub());
                if (!outer.getSub().isFalse()) {
                    //Don't need to reregister the nodes because the vtree is still the same respectively
                    dst.add(new SddElement(outer.getSub(), outer.getPrime()));
                }
                if (!negSub.isFalse()) {
                    dst.add(new SddElement(negSub, sdd.falsum()));
                }
                break;
            }
        }
    }

    private enum Action {
        DESCENT_PRIME,
        DESCENT_SUB,
        PARTITION_RR,
        PARTITION_LR,
        PARTITION_SW,
        REREGISTER
    }

    private enum PartitionAction {
        RR_abC_aBC,
        RR_bC_BC,
        RR_aC_aC,
        LR_a_a,
        LR_aBC_abC,
        LR_aC_aC,
        LR_aB_ab,
        SWAP
    }

    private void precomputePlanRot(final Collection<SddNode> nodes, final VTreeInternal parentInner,
                                   final VTreeInternal leftInner, final Action partitionType) {
        for (final SddNode pinnedNode : nodes) {
            precomputePlanRot(pinnedNode, parentInner, leftInner, partitionType);
        }
    }

    private void precomputePlanRot(final SddNode node, final VTreeInternal parentInner, final VTreeInternal childInner,
                                   final Action partitionType) {
        if (node.isDecomposition()) {
            final Action action = plan.get(node);
            if (action != null) {
                return;
            }
            final VTreeRoot vtr = sdd.getVTree();
            final VTree vt = node.getVTree();
            if (vt == parentInner) {
                plan.put(node, partitionType);
                for (final SddElement element : node.asDecomposition().getElements()) {
                    switch (partitionType) {
                        case PARTITION_LR:
                            precomputePartitionPlanLR(element, childInner);
                            break;
                        case PARTITION_RR:
                            precomputePartitionPlanRR(element, childInner);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid partition type: " + partitionType);
                    }
                    precomputePartitionPlanRR(element, childInner);
                }
            } else if (vt == childInner) {
                plan.put(node, Action.REREGISTER);
            } else if (vtr.isSubtree(vt, parentInner)) {
                // Do nothing
            } else {
                final boolean moveInPrime = vtr.isSubtree(parentInner, vt.asInternal().getLeft());
                final boolean moveInSub = vtr.isSubtree(parentInner, vt.asInternal().getRight());
                if (moveInPrime) {
                    plan.put(node, Action.DESCENT_PRIME);
                } else if (moveInSub) {
                    plan.put(node, Action.DESCENT_SUB);
                }
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final SddNode prime = element.getPrime();
                    final SddNode sub = element.getSub();
                    if (moveInPrime && !prime.isTrivial() && vtr.isSubtree(childInner, prime.getVTree())) {
                        precomputePlanRot(prime, parentInner, childInner, partitionType);
                    }
                    if (moveInSub && !sub.isTrivial() && vtr.isSubtree(childInner, sub.getVTree())) {
                        precomputePlanRot(sub, parentInner, childInner, partitionType);
                    }
                }
            }
        }
    }

    private void precomputePartitionPlanRR(final SddElement element, final VTreeInternal leftInner) {
        final PartitionAction action = partitionPlan.get(element);
        if (action != null) {
            return;
        }
        if (element.getPrime().getVTree() == leftInner) {
            partitionPlan.put(element, PartitionAction.RR_abC_aBC);
        } else if (sdd.getVTree().isSubtree(element.getPrime().getVTree(), leftInner.getRight())) {
            partitionPlan.put(element, PartitionAction.RR_bC_BC);
        } else {
            partitionPlan.put(element, PartitionAction.RR_aC_aC);
        }
    }

    private void precomputePartitionPlanLR(final SddElement element, final VTreeInternal rightInner) {
        final PartitionAction action = partitionPlan.get(element);
        if (action != null) {
            return;
        }
        if (element.getSub().isTrivial()) {
            partitionPlan.put(element, PartitionAction.LR_a_a);
        } else if (element.getSub().getVTree() == rightInner) {
            partitionPlan.put(element, PartitionAction.LR_aBC_abC);
        } else if (sdd.getVTree().getPosition(element.getSub().getVTree()) > sdd.getVTree().getPosition(rightInner)) {
            partitionPlan.put(element, PartitionAction.LR_aC_aC);
        } else {
            partitionPlan.put(element, PartitionAction.LR_aB_ab);
        }
    }

    private void precomputePlanSwap(final Collection<SddNode> nodes, final VTreeInternal swapPoint) {
        for (final SddNode pinnedNode : nodes) {
            precomputePlanSwap(pinnedNode, swapPoint);
        }
    }

    private void precomputePlanSwap(final SddNode node, final VTreeInternal swapPoint) {
        if (node.isDecomposition()) {
            final Action action = plan.get(node);
            if (action != null) {
                return;
            }
            final VTreeRoot vtr = sdd.getVTree();
            final VTree vt = node.getVTree();
            if (vt == swapPoint) {
                plan.put(node, Action.PARTITION_SW);
                for (final SddElement element : node.asDecomposition().getElements()) {
                    partitionPlan.put(element, PartitionAction.SWAP);
                }
            } else {
                final boolean moveInPrime = vtr.isSubtree(swapPoint, vt.asInternal().getLeft());
                final boolean moveInSub = vtr.isSubtree(swapPoint, vt.asInternal().getRight());
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final SddNode prime = element.getPrime();
                    final SddNode sub = element.getSub();
                    if (moveInPrime) {
                        plan.put(node, Action.DESCENT_PRIME);
                    } else if (moveInSub) {
                        plan.put(node, Action.DESCENT_SUB);
                    }
                    if (moveInPrime && !prime.isTrivial() && vtr.isSubtree(swapPoint, prime.getVTree())) {
                        precomputePlanSwap(element.getPrime(), swapPoint);
                    }
                    if (moveInSub && !sub.isTrivial() && vtr.isSubtree(swapPoint, sub.getVTree())) {
                        precomputePlanSwap(element.getSub(), swapPoint);
                    }
                }
            }
        }
    }

    private SddNode reregister(final SddNode node) {
        if (node.isDecomposition()) {
            return sdd.decomposition(node.asDecomposition().getElements());
        } else {
            return node;
        }
    }
}
