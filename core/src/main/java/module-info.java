module logicng.core {
    exports com.booleworks.logicng.backbones;

    exports com.booleworks.logicng.encodings;
    exports com.booleworks.logicng.encodings.cc;
    exports com.booleworks.logicng.encodings.pbc;

    exports com.booleworks.logicng.collections;

    exports com.booleworks.logicng.configurations;

    exports com.booleworks.logicng.datastructures;
    exports com.booleworks.logicng.datastructures.ubtrees;

    exports com.booleworks.logicng.explanations;
    exports com.booleworks.logicng.explanations.mus;
    exports com.booleworks.logicng.explanations.smus;

    exports com.booleworks.logicng.formulas;
    exports com.booleworks.logicng.formulas.implementation.cached;
    exports com.booleworks.logicng.formulas.implementation.noncaching;
    exports com.booleworks.logicng.formulas.cache;
    exports com.booleworks.logicng.formulas.printer;

    exports com.booleworks.logicng.functions;

    exports com.booleworks.logicng.graphs.algorithms;
    exports com.booleworks.logicng.graphs.datastructures;
    exports com.booleworks.logicng.graphs.generators;
    exports com.booleworks.logicng.graphs.io;

    exports com.booleworks.logicng.handlers;

    exports com.booleworks.logicng.io.graphical;
    exports com.booleworks.logicng.io.parsers;
    exports com.booleworks.logicng.io.readers;
    exports com.booleworks.logicng.io.writers;

    exports com.booleworks.logicng.knowledgecompilation.bdds;
    exports com.booleworks.logicng.knowledgecompilation.bdds.datastructures;
    exports com.booleworks.logicng.knowledgecompilation.bdds.functions;
    exports com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;
    exports com.booleworks.logicng.knowledgecompilation.bdds.orderings;
    exports com.booleworks.logicng.knowledgecompilation.dnnf;
    exports com.booleworks.logicng.knowledgecompilation.dnnf.datastructures;
    exports com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;
    exports com.booleworks.logicng.knowledgecompilation.dnnf.functions;
    exports com.booleworks.logicng.knowledgecompilation.sdd.compilers;
    exports com.booleworks.logicng.knowledgecompilation.sdd.functions;
    exports com.booleworks.logicng.knowledgecompilation.sdd.datastructures;
    exports com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration;
    exports com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

    exports com.booleworks.logicng.modelcounting;

    exports com.booleworks.logicng.np;

    exports com.booleworks.logicng.predicates;
    exports com.booleworks.logicng.predicates.satisfiability;

    exports com.booleworks.logicng.primecomputation;

    exports com.booleworks.logicng.propositions;

    exports com.booleworks.logicng.solvers;
    exports com.booleworks.logicng.solvers.datastructures;
    exports com.booleworks.logicng.solvers.functions;
    exports com.booleworks.logicng.solvers.functions.modelenumeration;
    exports com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider;
    exports com.booleworks.logicng.solvers.sat;
    exports com.booleworks.logicng.solvers.maxsat.algorithms;
    exports com.booleworks.logicng.solvers.maxsat.encodings;

    exports com.booleworks.logicng.transformations;
    exports com.booleworks.logicng.transformations.cnf;
    exports com.booleworks.logicng.transformations.dnf;
    exports com.booleworks.logicng.transformations.qe;
    exports com.booleworks.logicng.transformations.simplification;

    exports com.booleworks.logicng.util;

    opens com.booleworks.logicng.solvers.sat;
    exports com.booleworks.logicng.handlers.events;
}
