package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformationResult {
    private final Map<SddNode, SddNode> translations;
    private final VTree transformationPoint;
    private final VTreeRoot root;

    public TransformationResult(final Map<SddNode, SddNode> translations, final VTree transformationPoint,
                                final VTreeRoot root) {
        this.translations = translations;
        this.transformationPoint = transformationPoint;
        this.root = root;
    }

    public TransformationResult(final TransformationResult r) {
        this.translations = new HashMap<>(r.translations);
        this.transformationPoint = r.transformationPoint;
        this.root = r.root;
    }

    public SddNode map(SddNode node) {
        return translations.get(node);
    }

    public Map<SddNode, SddNode> getTranslations() {
        return translations;
    }

    public VTree getTransformationPoint() {
        return transformationPoint;
    }

    public VTreeRoot getRoot() {
        return root;
    }

    public static TransformationResult identity(final VTree vTree, final VTreeRoot root) {
        final Map<SddNode, SddNode> translations = new HashMap<>();
        for (final SddNode pinnedNode : root.getPinnedNodes()) {
            translations.put(pinnedNode, pinnedNode);
        }
        return new TransformationResult(translations, vTree, root);
    }

    public static TransformationResult collapse(final VTree vTree, final TransformationResult base,
                                                final TransformationResult... rs) {
        return collapse(vTree, base, List.of(rs));
    }

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
