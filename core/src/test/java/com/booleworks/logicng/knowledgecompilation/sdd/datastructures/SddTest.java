/*
 * // SPDX-License-Identifier: Apache-2.0 and MIT
 * // Copyright 2015-2023 Christoph Zengler
 * // Copyright 2023-20xx BooleWorks GmbH
 */

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddNodesFunction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SddTest {
    @Test
    public void testInitialization() {
        final FormulaFactory f = FormulaFactory.caching();
        final VTreeRoot emptyTreeRoot = VTreeRoot.builder().build(null);
        final VTreeRoot simpleTreeRoot = buildSimpleTree(f);
        final Sdd sdd1 = new Sdd(f, emptyTreeRoot);
        final Sdd sdd2 = new Sdd(f, simpleTreeRoot);
        assertThat(sdd1.getVTree()).isEqualTo(emptyTreeRoot);
        assertThat(sdd2.getVTree()).isEqualTo(simpleTreeRoot);
        assertThat(sdd1.getFactory()).isEqualTo(f);
        assertThat(sdd2.getFactory()).isEqualTo(f);
        assertThat(sdd1.hasVTree()).isFalse();
        assertThat(sdd2.hasVTree()).isTrue();
    }

    @Test
    public void testNodeConstruction() {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = buildSimpleSdd(f);
        final SddNodeTerminal verum = sdd.verum();
        final SddNodeTerminal falsum = sdd.falsum();
        assertThat(verum.getPhase()).isTrue();
        assertThat(verum.isTrue()).isTrue();
        assertThat(verum.isFalse()).isFalse();
        assertThat(verum.isLiteral()).isFalse();
        assertThat(verum.isDecomposition()).isFalse();
        assertThat(verum.getVTree()).isNull();
        assertThat(falsum.getPhase()).isFalse();
        assertThat(falsum.isTrue()).isFalse();
        assertThat(falsum.isFalse()).isTrue();
        assertThat(falsum.isLiteral()).isFalse();
        assertThat(falsum.isDecomposition()).isFalse();
        assertThat(falsum.getVTree()).isNull();

        final Variable v1 = f.variable("v1");
        final SddNodeTerminal term1 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v1), true);
        final SddNodeTerminal term1Neg = sdd.terminal(sdd.getVTree().getVTreeLeaf(v1), false);
        assertThat(term1.isLiteral()).isTrue();
        assertThat(term1.isTrue()).isFalse();
        assertThat(term1.isFalse()).isFalse();
        assertThat(term1.isDecomposition()).isFalse();
        assertThat(term1.getPhase()).isTrue();
        assertThat(term1.getVTree()).isEqualTo(sdd.getVTree().getVTreeLeaf(v1));
        assertThat(term1Neg.isLiteral()).isTrue();
        assertThat(term1Neg.isTrue()).isFalse();
        assertThat(term1Neg.isFalse()).isFalse();
        assertThat(term1Neg.isDecomposition()).isFalse();
        assertThat(term1Neg.getVTree()).isEqualTo(sdd.getVTree().getVTreeLeaf(v1));

        final Variable v2 = f.variable("v2");
        final SddNodeTerminal term2 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v2), true);
        final SddElement element1 = new SddElement(term1Neg, falsum);
        final SddElement element2 = new SddElement(term1, term2);
        final SddNodeDecomposition decomp1 =
                sdd.decompOfPartition(new ArrayList<>(List.of(element1, element2))).asDecomposition();
        assertThat(decomp1.isDecomposition()).isTrue();
        assertThat(decomp1.isLiteral()).isFalse();
        assertThat(decomp1.isTrue()).isFalse();
        assertThat(decomp1.isFalse()).isFalse();
        assertThat(decomp1.getElements()).containsExactlyInAnyOrder(element1, element2);
        assertThat(decomp1.getVTree()).isEqualTo(sdd.getVTree().getRoot().asInternal().getLeft());

        final SddNodeDecomposition decomp1Neg = sdd.negate(decomp1).asDecomposition();
        final Variable v3 = f.variable("v3");
        final SddNodeTerminal term3 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v3), false);
        final SddElement element3 = new SddElement(decomp1, term3);
        final SddElement element4 = new SddElement(decomp1Neg, verum);
        final SddNodeDecomposition decomp2 =
                sdd.decompOfPartition(new ArrayList<>(List.of(element3, element4))).asDecomposition();
        assertThat(decomp2.isDecomposition()).isTrue();
        assertThat(decomp2.isLiteral()).isFalse();
        assertThat(decomp2.isTrue()).isFalse();
        assertThat(decomp2.isFalse()).isFalse();
        assertThat(decomp2.getElements()).containsExactlyInAnyOrder(element3, element4);
        assertThat(decomp2.getVTree()).isEqualTo(sdd.getVTree().getRoot());
    }

    @Test
    public void testNodeConstructionEmpty() {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = buildEmptySdd(f);
        assertThat(sdd.verum()).isEqualTo(sdd.verumNode);
        assertThat(sdd.falsum()).isEqualTo(sdd.falsumNode);
    }

    @Test
    public void testNegation() {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = buildSimpleSdd(f);
        assertThat(sdd.negate(sdd.verum())).isEqualTo(sdd.falsum());
        assertThat(sdd.negate(sdd.falsum())).isEqualTo(sdd.verum());

        final Variable v1 = f.variable("v1");
        final SddNodeTerminal term1 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v1), true);
        final SddNodeTerminal term1Neg = sdd.terminal(sdd.getVTree().getVTreeLeaf(v1), false);
        assertThat(sdd.negate(term1)).isEqualTo(term1Neg);
        assertThat(sdd.negate(term1Neg)).isEqualTo(term1);

        final Variable v2 = f.variable("v2");
        final SddNodeTerminal term2 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v2), true);
        final SddElement element1 = new SddElement(term1Neg, sdd.falsum());
        final SddElement element2 = new SddElement(term1, term2);
        final SddElement element3 = new SddElement(term1Neg, sdd.verumNode);
        final SddElement element4 = new SddElement(term1, sdd.negate(term2));
        final SddNodeDecomposition decomp1 =
                sdd.decompOfPartition(new ArrayList<>(List.of(element1, element2))).asDecomposition();
        final SddNodeDecomposition decomp1Neg = sdd.negate(decomp1).asDecomposition();
        assertThat(sdd.negate(decomp1Neg)).isEqualTo(decomp1);
        assertThat(decomp1Neg.getVTree()).isEqualTo(decomp1.getVTree());
        assertThat(decomp1Neg.getElements()).containsExactlyInAnyOrder(element3, element4);
    }

    @Test
    public void testConjunction() {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = buildSimpleSdd(f);
        assertThat(sdd.conjunction(sdd.verum(), sdd.verum())).isEqualTo(sdd.verum());
        assertThat(sdd.conjunction(sdd.verum(), sdd.falsum())).isEqualTo(sdd.falsum());

        final Variable v1 = f.variable("v1");
        final SddNodeTerminal term1 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v1), true);
        assertThat(sdd.conjunction(term1, sdd.verum())).isEqualTo(term1);
        assertThat(sdd.conjunction(term1, sdd.falsum())).isEqualTo(sdd.falsum());

        final Variable v2 = f.variable("v2");
        final SddNodeTerminal term2 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v2), true);
        final SddNode conjunction1 = sdd.conjunction(term1, term2);
        assertThat(conjunction1.asDecomposition().getElements()).containsExactlyInAnyOrder(
                new SddElement(term1, term2),
                new SddElement(sdd.negate(term1), sdd.falsum())
        );

        final Variable v3 = f.variable("v3");
        final SddNodeTerminal term3 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v3), false);
        final SddNode conjunction2 = sdd.conjunction(term2, term3);
        final SddNode conjunction3 = sdd.conjunction(conjunction1, conjunction2);
        assertThat(conjunction3.asDecomposition().getElements()).containsExactlyInAnyOrder(
                new SddElement(conjunction1, term3),
                new SddElement(sdd.negate(conjunction1), sdd.falsum())
        );
    }

    @Test
    public void testDisjunction() {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = buildSimpleSdd(f);
        assertThat(sdd.disjunction(sdd.falsum(), sdd.falsum())).isEqualTo(sdd.falsum());
        assertThat(sdd.disjunction(sdd.verum(), sdd.falsum())).isEqualTo(sdd.verum());

        final Variable v1 = f.variable("v1");
        final SddNodeTerminal term1 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v1), true);
        assertThat(sdd.disjunction(term1, sdd.verum())).isEqualTo(sdd.verum());
        assertThat(sdd.disjunction(term1, sdd.falsum())).isEqualTo(term1);

        final Variable v2 = f.variable("v2");
        final SddNodeTerminal term2 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v2), true);
        final SddNode conjunction1 = sdd.disjunction(term1, term2);
        assertThat(conjunction1.asDecomposition().getElements()).containsExactlyInAnyOrder(
                new SddElement(term1, sdd.verum()),
                new SddElement(sdd.negate(term1), term2)
        );

        final Variable v3 = f.variable("v3");
        final SddNodeTerminal term3 = sdd.terminal(sdd.getVTree().getVTreeLeaf(v3), false);
        final SddNode conjunction2 = sdd.disjunction(term2, term3);
        final SddNode conjunction3 = sdd.disjunction(conjunction1, conjunction2);
        assertThat(conjunction3.asDecomposition().getElements()).containsExactlyInAnyOrder(
                new SddElement(conjunction1, sdd.verum()),
                new SddElement(sdd.negate(conjunction1), term3)
        );
    }

    @Test
    public void testGarbageCollection() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaParser p = new PropositionalParser(f);
        final Sdd sdd = buildSimpleSdd(f);

        int lastVersion = 0;
        final SddCompilerConfig cConfig = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .preprocessing(false)
                .build();
        final SddNode rootNode1 = SddCompiler.compile(f, p.parse("v1 & (v2 | v3)"), cConfig).getNode();
        sdd.pin(rootNode1);
        assertThat(sdd.getVersion()).isGreaterThan(lastVersion);
        lastVersion = sdd.getVersion();
        sdd.pin(rootNode1);
        assertThat(sdd.getVersion()).isEqualTo(lastVersion);
        final SddNode rootNode2 = SddCompiler.compile(f, p.parse("(~v1 | v2) & (v2 | ~v3)"), cConfig).getNode();
        sdd.pin(rootNode2);
        assertThat(sdd.getVersion()).isGreaterThan(lastVersion);
        lastVersion = sdd.getVersion();
        final SddNode rootNode3 = SddCompiler.compile(f, p.parse("v1 | v3"), cConfig).getNode();
        sdd.pin(rootNode3);
        assertThat(sdd.getVersion()).isGreaterThan(lastVersion);
        lastVersion = sdd.getVersion();
        sdd.pin(rootNode3);
        assertThat(sdd.getVersion()).isEqualTo(lastVersion);
        final Set<Integer> nodes1Start = extractDecompIds(SddNodesFunction.get().execute(rootNode1));
        final Set<Integer> nodes2Start = extractDecompIds(SddNodesFunction.get().execute(rootNode2));
        final Set<Integer> nodes3Start = extractDecompIds(SddNodesFunction.get().execute(rootNode3));
        final Set<Integer> allNodesStart = extractDecompIds(sdd.sddDecompositions.values());
        assertThat(allNodesStart).containsAll(nodes1Start);
        assertThat(allNodesStart).containsAll(nodes2Start);
        assertThat(allNodesStart).containsAll(nodes3Start);
        assertThat(allNodesStart).isSubsetOf(
                Stream.concat(Stream.concat(nodes1Start.stream(), nodes2Start.stream()), nodes3Start.stream())
                        .collect(Collectors.toList()));
        sdd.garbageCollectAll();
        assertThat(extractDecompIds(sdd.sddDecompositions.values())).isEqualTo(allNodesStart);
        sdd.unpin(rootNode2);
        assertThat(sdd.getVersion()).isGreaterThan(lastVersion);
        lastVersion = sdd.getVersion();
        sdd.garbageCollectAll();
        final Set<Integer> allNodesUnpin1 = extractDecompIds(sdd.sddDecompositions.values());
        assertThat(allNodesUnpin1).containsAll(nodes1Start);
        assertThat(allNodesUnpin1).containsAll(nodes3Start);
        assertThat(allNodesUnpin1).isSubsetOf(
                Stream.concat(nodes1Start.stream(), nodes3Start.stream()).collect(Collectors.toList()));
        sdd.unpin(rootNode1);
        assertThat(sdd.getVersion()).isEqualTo(lastVersion);
        sdd.garbageCollectAll();
        final Set<Integer> allNodesUnpin2 = extractDecompIds(sdd.sddDecompositions.values());
        assertThat(allNodesUnpin2).containsAll(nodes1Start);
        assertThat(allNodesUnpin2).containsAll(nodes3Start);
        assertThat(allNodesUnpin2).isSubsetOf(
                Stream.concat(nodes1Start.stream(), nodes3Start.stream()).collect(Collectors.toList()));
        sdd.unpinAll();
        assertThat(sdd.getVersion()).isGreaterThan(lastVersion);
        sdd.garbageCollectAll();
        final Set<Integer> allNodesUnpin3 = extractDecompIds(sdd.sddDecompositions.values());
        assertThat(allNodesUnpin3).isEmpty();
    }

    @Test
    public void testCacheInvalidation() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaParser p = new PropositionalParser(f);
        final Sdd sdd = buildSimpleSdd(f);

        final SddCompilerConfig cConfig = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .preprocessing(false)
                .build();
        final SddNode rootNode1 = SddCompiler.compile(f, p.parse("v1 & (v2 | v3)"), cConfig).getNode();
        assertThat(rootNode1.getSizeEntry()).isNull();
        sdd.pin(rootNode1);
        sdd.getActiveSize();
        assertThat(rootNode1.getSizeEntry().isValid()).isTrue();
        sdd.pin(rootNode1);
        assertThat(rootNode1.getSizeEntry().isValid()).isTrue();
        final SddNode rootNode2 = SddCompiler.compile(f, p.parse("(~v1 | v2) & (v2 | ~v3)"), cConfig).getNode();
        sdd.pin(rootNode2);
        assertThat(rootNode1.getSizeEntry().isValid()).isFalse();
        sdd.getActiveSize();
        assertThat(rootNode1.getSizeEntry().isValid()).isTrue();
        sdd.unpin(rootNode1);
        assertThat(rootNode1.getSizeEntry().isValid()).isTrue();
        sdd.unpinAll();
        assertThat(rootNode1.getSizeEntry().isValid()).isFalse();
    }

    private Set<Integer> extractDecompIds(final Collection<? extends SddNode> nodes) {
        return nodes.stream()
                .filter(SddNode::isDecomposition)
                .map(SddNode::getId)
                .collect(Collectors.toSet());
    }

    private Sdd buildEmptySdd(final FormulaFactory f) {
        return new Sdd(f, VTreeRoot.builder().build(null));
    }

    private Sdd buildSimpleSdd(final FormulaFactory f) {
        return new Sdd(f, buildSimpleTree(f));
    }

    private VTreeRoot buildSimpleTree(final FormulaFactory f) {
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Variable v1 = f.variable("v1");
        final Variable v2 = f.variable("v2");
        final VTreeLeaf leaf1 = builder.vTreeLeaf(v1);
        final VTreeLeaf leaf2 = builder.vTreeLeaf(v2);
        final VTreeInternal node1 = builder.vTreeInternal(leaf1, leaf2);
        final VTreeLeaf leaf3 = builder.vTreeLeaf(f.variable("v3"));
        final VTreeInternal node2 = builder.vTreeInternal(node1, leaf3);
        return builder.build(node2);
    }
}
