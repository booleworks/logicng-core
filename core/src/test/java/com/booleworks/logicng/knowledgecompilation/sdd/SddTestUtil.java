package com.booleworks.logicng.knowledgecompilation.sdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.graphical.GraphicalDotWriter;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeExport;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddDotExport;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddExportFormula;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelCountFunction;
import com.booleworks.logicng.modelcounting.ModelCounter;
import com.booleworks.logicng.transformations.PureExpansionTransformation;
import com.booleworks.logicng.transformations.cnf.CnfConfig;
import com.booleworks.logicng.transformations.cnf.CnfEncoder;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.List;

public class SddTestUtil {
    public static void validateMC(final SddNode node, final Formula originalFormula, final Sdd sdd) {
        final BigInteger models =
                new SddModelCountFunction(originalFormula.variables(sdd.getFactory()), sdd).execute(node);
        final BigInteger expected = ModelCounter.count(sdd.getFactory(), List.of(originalFormula),
                originalFormula.variables(sdd.getFactory()));
        assertThat(models).isEqualTo(expected);
    }

    public static void validateExport(final SddNode node, final Formula originalFormula, final Sdd sdd) {
        final Formula exported = node.execute(new SddExportFormula(sdd));
        assertThat(sdd.getFactory().equivalence(originalFormula, exported).isTautology(sdd.getFactory())).isTrue();
    }

    public static void printGraph(final SddNode node, final Sdd sdd) {
        final StringWriter sw = new StringWriter();
        node.execute(new SddDotExport(sdd, sw));
        sw.flush();
        System.out.println(sw);
    }

    public static void printVTree(final VTree node, final Sdd sdd) {
        final GraphicalRepresentation gr = VTreeExport.exportDot(node, sdd);
        System.out.println(gr.writeString(GraphicalDotWriter.get()));
    }

    public static VTree getVTreeAtPosition(final int position, final VTree root) {
        if (root.getPosition() == position) {
            return root;
        } else if (position < root.getPosition() && !root.isLeaf()) {
            return getVTreeAtPosition(position, root.asInternal().getLeft());
        } else if (position > root.getPosition() && !root.isLeaf()) {
            return getVTreeAtPosition(position, root.asInternal().getRight());
        } else {
            return null;
        }
    }

    public static Formula encodeAsPureCnf(final FormulaFactory f, final Formula formula) {
        final PureExpansionTransformation expander = new PureExpansionTransformation(f);
        final Formula expandedFormula = formula.transform(expander);

        final CnfConfig cnfConfig = CnfConfig.builder()
                .algorithm(CnfConfig.Algorithm.ADVANCED)
                .fallbackAlgorithmForAdvancedEncoding(CnfConfig.Algorithm.TSEITIN).build();

        return CnfEncoder.encode(f, expandedFormula, cnfConfig);
    }

    public static Formula encodeWithFactorization(final FormulaFactory f, final Formula formula) {
        final PureExpansionTransformation expander = new PureExpansionTransformation(f);
        final Formula expandedFormula = formula.transform(expander);

        final CnfConfig cnfConfig = CnfConfig.builder()
                .algorithm(CnfConfig.Algorithm.FACTORIZATION)
                .build();

        return CnfEncoder.encode(f, expandedFormula, cnfConfig);
    }
}
