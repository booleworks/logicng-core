// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Copyright (c) 2014-2015, Marijn Heule and Nathan Wetzler Last edit, March 4,
 * 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.booleworks.logicng.explanations.drup;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the DRUP-trim tool to check satisfiability proofs and
 * perform trimming.
 * @version 3.0.0
 * @since 1.3
 */
public final class DrupTrim {

    private static final int BIGINIT = 1000000;
    private static final int UNSAT = 0;
    private static final int SAT = 1;
    private static final int EXTRA = 2;
    private static final int MARK = 3;

    private static int getHash(final int[] _marks, final int mark, final LngIntVector input) {
        int sum = 0;
        int xor = 0;
        int prod = 1;
        for (int i = 0; i < input.size(); i++) {
            prod *= input.get(i);
            sum += input.get(i);
            xor ^= input.get(i);
            _marks[index(input.get(i))] = mark;
        }
        return Math.abs((1023 * sum + prod ^ (31 * xor)) % BIGINIT);
    }

    private static int index(final int lit) {
        return lit > 0 ? lit * 2 : (-lit * 2) ^ 1;
    }

    /**
     * Computes the DRUP result for a given problem in terms of original clauses
     * and the generated proof.
     * @param originalProblem the clauses of the original problem
     * @param proof           the clauses of the proof
     * @return the result of the DRUP execution from which the unsatisfiable
     * core can be generated
     */
    public DrupResult compute(final LngVector<LngIntVector> originalProblem, final LngVector<LngIntVector> proof) {
        final Solver s = new Solver(originalProblem, proof);
        final boolean parseReturnValue = s.parse();
        return parseReturnValue
                ? new DrupResult(false, s.verify())
                : new DrupResult(true, new LngVector<>());
    }

    private static class Solver {

        private final LngVector<LngIntVector> originalProblem;
        private final LngVector<LngIntVector> proof;
        private final LngVector<LngIntVector> core;
        private final boolean delete;
        private LngIntVector db;
        private int[] falseStack;
        private int[] reason;
        private int[] internalFalse;
        private int forcedPtr;
        private int processedPtr;
        private int assignedPtr;
        private LngIntVector adlist;
        private LngIntVector[] wlist;
        private int count;
        private int adlemmas;
        private int lemmas;

        private Solver(final LngVector<LngIntVector> originalProblem, final LngVector<LngIntVector> proof) {
            this.originalProblem = originalProblem;
            this.proof = proof;
            core = new LngVector<>();
            delete = true;
        }

        private void assign(final int a) {
            internalFalse[index(-a)] = 1;
            falseStack[assignedPtr++] = -a;
        }

        private void addWatch(final int cPtr, final int index) {
            final int lit = db.get(cPtr + index);
            wlist[index(lit)].push((cPtr << 1));
        }

        private void addWatchLit(final int l, final int m) {
            wlist[index(l)].push(m);
        }

        private void removeWatch(final int cPtr, final int index) {
            final int lit = db.get(cPtr + index);
            final LngIntVector watch = wlist[index(lit)];
            int watchPtr = 0;
            while (true) {
                final int _cPtr = watch.get(watchPtr++) >> 1;
                if (_cPtr == cPtr) {
                    watch.set(watchPtr - 1, wlist[index(lit)].back());
                    wlist[index(lit)].pop();
                    return;
                }
            }
        }

        private void markWatch(final int clausePtr, final int index, final int offset) {
            final LngIntVector watch = wlist[index(db.get(clausePtr + index))];
            final int clause = db.get(clausePtr - offset - 1);
            int watchPtr = 0;
            while (true) {
                final int _clause = (db.get((watch.get(watchPtr++) >> 1) - 1));
                if (_clause == clause) {
                    watch.set(watchPtr - 1, watch.get(watchPtr - 1) | 1);
                    return;
                }
            }
        }

        private void markClause(int clausePtr, final int index) {
            if ((db.get(clausePtr + index - 1) & 1) == 0) {
                db.set(clausePtr + index - 1, db.get(clausePtr + index - 1) | 1);
                if (db.get(clausePtr + 1 + index) == 0) {
                    return;
                }
                markWatch(clausePtr, index, -index);
                markWatch(clausePtr, 1 + index, -index);
            }
            while (db.get(clausePtr) != 0) {
                internalFalse[index(db.get(clausePtr++))] = MARK;
            }
        }

        private void analyze(final int clausePtr) {
            markClause(clausePtr, 0);
            while (assignedPtr > 0) {
                final int lit = falseStack[--assignedPtr];
                if ((internalFalse[index(lit)] == MARK) && reason[Math.abs(lit)] != 0) {
                    markClause(reason[Math.abs(lit)], -1);
                }
                internalFalse[index(lit)] = assignedPtr < forcedPtr ? 1 : 0;
            }
            processedPtr = forcedPtr;
            assignedPtr = forcedPtr;
        }

        private int propagate() {
            final int[] start = new int[2];
            int check = 0;
            int i;
            int lit;
            int _lit = 0;
            LngIntVector watch;
            int _watchPtr = 0;
            start[0] = processedPtr;
            start[1] = processedPtr;
            boolean gotoFlipCheck = true;
            while (gotoFlipCheck) {
                gotoFlipCheck = false;
                check ^= 1;
                // While unprocessed false literals
                while (!gotoFlipCheck && start[check] < assignedPtr) {
                    // Get first unprocessed literal
                    lit = falseStack[start[check]++];
                    // Obtain the first watch pointer
                    watch = wlist[index(lit)];
                    int watchPtr = lit == _lit ? _watchPtr : 0;

                    // While there are watched clauses (watched by lit)
                    while (watchPtr < watch.size()) {
                        if ((watch.get(watchPtr) & 1) != check) {
                            watchPtr++;
                            continue;
                        }
                        // Get the clause from DB
                        final int clausePtr = watch.get(watchPtr) / 2;
                        if (internalFalse[index(-db.get(clausePtr))] != 0 ||
                                internalFalse[index(-db.get(clausePtr + 1))] != 0) {
                            watchPtr++;
                            continue;
                        }
                        if (db.get(clausePtr) == lit) {
                            // Ensure that the other watched literal is in front
                            db.set(clausePtr, db.get(clausePtr + 1));
                        }
                        boolean gotoNextClause = false;
                        // Scan the non-watched literals
                        for (i = 2; db.get(clausePtr + i) != 0; i++) {
                            // When clause[j] is not false, it is either true
                            // or unset
                            if (internalFalse[index(db.get(clausePtr + i))] == 0) {
                                db.set(clausePtr + 1, db.get(clausePtr + i));
                                // Swap literals
                                db.set(clausePtr + i, lit);
                                // Add the watch to the list of clause[1]
                                addWatchLit(db.get(clausePtr + 1), watch.get(watchPtr));
                                // Remove pointer
                                watch.set(watchPtr, wlist[index(lit)].back());
                                wlist[index(lit)].pop();
                                gotoNextClause = true;
                                break;
                            } // go to the next watched clause
                        }
                        if (!gotoNextClause) {
                            db.set(clausePtr + 1, lit);
                            // Set lit at clause[1] and set next watch
                            watchPtr++;

                            // If the other watched literal is falsified,
                            if (internalFalse[index(db.get(clausePtr))] == 0) {
                                // A unit clause is found, and the reason is set
                                assign(db.get(clausePtr));
                                reason[Math.abs(db.get(clausePtr))] = clausePtr + 1;
                                if (check == 0) {
                                    start[0]--;
                                    _lit = lit;
                                    _watchPtr = watchPtr;
                                    gotoFlipCheck = true;
                                    break;
                                }
                            } else {
                                analyze(clausePtr);
                                return UNSAT;
                            } // Found a root level conflict -> UNSAT
                        }
                    }
                } // Set position for next clause
                if (check != 0) {
                    gotoFlipCheck = true;
                }
            }
            processedPtr = assignedPtr;
            return SAT;
        }

        int matchClause(final LngIntVector clauselist, final int[] _marks, final int mark, final LngIntVector input) {
            int i;
            int matchsize;
            for (i = 0; i < clauselist.size(); i++) {
                matchsize = 0;
                boolean aborted = false;
                for (int l = clauselist.get(i); db.get(l) != 0; l++) {
                    if (_marks[index(db.get(l))] != mark) {
                        aborted = true;
                        break;
                    }
                    matchsize++;
                }
                if (!aborted && input.size() == matchsize) {
                    final int result = clauselist.get(i);
                    clauselist.set(i, clauselist.back());
                    return result;
                }
            }
            throw new IllegalStateException("Could not match deleted clause");
        }

        /**
         * Parses the input and returns {@code true} if further processing is
         * required and {@code false} if the formula is trivially UNSAT.
         * @return {@code true} if further processing is required and
         * {@code false} if the formula is trivially UNSAT
         */
        private boolean parse() {
            int nVars = 0;
            for (final LngIntVector vector : originalProblem) {
                for (int i = 0; i < vector.size(); i++) {
                    if (Math.abs(vector.get(i)) > nVars) {
                        nVars = Math.abs(vector.get(i));
                    }
                }
            }
            final int nClauses = originalProblem.size();

            boolean del = false;
            int nZeros = nClauses;
            final LngIntVector buffer = new LngIntVector();

            db = new LngIntVector();

            count = 1;
            falseStack = new int[nVars + 1];
            reason = new int[nVars + 1];
            internalFalse = new int[2 * nVars + 3];

            wlist = new LngIntVector[2 * nVars + 3];
            for (int i = 1; i <= nVars; ++i) {
                wlist[index(i)] = new LngIntVector();
                wlist[index(-i)] = new LngIntVector();
            }

            adlist = new LngIntVector();

            final int[] marks = new int[2 * nVars + 3];
            int mark = 0;

            final Map<Integer, LngIntVector> hashTable = new HashMap<>();
            LngVector<LngIntVector> currentFile = originalProblem;
            boolean fileSwitchFlag;
            int clauseNr = 0;
            while (true) {
                fileSwitchFlag = nZeros <= 0;
                if (clauseNr >= currentFile.size()) {
                    lemmas = db.size() + 1;
                    break;
                }
                final LngIntVector clause = currentFile.get(clauseNr++);
                final List<Integer> toks = new ArrayList<>(clause.size() - 1);
                if (fileSwitchFlag && clause.get(0) == -1) {
                    del = true;
                }
                for (int i = (fileSwitchFlag ? 1 : 0); i < clause.size(); i++) {
                    toks.add(clause.get(i));
                }
                for (final Integer l : toks) {
                    buffer.push(l);
                }
                if (clauseNr >= currentFile.size() && !fileSwitchFlag) {
                    fileSwitchFlag = true;
                    clauseNr = 0;
                    currentFile = proof;
                }
                if (clauseNr > currentFile.size() && fileSwitchFlag && !currentFile.isEmpty()) {
                    break;
                }
                final int hash = getHash(marks, ++mark, buffer);
                if (del) {
                    if (delete) {
                        final int match = matchClause(hashTable.get(hash), marks, mark, buffer);
                        hashTable.get(hash).pop();
                        adlist.push((match << 1) + 1);
                    }
                    del = false;
                    buffer.clear();
                    continue;
                }
                final int clausePtr = db.size() + 1;
                db.push(2 * count++);
                for (int i = 0; i < buffer.size(); i++) {
                    db.push(buffer.get(i));
                }
                db.push(0);

                LngIntVector vec = hashTable.get(hash);
                if (vec == null) {
                    vec = new LngIntVector();
                    hashTable.put(hash, vec);
                }
                vec.push(clausePtr);

                adlist.push(clausePtr << 1);

                if (nZeros == 0) {
                    lemmas = clausePtr;
                    adlemmas = adlist.size() - 1;
                }
                if (nZeros > 0) {
                    if (buffer.isEmpty() || ((buffer.size() == 1) && internalFalse[index(db.get(clausePtr))] != 0)) {
                        return false;
                    } else if (buffer.size() == 1) {
                        if (internalFalse[index(-db.get(clausePtr))] == 0) {
                            reason[Math.abs(db.get(clausePtr))] = clausePtr + 1;
                            assign(db.get(clausePtr));
                        }
                    } else {
                        addWatch(clausePtr, 0);
                        addWatch(clausePtr, 1);
                    }
                } else if (buffer.isEmpty()) {
                    break;
                }
                buffer.clear();
                --nZeros;
            }
            return true;
        }

        private LngVector<LngIntVector> verify() {
            int ad;
            long d;
            boolean flag = false;
            int clausePtr = 0;
            int lemmasPtr = lemmas;
            final int lastPtr = lemmas;
            final int endPtr = lemmas;
            int checked = adlemmas;
            final LngIntVector buffer = new LngIntVector();
            int time;

            boolean gotoPostProcess = false;
            if (processedPtr < assignedPtr) {
                if (propagate() == UNSAT) {
                    gotoPostProcess = true;
                }
            }
            forcedPtr = processedPtr;

            if (!gotoPostProcess) {
                boolean gotoVerification = false;
                while (!gotoVerification) {
                    flag = false;
                    buffer.clear();
                    time = db.get(lemmasPtr - 1);
                    clausePtr = lemmasPtr;
                    do {
                        ad = adlist.get(checked++);
                        d = ad & 1;
                        final int cPtr = ad >> 1;
                        if (d != 0 && db.get(cPtr + 1) != 0) {
                            if (reason[Math.abs(db.get(cPtr))] - 1 == ad >> 1) {
                                continue;
                            }
                            removeWatch(cPtr, 0);
                            removeWatch(cPtr, 1);
                        }
                    } while (d != 0);

                    while (db.get(lemmasPtr) != 0) {
                        final int lit = db.get(lemmasPtr++);
                        if (internalFalse[index(-lit)] != 0) {
                            flag = true;
                        }
                        if (internalFalse[index(lit)] == 0) {
                            if (buffer.size() <= 1) {
                                db.set(lemmasPtr - 1, db.get(clausePtr + buffer.size()));
                                db.set(clausePtr + buffer.size(), lit);
                            }
                            buffer.push(lit);
                        }
                    }

                    if (db.get(clausePtr + 1) != 0) {
                        addWatch(clausePtr, 0);
                        addWatch(clausePtr, 1);
                    }

                    lemmasPtr += EXTRA;

                    if (flag) {
                        adlist.set(checked - 1, 0);
                    }
                    if (flag) {
                        continue; // Clause is already satisfied
                    }
                    if (buffer.isEmpty()) {
                        throw new IllegalStateException("Conflict claimed, but not detected");
                    }

                    if (buffer.size() == 1) {
                        assign(buffer.get(0));
                        reason[Math.abs(buffer.get(0))] = clausePtr + 1;
                        forcedPtr = processedPtr;
                        if (propagate() == UNSAT) {
                            gotoVerification = true;
                        }
                    }

                    if (lemmasPtr >= db.size()) {
                        break;
                    }
                }
                if (!gotoVerification) {
                    throw new IllegalStateException("No conflict");
                }

                forcedPtr = processedPtr;
                lemmasPtr = clausePtr - EXTRA;

                while (true) {
                    buffer.clear();
                    clausePtr = lemmasPtr + EXTRA;
                    do {
                        ad = adlist.get(--checked);
                        d = ad & 1;
                        final int cPtr = ad >> 1;
                        if (d != 0 && db.get(cPtr + 1) != 0) {
                            if (reason[Math.abs(db.get(cPtr))] - 1 == ad >> 1) {
                                continue;
                            }
                            addWatch(cPtr, 0);
                            addWatch(cPtr, 1);
                        }
                    } while (d != 0);

                    time = db.get(clausePtr - 1);

                    if (db.get(clausePtr + 1) != 0) {
                        removeWatch(clausePtr, 0);
                        removeWatch(clausePtr, 1);
                    }

                    final boolean gotoNextLemma = ad == 0;

                    if (!gotoNextLemma) {
                        while (db.get(clausePtr) != 0) {
                            final int lit = db.get(clausePtr++);
                            if (internalFalse[index(-lit)] != 0) {
                                flag = true;
                            }
                            if (internalFalse[index(lit)] == 0) {
                                buffer.push(lit);
                            }
                        }

                        if (flag && buffer.size() == 1) {
                            do {
                                internalFalse[index(falseStack[--forcedPtr])] = 0;
                            } while (falseStack[forcedPtr] != -buffer.get(0));
                            processedPtr = forcedPtr;
                            assignedPtr = forcedPtr;
                        }

                        if ((time & 1) != 0) {
                            int i;
                            for (i = 0; i < buffer.size(); ++i) {
                                assign(-buffer.get(i));
                                reason[Math.abs(buffer.get(i))] = 0;
                            }
                            if (propagate() == SAT) {
                                throw new IllegalStateException("Formula is SAT");
                            }
                        }
                    }

                    if (lemmasPtr + EXTRA == lastPtr) {
                        break;
                    }
                    while (db.get(--lemmasPtr) != 0) {
                        // empty on purpose
                    }
                }
            }

            int marked;
            lemmasPtr = 0;
            while (lemmasPtr + EXTRA <= lastPtr) {
                if ((db.get(lemmasPtr++) & 1) != 0) {
                    count++;
                }
                while (db.get(lemmasPtr++) != 0) {
                    // empty on purpose
                }
            }
            lemmasPtr = 0;

            while (lemmasPtr + EXTRA <= lastPtr) {
                final LngIntVector coreVec = new LngIntVector();
                marked = db.get(lemmasPtr++) & 1;
                while (db.get(lemmasPtr) != 0) {
                    if (marked != 0) {
                        coreVec.push(db.get(lemmasPtr));
                    }
                    lemmasPtr++;
                }
                if (marked != 0) {
                    core.push(coreVec);
                }
                lemmasPtr++;
            }

            count = 0;
            while (lemmasPtr + EXTRA <= endPtr) {
                time = db.get(lemmasPtr);
                marked = db.get(lemmasPtr++) & 1;
                if (marked != 0) {
                    count++;
                }
                while (db.get(lemmasPtr) != 0) {
                    lemmasPtr++;
                }
                lemmasPtr++;
            }
            return core;
        }
    }

    /**
     * The result of an DRUP execution.
     */
    public static class DrupResult {
        private final boolean trivialUnsat;
        private final LngVector<LngIntVector> unsatCore;

        /**
         * Constructs a new DRUP result
         * @param trivialUnsat a flag whether the formula is trivially unsatisfiable
         * @param unsatCore    the unsatisfiable core
         */
        public DrupResult(final boolean trivialUnsat, final LngVector<LngIntVector> unsatCore) {
            this.trivialUnsat = trivialUnsat;
            this.unsatCore = unsatCore;
        }

        /**
         * Returns {@code true} if the formula was trivially unsatisfiable.
         * @return {@code true} if the formula was trivially unsatisfiable
         */
        public boolean isTrivialUnsat() {
            return trivialUnsat;
        }

        /**
         * Returns the unsatisfiable core of the formula.
         * @return the unsatisfiable core of the formula
         */
        public LngVector<LngIntVector> getUnsatCore() {
            return unsatCore;
        }
    }
}
