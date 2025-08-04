package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores information obtained during a global transformation and can be used
 * to translate old nodes to the new nodes after the transformation.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class TransformationResult {
    private final Map<SddNode, SddNode> translations;
    private final VTree transformationPoint;
    private final VTreeRoot root;

    /**
     * Constructs a new transformation result.
     * @param translations        a mapping from old nodes to new nodes (only
     *                            pinned nodes)
     * @param transformationPoint the point within the new vtree where the
     *                            transformation happend
     * @param root                the new vtree root
     */
    public TransformationResult(final Map<SddNode, SddNode> translations, final VTree transformationPoint,
                                final VTreeRoot root) {
        this.translations = translations;
        this.transformationPoint = transformationPoint;
        this.root = root;
    }

    /**
     * Copy constructor.
     * @param r the existing transformation result
     */
    public TransformationResult(final TransformationResult r) {
        this.translations = new HashMap<>(r.translations);
        this.transformationPoint = r.transformationPoint;
        this.root = r.root;
    }

    /**
     * Translate an old SDD node to corresponding new SDD node after the
     * transformation.
     * @param node the old node (must be pinned before the transformation)
     * @return the new SDD node
     */
    public SddNode map(final SddNode node) {
        return translations.get(node);
    }

    /**
     * Returns the map of translations of old to new SDD nodes (only of the
     * pinned nodes).
     * @return return the map of translations
     */
    public Map<SddNode, SddNode> getTranslations() {
        return translations;
    }

    /**
     * Returns the point in the new vtree where the transformation happened.
     * @return the point in the new vtree where the transformation happened
     */
    public VTree getTransformationPoint() {
        return transformationPoint;
    }

    /**
     * Returns the new vtree root.
     * @return the new vtree root
     */
    public VTreeRoot getRoot() {
        return root;
    }

    /**
     * Returns a transformation result for the identity transformation.
     * @param vTree the transformation point
     * @param root  the vtree root
     * @return a transformation result for the identity transformation
     */
    public static TransformationResult identity(final VTree vTree, final VTreeRoot root) {
        final Map<SddNode, SddNode> translations = new HashMap<>();
        for (final SddNode pinnedNode : root.getPinnedNodes()) {
            translations.put(pinnedNode, pinnedNode);
        }
        return new TransformationResult(translations, vTree, root);
    }

    /**
     * Collapses/merges a sequence of transformations results into one
     * transformation result.
     * <p>
     * The translations will map from the old SDD nodes of {@code base} in the
     * list to the new SDD nodes in the last element in the list.  The caller
     * has to pass the vtree that he considers as transformation point.
     * @param vTree the transformation point
     * @param base  the start transformation result
     * @param rs    further transformation results
     * @return the collapsed transformation result
     */
    public static TransformationResult collapse(final VTree vTree, final TransformationResult base,
                                                final TransformationResult... rs) {
        return collapse(vTree, base, List.of(rs));
    }

    /**
     * Collapses/merges a sequence of transformations results into one
     * transformation result.
     * <p>
     * The translations will map from the old SDD nodes of {@code base} in the
     * list to the new SDD nodes in the last element in the list.  The caller
     * has to pass the vtree that he considers as transformation point.
     * @param vTree the transformation point
     * @param base  the start transformation result
     * @param rs    further transformation results
     * @return the collapsed transformation result
     */
    public static TransformationResult collapse(final VTree vTree, final TransformationResult base,
                                                final List<TransformationResult> rs) {
        if (rs.isEmpty()) {
            throw new IllegalArgumentException("Cannot collapse empty list of results");
        }
        Map<SddNode, SddNode> translations = base.getTranslations();
        VTreeRoot lastRoot = base.getRoot();
        for (final TransformationResult r : rs) {
            final Map<SddNode, SddNode> nextTrans = new HashMap<>();
            translations.forEach((k, v) -> nextTrans.put(k, r.getTranslations().getOrDefault(v, v)));
            translations = nextTrans;
            lastRoot = r.getRoot();
        }
        return new TransformationResult(translations, vTree, lastRoot);
    }
}
