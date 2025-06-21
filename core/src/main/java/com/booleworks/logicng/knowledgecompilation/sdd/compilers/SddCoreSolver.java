package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfCoreSolver;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.solvers.datastructures.LngClause;

import java.util.BitSet;

public class SddCoreSolver extends DnnfCoreSolver implements SddSatSolver {
    protected LngVector<VTreeLeaf> leaves = null;

    protected BitSet trailBitset = new BitSet();
    protected LngVector<BitSet> trailLimBitsets = new LngVector<>();

    int lastAssertionLevel = 0;
    int canceledUntil = -1;
    protected BitSet subsumedClauseBitset = new BitSet();

    public SddCoreSolver(final FormulaFactory f, final int numberOfVariables) {
        super(f, numberOfVariables);
    }

    public void init(final LngVector<VTreeLeaf> leaves) {
        this.leaves = leaves;
    }

    @Override
    public int impliedLiteral(final int variable) {
        final BitSet trailBitset = impliedLiteralBitset();
        if (trailBitset.get(variable * 2)) {
            return variable * 2;
        } else if (trailBitset.get(variable * 2 + 1)) {
            return variable * 2 + 1;
        }
        return -1;
    }

    @Override
    public BitSet subsumedClauseBitset() {
        if (canceledUntil != -1 && canceledUntil < lastAssertionLevel) {
            lastAssertionLevel = 0;
            subsumedClauseBitset.clear();
        }
        canceledUntil = -1;
        for (; lastAssertionLevel < trail.size(); ++lastAssertionLevel) {
            final int literalIndex = trail.get(lastAssertionLevel);
            final int variableIndex = var(literalIndex);
            final VTreeLeaf leaf = leaves.get(variableIndex);
            if (sign(literalIndex)) {
                subsumedClauseBitset.or(leaf.getClauseNegMask());
            } else {
                subsumedClauseBitset.or(leaf.getClausePosMask());
            }
        }
        return subsumedClauseBitset;
    }

    @Override
    public BitSet impliedLiteralBitset() {
        return trailBitset;
    }

    @Override
    protected void cancelUntil(final int level) {
        if (decisionLevel() > level) {
            trailBitset = (BitSet) trailLimBitsets.get(level).clone();
            if (canceledUntil == -1) {
                canceledUntil = level;
            } else {
                canceledUntil = Math.min(canceledUntil, level);
            }
        }
        super.cancelUntil(level);
    }

    @Override
    protected void pushTrailLim() {
        super.pushTrailLim();
        if (trailLim.size() - 1 < trailLimBitsets.size()) {
            trailLimBitsets.get(trailLim.size() - 1).clear();
            trailLimBitsets.get(trailLim.size() - 1).or(trailBitset);
        } else {
            trailLimBitsets.push((BitSet) trailBitset.clone());
        }
    }

    @Override
    protected void uncheckedEnqueue(final int lit, final LngClause reason) {
        trailBitset.set(lit);
        super.uncheckedEnqueue(lit, reason);
    }

    @Override
    protected void completeBacktrack() {
        trailBitset.clear();
        super.completeBacktrack();
    }
}
