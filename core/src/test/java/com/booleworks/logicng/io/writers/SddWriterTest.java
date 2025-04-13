package com.booleworks.logicng.io.writers;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.SddReader;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeSet;

public class SddWriterTest {

    @Test
    public void testSimpleVTree() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final VTreeLeaf l1 = sf.vTreeLeaf(f.variable("A"));
        testVTreeFile("simple1", l1);
        final VTreeLeaf l2 = sf.vTreeLeaf(f.variable("B"));
        final VTree d = sf.vTreeInternal(l1, l2);
        testVTreeFile("simple2", d);
    }

    @Test
    public void testVTree() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final VTreeLeaf l1 = sf.vTreeLeaf(f.variable("v1"));
        final VTreeLeaf l10 = sf.vTreeLeaf(f.variable("v10"));
        final VTreeLeaf l11 = sf.vTreeLeaf(f.variable("v11"));
        final VTreeLeaf l12 = sf.vTreeLeaf(f.variable("v12"));
        final VTreeLeaf l13 = sf.vTreeLeaf(f.variable("v13"));
        final VTreeLeaf l14 = sf.vTreeLeaf(f.variable("v14"));
        final VTreeLeaf l15 = sf.vTreeLeaf(f.variable("v15"));
        final VTreeLeaf l16 = sf.vTreeLeaf(f.variable("v15"));
        final VTreeLeaf l17 = sf.vTreeLeaf(f.variable("v15"));
        final VTreeLeaf l18 = sf.vTreeLeaf(f.variable("v15"));
        final VTree d2 = sf.vTreeInternal(l1, l10);
        final VTree d6 = sf.vTreeInternal(l12, l13);
        final VTree d7 = sf.vTreeInternal(l11, d6);
        final VTree d8 = sf.vTreeInternal(d2, d7);
        final VTree d11 = sf.vTreeInternal(l14, l15);
        final VTree d15 = sf.vTreeInternal(l17, l18);
        final VTree d16 = sf.vTreeInternal(l16, d15);
        final VTree d17 = sf.vTreeInternal(d11, d16);
        final VTree d18 = sf.vTreeInternal(d8, d17);
        testVTreeFile("vtree1", d18);
    }

    @Test
    public void testVTreeImportExport() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final VTree vTree = SddReader.readVTree(new File("../test_files/sdd/big-swap.vtree"), sf);
        SddWriter.writeVTree(new File("../test_files/writers/temp/big-swap.vtree"), vTree);
        final VTree vTree2 = SddReader.readVTree(new File("../test_files/writers/temp/big-swap.vtree"), sf);
        assert vTree == vTree2;
    }

    private void testVTreeFile(final String fileName, final VTree vTree) throws IOException {
        SddWriter.writeVTree(new File("../test_files/writers/temp/" + fileName + ".vtree"), vTree);
        final File temp = new File("../test_files/writers/temp/" + fileName + ".vtree");
        final File expected = new File("../test_files/writers/sdd/" + fileName + ".vtree");
        assertFilesEqual(expected, temp);
    }

    @Test
    public void testSimpleSdd() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final VTreeRoot root =
                sf.constructRoot(SddReader.readVTree(new File("../test_files/sdd/big-swap.vtree"), sf));
        final SddNode terminal1 = sf.terminal(f.variable("v1"), root);
        final SddNode terminal2 = sf.terminal(f.variable("v1").negate(f), root);
        final SddNode terminal3 = sf.terminal(f.variable("v8"), root);
        final TreeSet<SddElement> elems = new TreeSet<>();
        elems.add(new SddElement(terminal1, terminal3));
        elems.add(new SddElement(terminal2, sf.verum()));
        final SddNode decomp1 = sf.decomposition(elems, root);
        testSddFile("sdd_simple1", terminal1, root);
        testSddFile("sdd_simple2", decomp1, root);
    }

    @Test
    public void testSddImportExport() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final Pair<SddNode, VTreeRoot> imp = SddReader.readSdd(new File("../test_files/sdd/big-swap.sdd"),
                new File("../test_files/sdd/big-swap.vtree"), sf);
        SddWriter.writeSdd(new File("../test_files/writers/temp/big-swap.sdd"),
                new File("../test_files/writers/temp/big-swap.vtree"), imp.getFirst(), imp.getSecond());
        final Pair<SddNode, VTreeRoot> imp2 = SddReader.readSdd(new File("../test_files/writers/temp/big-swap.sdd"),
                new File("../test_files/writers/temp/big-swap.vtree"), sf);
        assert imp.getFirst() == imp2.getFirst();
    }

    private void testSddFile(final String fileName, final SddNode sdd, final VTreeRoot root) throws IOException {
        final File temp = new File("../test_files/writers/temp/" + fileName + ".sdd");
        SddWriter.writeSdd(temp, new File("../test_files/writers/temp/" + fileName + "dump.vtree"), sdd, root);
        final File expected = new File("../test_files/writers/sdd/" + fileName + ".sdd");
        assertFilesEqual(expected, temp);
    }


    private void assertFilesEqual(final File expected, final File actual) throws IOException {
        final SoftAssertions softly = new SoftAssertions();
        final BufferedReader expReader = new BufferedReader(new FileReader(expected));
        final BufferedReader actReader = new BufferedReader(new FileReader(actual));
        for (int lineNumber = 1; expReader.ready() && actReader.ready(); lineNumber++) {
            softly.assertThat(actReader.readLine()).as("Line " + lineNumber + " not equal")
                    .isEqualTo(expReader.readLine());
        }
        if (expReader.ready()) {
            softly.fail("Missing line(s) found, starting with \"" + expReader.readLine() + "\"");
        }
        if (actReader.ready()) {
            softly.fail("Additional line(s) found, starting with \"" + actReader.readLine() + "\"");
        }
        softly.assertAll();
    }
}
