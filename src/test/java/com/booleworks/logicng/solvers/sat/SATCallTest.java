package com.booleworks.logicng.solvers.sat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.handlers.TimeoutSATHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SATSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SATCallTest {

    @Test
    public void test() throws ParserException {
        // wie bisher
        final FormulaFactory f = FormulaFactory.caching();
        final List<Variable> vars = List.of(f.variable("B"), f.variable("C"));
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("(A | B) & (C | ~B) & (B | D)"));

        // Aufbau Builder (tut noch nichts)
        final SATCallBuilder satBuilder = solver.satCall()
                .addFormulas(List.of(f.parse("A | D")))
                .selectionOrder(List.of(f.variable("A"), f.variable("B"), f.variable("C"), f.variable("D")))
                .assumptions(List.of(f.literal("A", false)))
                .handler(new TimeoutSATHandler(1000, TimeoutHandler.TimerType.RESTARTING_TIMEOUT));

        // start() ruft dann u.a. solve() auf
        try (final SATCall satCall = satBuilder.solve()) {
            if (satCall.getSatResult() == Tristate.TRUE) {
                final Assignment model = satCall.model(vars);
            } else {
                final UNSATCore<Proposition> mus = satCall.unsatCore();
            }
        }

        // Shortcuts (ich glaube das wollen wir wohl ;) ), auch in Kombination mit Assumptions und Co möglich
        // die wrappen praktisch start(), model()/unsatCore() und close()
        final boolean sat = solver.sat();
        final Assignment model = solver.satCall().selectionOrder(vars).model(vars);
//        final UNSATCore<Proposition> mus = solver.satCall().assumptions(vars).unsatCore();

        // böse, weil der SATCall ein AutoClosable ist, das geschlossen werden muss (entsprechend meckert IntelliJ auch)
        final Assignment model2 = solver.satCall().solve().model(vars);
        // Exception, weil obiger Call noch offen ist
//        solver.satCall().solve();
    }
}
