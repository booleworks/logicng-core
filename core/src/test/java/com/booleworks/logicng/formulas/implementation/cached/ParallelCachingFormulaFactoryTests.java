package com.booleworks.logicng.formulas.implementation.cached;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.FormulaReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ParallelCachingFormulaFactoryTests {
    @Test
    @LongRunningTag
    public void testThreadSafeParse() throws IOException, InterruptedException {
        final List<String> formulaStrings = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader("../test_files/formulas/formula3.txt"))) {
            while (br.ready()) {
                formulaStrings.add(br.readLine());
            }
        }

        final List<Integer> threadCounts = List.of(1, 2, 4, 8);
        final int iterations = 100;
        for (final int threadCount : threadCounts) {
            for (int iter = 0; iter < iterations; ++iter) {
                final ConcurrentHashMap<String, Formula> allFormulas = new ConcurrentHashMap<>();
                final CachingFormulaFactory f =
                        FormulaFactory.caching(FormulaFactoryConfig.builder().threadSafe(true).build());
                final List<ParseFormulaRunnable> jobs = new ArrayList<>(threadCount);
                final List<Thread> threads = new ArrayList<>(threadCount);
                for (int i = 0; i < threadCount; ++i) {
                    final ParseFormulaRunnable job = new ParseFormulaRunnable(f, formulaStrings, allFormulas);
                    jobs.add(job);
                    threads.add(new Thread(job));
                }
                for (final Thread thread : threads) {
                    thread.start();
                }
                for (final Thread thread : threads) {
                    thread.join();
                }
                final List<String> allAssertionFailures = new ArrayList<>();
                for (final ParseFormulaRunnable job : jobs) {
                    allAssertionFailures.addAll(job.assertionFailures);
                }
                assertThat(allAssertionFailures).isEmpty();
            }
        }
    }

    @Test
    @LongRunningTag
    public void testThreadSafeParseAndConstruct() throws IOException, InterruptedException, ParserException {
        final CachingFormulaFactory preF = FormulaFactory.caching();
        FormulaReader.readFormula(preF, "../test_files/formulas/formula3.txt");
        final List<LinkedHashSet<Literal>> clauses = extractClauses(preF);
        final List<LinkedHashSet<Formula>> cnfs = extractCnfs(preF);

        final FormulaFactoryConfig config = FormulaFactoryConfig.builder()
                .threadSafe(true)
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT)
                .build();

        final List<String> formulaStrings = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader("../test_files/formulas/formula3.txt"))) {
            while (br.ready()) {
                formulaStrings.add(br.readLine());
            }
        }

        final List<Integer> threadCounts = List.of(1, 2, 4);
        final int iterations = 100;
        for (final int threadCount : threadCounts) {
            for (int iter = 0; iter < iterations; ++iter) {
                final ConcurrentHashMap<String, Formula> allFormulas = new ConcurrentHashMap<>();
                final ConcurrentHashMap<LinkedHashSet<Literal>, Formula> allClauses = new ConcurrentHashMap<>();
                final ConcurrentHashMap<LinkedHashSet<Formula>, Formula> allCnfs = new ConcurrentHashMap<>();
                final CachingFormulaFactory f = FormulaFactory.caching(config);
                final List<Runnable> jobs = new ArrayList<>(threadCount);
                final List<Thread> threads = new ArrayList<>(threadCount);
                for (int i = 0; i < threadCount; ++i) {
                    final ParseFormulaRunnable parseJob =
                            new ParseFormulaRunnable(f, shuffle(formulaStrings, iter), allFormulas);
                    final ConstructClausesRunnable clauseJob =
                            new ConstructClausesRunnable(f, shuffle(clauses, iter + 1), allClauses);
                    final ConstructCnfsRunnable cnfJob = new ConstructCnfsRunnable(f, shuffle(cnfs, iter + 2), allCnfs);
                    jobs.add(parseJob);
                    jobs.add(clauseJob);
                    jobs.add(cnfJob);
                    threads.add(new Thread(parseJob));
                    threads.add(new Thread(clauseJob));
                    threads.add(new Thread(cnfJob));
                }
                for (final Thread thread : threads) {
                    thread.start();
                }
                for (final Thread thread : threads) {
                    thread.join();
                }
                final List<String> allAssertionFailures = new ArrayList<>();
                for (final Runnable job : jobs) {
                    if (job instanceof ParseFormulaRunnable) {
                        allAssertionFailures.addAll(((ParseFormulaRunnable) job).assertionFailures);
                    } else if (job instanceof ConstructClausesRunnable) {
                        allAssertionFailures.addAll(((ConstructClausesRunnable) job).assertionFailures);
                    } else if (job instanceof ConstructCnfsRunnable) {
                        allAssertionFailures.addAll(((ConstructCnfsRunnable) job).assertionFailures);
                    }
                }
                assertThat(allAssertionFailures).isEmpty();
            }
        }
    }

    private static List<LinkedHashSet<Literal>> extractClauses(final CachingFormulaFactory f) {
        final List<LinkedHashSet<Literal>> allClauses = new ArrayList<>();
        final List<Map<LinkedHashSet<? extends Formula>, Or>> caches = List.of(f.ors2, f.ors3, f.ors4, f.orsN);
        for (final Map<LinkedHashSet<? extends Formula>, Or> cache : caches) {
            allClauses.addAll(
                    cache.values().stream().filter(Or::isCnfClause)
                            .map(clause -> new LinkedHashSet<>(clause.literals(f)))
                            .collect(Collectors.toList())
            );
        }
        return allClauses;
    }

    private static List<LinkedHashSet<Formula>> extractCnfs(final CachingFormulaFactory f) {
        final List<LinkedHashSet<Formula>> allCnfs = new ArrayList<>();
        final List<Map<LinkedHashSet<? extends Formula>, And>> caches = List.of(f.ands2, f.ands3, f.ands4, f.andsN);
        for (final Map<LinkedHashSet<? extends Formula>, And> cache : caches) {
            allCnfs.addAll(
                    cache.values().stream().filter(formula -> formula.isCnf(f))
                            .map(formula -> new LinkedHashSet<>(((And) formula).getOperands()))
                            .collect(Collectors.toList())
            );
        }
        return allCnfs;
    }

    private static <T> List<T> shuffle(final List<T> list, final long seed) {
        final Random random = new Random(seed);
        final List<T> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled, random);
        return shuffled;
    }

    private static class ParseFormulaRunnable implements Runnable {
        final private FormulaFactory f;
        final private List<String> unparsedFormulas;
        final private ConcurrentHashMap<String, Formula> allFormulas;
        final List<String> assertionFailures = new ArrayList<>();

        public ParseFormulaRunnable(final FormulaFactory f, final List<String> unparsedFormulas,
                                    final ConcurrentHashMap<String, Formula> allFormulas) {
            this.f = f;
            this.unparsedFormulas = unparsedFormulas;
            this.allFormulas = allFormulas;
        }

        @Override
        public void run() {
            final FormulaParser parser = new PropositionalParser(f);
            for (final String unparsedFormula : unparsedFormulas) {
                try {
                    final Formula formula = parser.parse(unparsedFormula);
                    final Formula existingFormula = allFormulas.putIfAbsent(unparsedFormula, formula);
                    if (existingFormula != null && existingFormula != formula) {
                        assertionFailures.add(String.format("Duplicated formula object for %s", unparsedFormula));
                    }
                } catch (final ParserException e) {
                    assertionFailures.add(String.format("Exception in thread: " + e.getMessage()));
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class ConstructClausesRunnable implements Runnable {
        final private FormulaFactory f;
        final private List<LinkedHashSet<Literal>> clauses;
        final private ConcurrentHashMap<LinkedHashSet<Literal>, Formula> allClauses;
        final List<String> assertionFailures = new ArrayList<>();

        public ConstructClausesRunnable(final FormulaFactory f, final List<LinkedHashSet<Literal>> clauses,
                                        final ConcurrentHashMap<LinkedHashSet<Literal>, Formula> allClauses) {
            this.f = f;
            this.clauses = clauses;
            this.allClauses = allClauses;
        }

        @Override
        public void run() {
            for (final LinkedHashSet<Literal> literals : clauses) {
                final Formula clause = f.clause(literals);
                final Formula existingFormula = allClauses.putIfAbsent(literals, clause);
                if (existingFormula != null && existingFormula != clause) {
                    assertionFailures.add(String.format("Duplicated clause object for %s", literals));
                }
            }
        }
    }

    private static class ConstructCnfsRunnable implements Runnable {
        final private FormulaFactory f;
        final private List<LinkedHashSet<Formula>> cnfs;
        final private ConcurrentHashMap<LinkedHashSet<Formula>, Formula> allCnfs;
        final List<String> assertionFailures = new ArrayList<>();

        public ConstructCnfsRunnable(final FormulaFactory f, final List<LinkedHashSet<Formula>> cnfs,
                                     final ConcurrentHashMap<LinkedHashSet<Formula>, Formula> allCnfs) {
            this.f = f;
            this.cnfs = cnfs;
            this.allCnfs = allCnfs;
        }

        @Override
        public void run() {
            for (final LinkedHashSet<Formula> clauses : cnfs) {
                final Formula cnf = f.cnf(clauses);
                final Formula existingFormula = allCnfs.putIfAbsent(clauses, cnf);
                if (existingFormula != null && existingFormula != cnf) {
                    assertionFailures.add(String.format("Duplicated clause object for %s", clauses));
                }
            }
        }
    }
}
