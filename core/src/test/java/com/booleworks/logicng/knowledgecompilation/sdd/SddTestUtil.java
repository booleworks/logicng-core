package com.booleworks.logicng.knowledgecompilation.sdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.io.graphical.GraphicalDotWriter;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddDotExport;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddExportFormula;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelCountFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.VTreeDotExport;
import com.booleworks.logicng.modelcounting.ModelCounter;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.List;

public class SddTestUtil {
    public static void validateMC(final SddNode node, final VTreeRoot root, final Formula originalFormula,
                                  final SddFactory sf) {
        final BigInteger models =
                new SddModelCountFunction(originalFormula.variables(sf.getFactory()), node, root).apply(sf);
        final BigInteger expected = ModelCounter.count(sf.getFactory(), List.of(originalFormula),
                originalFormula.variables(sf.getFactory()));
        assertThat(models).isEqualTo(expected);
    }

    public static void validateExport(final SddNode node, final Formula originalFormula, final SddFactory sf) {
        final Formula exported = sf.apply(new SddExportFormula(node));
        assertThat(sf.getFactory().equivalence(originalFormula, exported).isTautology(sf.getFactory())).isTrue();
    }

    public static void printGraph(final SddNode node, final VTreeRoot root, final SddFactory sf) {
        final StringWriter sw = new StringWriter();
        sf.apply(new SddDotExport(node, root, sw));
        sw.flush();
        System.out.println(sw);
    }

    public static void printVTree(final VTree node, final SddFactory sf) {
        final GraphicalRepresentation gr = sf.apply(new VTreeDotExport(node));
        System.out.println(gr.writeString(GraphicalDotWriter.get()));
    }
}
