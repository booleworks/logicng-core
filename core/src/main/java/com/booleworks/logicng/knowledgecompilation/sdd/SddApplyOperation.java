package com.booleworks.logicng.knowledgecompilation.sdd;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;

public enum SddApplyOperation {
    CONJUNCTION,
    DISJUNCTION;

    public SddNodeTerminal zero(final SddFactory sf) {
        switch (this) {
            case CONJUNCTION:
                return sf.falsum();
            case DISJUNCTION:
                return sf.verum();
        }
        throw new RuntimeException("Unsupported operation");
    }

    public SddNodeTerminal one(final SddFactory sf) {
        switch (this) {
            case CONJUNCTION:
                return sf.verum();
            case DISJUNCTION:
                return sf.falsum();
        }
        throw new RuntimeException("Unsupported operation");
    }

    public boolean isZero(final SddNode node) {
        switch (this) {
            case CONJUNCTION:
                return node instanceof SddNodeTerminal
                        && ((SddNodeTerminal) node).getTerminal().getType() == FType.FALSE;
            case DISJUNCTION:
                return node instanceof SddNodeTerminal
                        && ((SddNodeTerminal) node).getTerminal().getType() == FType.TRUE;
        }
        throw new RuntimeException("Unsupported operation");
    }

    public boolean isOne(final SddNode node) {
        switch (this) {
            case CONJUNCTION:
                return node instanceof SddNodeTerminal
                        && ((SddNodeTerminal) node).getTerminal().getType() == FType.TRUE;
            case DISJUNCTION:
                return node instanceof SddNodeTerminal
                        && ((SddNodeTerminal) node).getTerminal().getType() == FType.FALSE;
        }
        throw new RuntimeException("Unsupported operation");
    }
}
