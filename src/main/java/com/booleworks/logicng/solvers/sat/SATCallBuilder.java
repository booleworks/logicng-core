package com.booleworks.logicng.solvers.sat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.MiniSat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SATCallBuilder {
    private final FormulaFactory f;
    private final MiniSat solver;
    private SATHandler handler;
    private final List<Proposition> additionalPropositions;
    private List<? extends Literal> selectionOrder;

    SATCallBuilder(final FormulaFactory f, final MiniSat solver) {
        this.f = f;
        this.solver = solver;
        additionalPropositions = new ArrayList<>();
    }

    public SATCall solve() {
        return new SATCall(f, solver, handler, additionalPropositions, selectionOrder);
    }

    public SATCallBuilder handler(final SATHandler handler) {
        this.handler = handler;
        return this;
    }

    // TODO could be removed (when we're sure about the API)
    public SATCallBuilder assumptions(final Collection<? extends Literal> assumptions) {
        return addFormulas(assumptions);
    }

    public SATCallBuilder assumptions(final Literal... assumptions) {
        return addFormulas(assumptions);
    }

    public SATCallBuilder addFormulas(final Collection<? extends Formula> formulas) {
        for (final Formula formula : formulas) {
            additionalPropositions.add(new StandardProposition(formula));
        }
        return this;
    }

    public SATCallBuilder addFormulas(final Formula... formulas) {
        return addFormulas(Arrays.asList(formulas));
    }

    public SATCallBuilder addPropositions(final Collection<? extends Proposition> propositions) {
        additionalPropositions.addAll(propositions);
        return this;
    }

    public SATCallBuilder addPropositions(final Proposition... propositions) {
        additionalPropositions.addAll(List.of(propositions));
        return this;
    }

    public SATCallBuilder selectionOrder(final List<? extends Literal> selectionOrder) {
        this.selectionOrder = selectionOrder;
        return this;
    }

    // Utility methods, s.t. the user does not need to use the try-with resource

    public Tristate sat() {
        try (final SATCall call = solve()) {
            return call.getSatResult();
        }
    }

    public Assignment model(final Collection<Variable> variables) {
        try (final SATCall call = solve()) {
            return call.model(variables);
        }
    }

    public Assignment model(final Variable... variables) {
        return model(Arrays.asList(variables));
    }

    public UNSATCore<Proposition> unsatCore() {
        try (final SATCall call = solve()) {
            return call.unsatCore();
        }
    }
}
