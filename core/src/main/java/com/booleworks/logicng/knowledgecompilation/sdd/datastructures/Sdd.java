package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static com.booleworks.logicng.solvers.sat.LngCoreSolver.mkLit;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddApply;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddCompression;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * The SDD container.
 * <p>
 * This is the main class for creating and managing SDDs.  Similar to a formula
 * factory, it ensures that every vtree node and SDD node is unique, but,
 * additionally, it can perform garbage collection to clean up unused nodes.
 * It allows to perform different operation on SDDs, such as conjunction
 * and disjunction.
 *
 * <h2>Theoretical Foundations</h2>
 * <p>
 * There are two important datastructures: The {@link VTree} and the SDD
 * ({@link SddNode}).  The vtree is a complete binary tree storing variables at
 * its leaves and fundamentally dictates the structure of the SDD.  The SDD is
 * a DAG of {@link SddNodeTerminal}, {@link SddNodeDecomposition}, and
 * {@link SddElement}. And SDD element is a pair of SDD nodes, where the
 * first/left node is referred to as "prime" and the second/right node is
 * referred to as "sub".  Each SDD node respects to a vtree node, terminal nodes
 * to a leaf and decomposition nodes to an inner node.  A terminal node stores
 * a literal value of the variable at its vtree leaf.  The decomposition node
 * consists of a set of SDD elements, where the prime of each element needs to
 * respect the left child of its vtree, and the sub of each element needs to
 * respect the right child of its vtree.  Furthermore, all primes of the
 * elements of a decomposition node must be a prime partition, i.e., for all
 * {@code p_i, p_j (i != j): p_i & p_j = false} and
 * {@code p_0 | ... | p_n = true}.
 * <p>
 * A more detailed introduction can be found in:
 * [1] Darwiche, Adnan (2011). "SDD: A New Canonical Representation of
 * Propositional Knowledge Bases". International Joint Conference on Artificial
 * Intelligence.
 *
 * <h2>Basics</h2>
 *
 * <h3>Automated Construction</h3>
 * <p>
 * The most streamlined way to construct a formula is to use
 * {@link com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler SddCompiler}
 * to construct an SDD from a formula.  The compilers also provide the SDD
 * container and the vtree. Additionally, the bottom-up compiler can be used
 * with existing SDD containers and with custom vtrees.
 *
 * <h3>Manual Construction</h3>
 * <p>
 * We, generally, suggest to use the automated approach as it is less
 * error-prone. However, there are circumstance where it might be appropriate
 * to do this yourself. For example, if you have additional information about
 * the problem or want to build custom compilers.
 * <p>
 * First a vtree must be constructed using {@link VTreeRoot} and
 * {@link VTreeRoot.Builder}. This can either be done manually by using the
 * methods provided by {@link VTreeRoot.Builder}, or automatically by using
 * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration.VTreeGenerator VTreeGenerator}
 * (we suggest
 * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration.DecisionVTreeGenerator
 * DecisionVTreeGenerator}).
 * <p>
 * Afterward, the vtree root can be used to instantiate {@link Sdd}, which is
 * the factory/container for creating and storing all SDD nodes. Every SDD
 * created in this container must adhere to the structure defined by the vtree
 * used for the instantiation of the container.
 * <p>
 * The SDD container can be passed to the bottom-up compiler, which will then
 * compile formulas into SDDs with respect to the defined vtree. Alternatively,
 * SDD nodes can also be constructed manually, using
 * {@link Sdd#terminal(VTreeLeaf, boolean)} and
 * {@link Sdd#decompOfPartition(ArrayList)}. {@link Sdd#verum()} and
 * {@link Sdd#falsum()} return terminal nodes representing {@code true} or
 * {@code false}. <strong>However, the caller is responsible to ensure that
 * these nodes adhere to all invariants of reduced (compressed and trimmed, see
 * [1]) SDDs. These invariants are not checked and violating them results in
 * undefined behaviour!</strong>
 * <p>
 * Example for manual construction:
 * <pre>{@code
 * final FormulaFactory f = FormulaFactory.caching();
 * // Construct VTree
 * final VTreeRoot.Builder vBuilder = VTreeRoot.builder();
 * final VTreeLeaf va = vBuilder.vTreeLeaf(f.variable("A"));
 * final VTreeLeaf vb = vBuilder.vTreeLeaf(f.variable("A"));
 * final VTree vab = vBuilder.vTreeInternal(va, vb);
 * final VTreeRoot vRoot = vBuilder.build(vab);
 *
 * // Create SDD container
 * final Sdd sdd = new Sdd(f, vRoot);
 *
 * // Construct SDD
 * final SddNode ta = sdd.terminal(va, true);
 * final SddNode tb = sdd.terminal(vb, true);
 * final SddNode taNeg = sdd.terminal(va, false);
 * final ArrayList<SddElement> elements = new ArrayList<>(List.of(
 * new SddElement(ta, tb),
 * new SddElement(taNeg, sdd.falsum())
 * ));
 * final SddNode d_aAndb = sdd.decomposition(elements);
 * // d_aAndb represents the formula (A & B) | (~A & $false)
 * }</pre>
 *
 * <h2>Operations and Transformations</h2>
 * <p>
 * There are various operations one can apply to SDDs:
 * <ul>
 *     <li><strong>Conjunction and Disjunction:</strong> Two SDDs can be combined
 *     with a binary operation using {@link Sdd#conjunction(SddNode, SddNode)}
 *     or {@link Sdd#disjunction(SddNode, SddNode)}.</li>
 *     <li><strong>Functions:</strong>
 *     {@link com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction SddFunction}
 *     can compute commonly used properties of SDDs, such as the variables of
 *     the SDD, the size of an SDD, an enumeration of all models, ...
 *     </li>
 * </ul>
 * <h2>Internal Variable and Literal Representation</h2>
 * For optimization reasons, an SDD container uses an integer representation
 * for variables and literals internally and does not use the LNG types.
 * One can convert between both representations if necessary:
 * <ul>
 *     <li>{@link Sdd#variableToIndex(Variable)}: converts a lng variable to
 *     the internal representation.</li>
 *     <li>{@link Sdd#indexToVariable(int)}: converts the internal
 *     representation to a LNG variable.</li>
 *     <li>{@link Sdd#literalToIndex(Literal)}: converts a lng literal to the
 *     internal representation. </li>
 *     <li>{@link Sdd#literalToIndex(int, boolean)}: constructs an internal
 *     representation of a literal from a variable index and a phase.</li>
 *     <li>{@link Sdd#indexToLiteral(int)}: converts the internal representation
 *     to a LNG literal.</li>
 *     <li>{@link Sdd#negateLitIdx(int)}: negates a literal given as internal
 *     representation.</li>
 * </ul>
 * <p>
 * Often it is necessary to convert collection of variables or literals. For
 * that, {@link SddUtil} provides various conversion functions.
 *
 * <h2>Pinning and Garbage Collection</h2>
 * <p>
 * Transformations and binary operations can leave a lot of unused SDD nodes in
 * the SDD container.  Garbage collection can be used to remove those unused
 * nodes.  For that, however, the SDD container needs to know which nodes are
 * still in use and which are not.  With {@link Sdd#pin(SddNode)} nodes can be
 * marked as still in use. This will protect the node and all it's children
 * against being removed by the garbage collector. With
 * {@link Sdd#unpin(SddNode)} the pin of a pinned node can be removed, such that
 * it can be removed by the garbage collector.
 * <p>
 * Garbage collection can be invoked using {@link Sdd#garbageCollectAll()}. But
 * some algorithms such as the bottom-up compiler will also invoke garbage
 * collection. (This should usually be noted in the documentation of these
 * algorithms.) In those cases you need to pin all SDD nodes that are in the
 * SDD container that you still want to use afterward.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class Sdd {
    private final FormulaFactory f;
    private final VTreeRoot vTreeRoot;
    private int currentSddId;
    private final HashMap<ArrayList<SddElement>, SddNodeDecomposition> sddDecompositions;
    private final HashMap<Integer, SddNodeTerminal> sddTerminals;
    private final SddNodeTerminal verumNode;
    private final SddNodeTerminal falsumNode;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> conjunctions;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> disjunctions;

    private final VSCacheEntry<Integer> activeSize;

    public Sdd(final FormulaFactory f, final VTreeRoot root) {
        this.f = f;
        this.vTreeRoot = root;
        currentSddId = 2;
        sddDecompositions = new HashMap<>();
        sddTerminals = new HashMap<>();
        conjunctions = new HashMap<>();
        disjunctions = new HashMap<>();
        verumNode = new SddNodeTerminal(0, null, true);
        falsumNode = new SddNodeTerminal(1, null, false);
        verumNode.setNegation(falsumNode);
        falsumNode.setNegation(verumNode);
        activeSize = new VSCacheEntry<>(0);
    }

    /**
     * Converts the internal representation of a variable to the corresponding
     * LNG variable.
     * @param index the index of a variable
     * @return the corresponding LNG variable
     */
    public Variable indexToVariable(final int index) {
        return vTreeRoot.getVariables().indexToVariable(f, index);
    }

    /**
     * Converts a LNG variable to the internal representation. If the variable
     * is unknown to this SDD container, it will return {@code -1}.
     * @param variable the LNG variable
     * @return the internal representation or {@code -1} if the variable is
     * unknown
     */
    public int variableToIndex(final Variable variable) {
        return vTreeRoot.getVariables().variableToIndex(variable);
    }

    /**
     * Constructs a literal in the internal representation from a variable index
     * and a phase.
     * @param varIdx the index of the variable
     * @param phase  the phase
     * @return the literal in internal representation
     */
    public int literalToIndex(final int varIdx, final boolean phase) {
        return vTreeRoot.getVariables().literalToIndex(varIdx, phase);
    }

    /**
     * Converts a LNG literal to the internal representation. If the variable of
     * the literal is unknown to this SDD container, it will return {@code -1}.
     * @param literal the LNG literal
     * @return the internal representation or {@code -1} if the variable is
     * unknown
     */
    public int literalToIndex(final Literal literal) {
        return vTreeRoot.getVariables().literalToIndex(literal);
    }

    /**
     * Converts the internal representation of a literal to the corresponding
     * LNG literal.
     * @param litIdx the index of a literal
     * @return the corresponding LNG literal
     */
    public Literal indexToLiteral(final int litIdx) {
        return vTreeRoot.getVariables().indexToLiteral(f, litIdx);
    }

    /**
     * Converts the internal representation of a literal to the internal
     * representation of the variable of the literal.
     * @param litIdx the literal index
     * @return the variable index
     */
    public int litIdxToVarIdx(final int litIdx) {
        return vTreeRoot.getVariables().litIdxToVarIdx(litIdx);
    }

    /**
     * Negates a literal in internal representation.
     * @param litIdx the literal index
     * @return the index of the negated literal
     */
    public int negateLitIdx(final int litIdx) {
        return vTreeRoot.getVariables().negateLitIdx(litIdx);
    }

    /**
     * Returns whether there exists an internal representation of the variable.
     * <p>
     * An internal representation can be created by constructing a vtree leaf
     * for the variable.
     * @param variable the variable
     * @return whether there exists an internal representation.
     */
    public boolean knows(final Variable variable) {
        return vTreeRoot.getVariables().knows(variable);
    }

    /**
     * Returns whether a vtree is defined in this SDD container. An SDD
     * container can have no vtree, but can then only create trivial nodes.
     * @return whether a vtree is defined
     */
    public boolean hasVTree() {
        return vTreeRoot.getRoot() != null;
    }

    /**
     * Returns the SDD node representing {@code true}.
     * @return the SDD node representing {@code true}
     */
    public SddNodeTerminal verum() {
        return verumNode;
    }

    /**
     * Returns the SDD node representing {@code false}.
     * @return the SDD node representing {@code false}
     */
    public SddNodeTerminal falsum() {
        return falsumNode;
    }

    /**
     * Constructs a new terminal SDD node for a vtree leaf and a phase or
     * returns the existing node if it already exists.
     * @param terminal the vtree leaf
     * @param phase    the phase
     * @return the terminal SDD node
     */
    public SddNodeTerminal terminal(final VTreeLeaf terminal, final boolean phase) {
        final int litIdx = mkLit(terminal.getVariable(), !phase);
        final SddNodeTerminal cached = sddTerminals.get(litIdx);
        if (cached != null) {
            return cached;
        }
        final SddNodeTerminal newNode = new SddNodeTerminal(currentSddId++, terminal, phase);
        sddTerminals.put(litIdx, newNode);

        final int negTerminal = mkLit(terminal.getVariable(), phase);
        final SddNodeTerminal newNodeNeg = new SddNodeTerminal(currentSddId++, terminal, !phase);
        sddTerminals.put(negTerminal, newNodeNeg);

        newNode.setNegation(newNodeNeg);
        newNodeNeg.setNegation(newNode);
        return newNode;
    }

    /**
     * Creates a decomposition node for a list of SDD elements, where the
     * primes form a complete partition.  The elements do not need to be
     * compressed and trimmed.
     * <p>
     * The caller is responsible to ensure that the elements adhere to the
     * structure of the active vtree of this container.
     * @param newElements the elements for the decomposition node
     * @return the decomposition node
     */
    public SddNode decompOfPartition(final ArrayList<SddElement> newElements) {
        return decompOfPartition(newElements, NopHandler.get()).getResult();
    }

    /**
     * Creates a decomposition node for a list of SDD elements, where the
     * primes form a complete partition.  The elements do not need to be
     * compressed and trimmed.
     * <p>
     * The caller is responsible to ensure that the elements adhere to the
     * structure of the active vtree of this container.
     * @param newElements the elements for the decomposition node
     * @param handler     the computation handler
     * @return the decomposition node or the canceling cause if the computation
     * was aborted by the handler.
     */
    public LngResult<SddNode> decompOfPartition(final ArrayList<SddElement> newElements,
                                                final ComputationHandler handler) {
        newElements.sort(SddElement::compareTo);
        final LngResult<Pair<SddNode, ArrayList<SddElement>>> res =
                SddCompression.compressAndTrim(newElements, this, handler);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        }
        if (res.getResult().getFirst() != null) {
            return LngResult.of(res.getResult().getFirst());
        } else {
            return LngResult.of(decomposition(res.getResult().getSecond()));
        }
    }

    /**
     * Creates a decomposition node for a list of SDD elements, where the
     * primes form a complete partition, and the elements are compressed and
     * trimmed.
     * <p>
     * The caller is responsible to ensure that the elements adhere to the
     * structure of the active vtree of this container.
     * @param newElements the elements for the decomposition node
     * @return the decomposition node
     */
    public SddNode decompOfCompressedPartition(final ArrayList<SddElement> newElements) {
        assert !newElements.isEmpty();
        newElements.sort(SddElement::compareTo);
        assert SddCompression.isCompressed(newElements);
        return decomposition(newElements);
    }

    private SddNodeDecomposition decomposition(final ArrayList<SddElement> elements) {
        final SddNodeDecomposition cached = sddDecompositions.get(elements);
        if (cached != null) {
            return cached;
        }
        final VTree vTree = SddUtil.lcaOfCompressedElements(elements, this);
        final SddNodeDecomposition newNode =
                new SddNodeDecomposition(currentSddId++, vTree, elements);
        sddDecompositions.put(elements, newNode);
        return newNode;
    }

    /**
     * Returns (and computes if not cached) the conjunction of two SDD nodes.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * of the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the result of the binary operation has changed
     * potentially.
     * @param left    the first node
     * @param right   the second node
     * @param handler the computation handler
     * @return the conjunction or the canceling cause if the computation was
     * aborted by the handler.
     */
    public LngResult<SddNode> conjunction(final SddNode left, final SddNode right, final ComputationHandler handler) {
        return binaryOperation(left, right, SddApply.Operation.CONJUNCTION, handler);
    }

    /**
     * Returns (and computes if not cached) the conjunction of two SDD nodes.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * of the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the result of the binary operation has changed
     * potentially.
     * @param left  the first node
     * @param right the second node
     * @return the conjunction
     */
    public SddNode conjunction(final SddNode left, final SddNode right) {
        return conjunction(left, right, NopHandler.get()).getResult();
    }

    /**
     * <strong>Do not use this function!</strong> An optimized conjunction
     * operator for joining two SDD nodes with disjunct variables.
     * @param left  the first node
     * @param right the second node
     * @return the conjunction
     */
    public SddNode conjunctionUnsafe(final SddNode left, final SddNode right) {
        assert left != null && right != null;
        if (left.isFalse() || right.isFalse()) {
            return falsum();
        }
        if (left.isTrue()) {
            return right;
        }
        if (right.isTrue()) {
            return left;
        }

        final SddNode cached = lookupApplyComputation(left, right, SddApply.Operation.CONJUNCTION);
        if (cached != null) {
            return cached;
        }

        final ArrayList<SddElement> newElements = new ArrayList<>();
        newElements.add(new SddElement(left, right));
        newElements.add(new SddElement(negate(left), falsum()));
        final SddNode newNode = decompOfCompressedPartition(newElements);
        cacheApplyComputation(left, right, newNode, SddApply.Operation.CONJUNCTION);
        return newNode;
    }

    /**
     * Returns (and computes if not cached) the disjunction of two SDD nodes.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * of the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the result of the binary operation has changed
     * potentially.
     * @param left    the first node
     * @param right   the second node
     * @param handler the computation handler
     * @return the disjunction or the canceling cause if the computation was
     * aborted by the handler.
     */
    public LngResult<SddNode> disjunction(final SddNode left, final SddNode right, final ComputationHandler handler) {
        return binaryOperation(left, right, SddApply.Operation.DISJUNCTION, handler);
    }

    /**
     * Returns (and computes if not cached) the disjunction of two SDD nodes.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * of the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the result of the binary operation has changed
     * potentially.
     * @param left  the first node
     * @param right the second node
     * @return the disjunction
     */
    public SddNode disjunction(final SddNode left, final SddNode right) {
        return disjunction(left, right, NopHandler.get()).getResult();
    }

    /**
     * Returns (and computes if not cached) the SDD node resulting from the
     * application of a binary operation on two SDD nodes.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * of the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the result of the binary operation has changed
     * potentially.
     * @param left    the first SDD node
     * @param right   the second SDD node
     * @param op      the binary operation
     * @param handler the computation handler
     * @return the result of the binary operation or the canceling cause if the
     * computation was aborted by the handler.
     */
    public LngResult<SddNode> binaryOperation(final SddNode left, final SddNode right, final SddApply.Operation op,
                                              final ComputationHandler handler) {
        final SddNode cached = lookupApplyComputation(left, right, op);
        if (cached != null) {
            return LngResult.of(cached);
        }
        final LngResult<SddNode> result = SddApply.apply(left, right, op, this, handler);
        if (!result.isSuccess()) {
            return result;
        }
        final SddNode newNode = result.getResult();
        cacheApplyComputation(left, right, newNode, op);
        return result;
    }

    /**
     * Returns (and computes if not cached) the SDD node resulting from the
     * application of a binary operation on two SDD nodes.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * of the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the result of the binary operation has changed
     * potentially.
     * @param left  the first SDD node
     * @param right the secon SDD node
     * @param op    the binary operation
     * @return the result of the binary operation
     */
    public SddNode binaryOperation(final SddNode left, final SddNode right, final SddApply.Operation op) {
        return binaryOperation(left, right, op, NopHandler.get()).getResult();
    }

    private SddNode lookupApplyComputation(final SddNode left, final SddNode right,
                                           final SddApply.Operation op) {
        switch (op) {
            case CONJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                return conjunctions.get(key);
            }
            case DISJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                return disjunctions.get(key);
            }
            default:
                throw new RuntimeException("Unknown operation type");
        }
    }

    private void cacheApplyComputation(final SddNode left, final SddNode right, final SddNode result,
                                       final SddApply.Operation op) {
        switch (op) {
            case CONJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                conjunctions.put(key, result);
                return;
            }
            case DISJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                disjunctions.put(key, result);
                return;
            }
            default:
                throw new RuntimeException("Unknown operation type");
        }
    }

    /**
     * Returns the active vtree of this SDD container.
     * <p>
     * Must not be called if there is no active vtree.
     * @return the active vtree of this SDD container
     */
    public VTreeRoot getVTree() {
        return vTreeRoot;
    }

    /**
     * Returns the negation of the SDD node.
     * <p>
     * This value is computed lazily and is cached. After each generation bump
     * in the vtree stack (e.g. a global transformation), the cache is
     * invalidated because the negation has changed potentially.
     * @param node the SDD node
     * @return the negation of the SDD node
     */
    public SddNode negate(final SddNode node) {
        final SddNode cached = node.getNegation();
        if (cached != null) {
            return cached;
        }

        final SddNode nodeNeg;
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            //Note: compression is not possible here
            final ArrayList<SddElement> newElements = new ArrayList<>();
            for (final SddElement element : decomp) {
                final SddNode subNeg = negate(element.getSub());
                newElements.add(new SddElement(element.getPrime(), subNeg));
            }
            nodeNeg = decompOfCompressedPartition(newElements);
        } else if (node.isLiteral()) {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = terminal(t.getVTree(), !t.getPhase());
        } else {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = t.getPhase() ? falsum() : verum();
        }
        node.setNegation(nodeNeg);
        nodeNeg.setNegation(node);
        return nodeNeg;
    }

    /**
     * Returns the negation of this SDD node if it was already computed and is
     * cached otherwise it returns {@code null}.
     * @param node the SDD node
     * @return the negation of this SDD node if it is cached or {@code null}
     * otherwise
     */
    public SddNode getNegationIfCached(final SddNode node) {
        if (node.getNegation() == null) {
            return null;
        } else {
            return node.getNegation();
        }
    }

    /**
     * Pins an SDD node to the active vtree, so that the node and all its
     * successors are protected from the garbage collector. A node is still
     * protected even if the vtree becomes inactive.
     * <p>
     * The node can be unpinned by calling {@link Sdd#unpin(SddNode)} or by
     * removing the vtree from the vtree stack.
     * @param node the SDD node
     */
    public void pin(final SddNode node) {
        if (node.isDecomposition()) {
            vTreeRoot.pin(node.asDecomposition());
        }
    }

    /**
     * Unpins an SDD node from the active vtree, so that it can be removed by
     * the garbage collector.
     * <p>
     * {@code node} must be a pinned node in the active vtree.
     * @param node the pinned SDD node
     */
    public void unpin(final SddNode node) {
        if (node.isDecomposition()) {
            vTreeRoot.unpin(node.asDecomposition());
        }
    }

    /**
     * Invokes garbage collection for all nodes.
     * <p>
     * Garbage collection removes all nodes that are not a successor of a pinned
     * node.  Nodes that are pinned by an inactive vtree will not be collected.
     */
    public void garbageCollectAll() {
        final List<SddNode> unusedNodes = sddDecompositions.values().stream()
                .filter(d -> d.getRefs() == 0)
                .collect(Collectors.toList());
        garbageCollectSelection(unusedNodes);
    }

    /**
     * Invokes garbage collection for a selection of nodes.
     * <p>
     * Garbage collection removes all nodes that are not a successor of a pinned
     * node.  Nodes that are pinned by a non-active vtree will not be collected.
     * It considers the given nodes as root and traverses only to the children
     * of these nodes.
     * @param nodes the root nodes for the garbage collection.
     */
    public void garbageCollectSelection(final Collection<SddNode> nodes) {
        final HashSet<Integer> idsToRemove = new HashSet<>();
        final Queue<SddNode> children = new ArrayDeque<>(nodes);
        while (!children.isEmpty()) {
            final SddNode node = children.poll();
            if (node.isDecomposition() && node.asDecomposition().getRefs() == 0) {
                node.asDecomposition().free();
                idsToRemove.add(node.getId());
                for (final SddElement element : node.asDecomposition()) {
                    final SddNode prime = element.getPrime();
                    final SddNode sub = element.getSub();
                    if (prime.isDecomposition() && prime.asDecomposition().getRefs() == 0) {
                        children.add(prime.asDecomposition());
                    }
                    if (sub.isDecomposition() && sub.asDecomposition().getRefs() == 0) {
                        children.add(sub.asDecomposition());
                    }
                }
            }
        }
        sddDecompositions.entrySet().removeIf(
                e -> idsToRemove.contains(e.getValue().id));
        conjunctions.entrySet().removeIf(e ->
                idsToRemove.contains(e.getValue().id)
                        || idsToRemove.contains(e.getKey().getFirst().id)
                        || idsToRemove.contains(e.getKey().getSecond().id));
        disjunctions.entrySet().removeIf(e ->
                idsToRemove.contains(e.getValue().id)
                        || idsToRemove.contains(e.getKey().getFirst().id)
                        || idsToRemove.contains(e.getKey().getSecond().id));
    }

    /**
     * Returns the active size of this SDD container.  The active size is the
     * collective size of all pinned SDD nodes of the active vtree.
     * <p>
     * This value is computed lazily and is cached.  After each version bump
     * (e.g. pinning/unpinning a node or generation bump) the cache is invalided
     * and the value is recomputed.
     * @return the active size of this SDD container.
     */
    public int getActiveSize() {
        if (activeSize.isValid()) {
            return activeSize.getElement();
        } else {
            int size = 0;
            for (final SddNode node : getVTree().getPinnedNodes()) {
                if (node.getSizeEntry() == null || !node.getSizeEntry().isValid()) {
                    size += getActiveSize(node);
                }
            }
            activeSize.update(size);
            return size;
        }
    }

    private int getActiveSize(final SddNode node) {
        if (node.getSizeEntry() != null && node.getSizeEntry().isValid()) {
            return 0;
        } else {
            int size = 1;
            if (node.isDecomposition()) {
                for (final SddElement element : node.asDecomposition()) {
                    if (element.getPrime().getSizeEntry() == null || !element.getPrime().getSizeEntry().isValid()) {
                        size += getActiveSize(element.getPrime());
                    }
                    if (element.getSub().getSizeEntry() == null || !element.getSub().getSizeEntry().isValid()) {
                        size += getActiveSize(element.getSub());
                    }
                }
            }
            if (node.getSizeEntry() == null) {
                node.setSizeEntry(new VSCacheEntry<>(size));
            } else {
                node.getSizeEntry().update(size);
            }
            return size;
        }
    }

    /**
     * Returns a view into the decomposition nodes of this SDD container.
     * @return a view into the decomposition nodes of this SDD container
     */
    public Collection<SddNodeDecomposition> getDecompositionNodes() {
        return Collections.unmodifiableCollection(sddDecompositions.values());
    }

    /**
     * Returns a view into the terminal nodes of this SDD container.
     * @return a view into the terminal nodes of this SDD container
     */
    public Collection<SddNodeTerminal> getTerminalNodes() {
        return Collections.unmodifiableCollection(sddTerminals.values());
    }

    /**
     * Returns the number of terminal and decomposition nodes.
     * @return the number of terminal and decomposition nodes
     */
    public int getSddNodeCount() {
        return sddTerminals.size() + sddDecompositions.size();
    }

    /**
     * Returns the number terminal nodes in this SDD container.
     * @return the number terminal nodes in this SDD container
     */
    public int getTerminalCount() {
        return sddTerminals.size();
    }

    /**
     * Returns the number of decomposition nodes in this SDD container.
     * @return the number of decomposition nodes in this SDD container
     */
    public int getDecompositionCount() {
        return sddDecompositions.size();
    }

    /**
     * Returns the formula factory of this SDD container.
     * @return the formula factory of this SDD container
     */
    public FormulaFactory getFactory() {
        return f;
    }

    private <E> VSCacheEntry<E> invariantVSCacheEntry(final E element) {
        final VSCacheEntry<E> entry = new VSCacheEntry<>(element);
        entry.version = -1;
        return entry;
    }

    class VSCacheEntry<T> {
        int version;
        T element;

        VSCacheEntry(final T element) {
            this.version = getVTree().getVersion();
            this.element = element;
        }

        int getVersion() {
            return version;
        }

        T getElement() {
            return element;
        }

        void update(final T element) {
            assert version != -1;
            this.element = element;
            this.version = getVTree().getVersion();
        }

        boolean isValid() {
            return version == -1 || version == getVTree().getVersion();
        }
    }
}
