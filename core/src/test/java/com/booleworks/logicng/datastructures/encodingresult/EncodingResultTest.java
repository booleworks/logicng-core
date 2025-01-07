package com.booleworks.logicng.datastructures.encodingresult;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.InternalAuxVarType;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EncodingResultTest {

    @Test
    public void testEncodingResultSolver() {
        final FormulaFactory f = FormulaFactory.caching();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResultSolver result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);

        final Variable v = result.newVariable(InternalAuxVarType.CC);
        assertThat(v.getName()).isEqualTo("@AUX_CC_SAT_SOLVER_0");

        result.addClause(f.variable("A"), v);
        final LngVector<LngClause> clss = solver.getUnderlyingSolver().getClauses();
        assertThat(clss).hasSize(1);

        final List<String> cls = Arrays.stream(clss.get(0).getData().toArray())
                .mapToObj(idx -> solver.getUnderlyingSolver().nameForIdx(LngCoreSolver.var(idx)))
                .collect(Collectors.toList());
        assertThat(cls).containsExactlyInAnyOrder("A", "@AUX_CC_SAT_SOLVER_0");

        assertThat(result.getFactory()).isEqualTo(f);
        assertThat(result.getSolver()).isEqualTo(solver.getUnderlyingSolver());
        assertThat(result.getProposition()).isEqualTo(null);
    }

    @Test
    public void testEncodingResultFF() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().name("ABC").build());
        final EncodingResultFF result = new EncodingResultFF(f);

        final Variable v = result.newVariable(InternalAuxVarType.CC);
        assertThat(v.getName()).isEqualTo("@AUX_ABC_CC_0");

        result.addClause(f.variable("A"), v);
        final List<Formula> clss = result.getResult();
        assertThat(clss).hasSize(1);

        final Formula cls = clss.get(0);
        assertThat(cls).isEqualTo(f.or(f.variable("A"), v));

        assertThat(result.getFactory()).isEqualTo(f);
    }
}
