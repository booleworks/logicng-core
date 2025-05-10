package com.booleworks.logicng.knowledgecompilation.sdd;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;

public enum SddApplyOperation {
    CONJUNCTION,
    DISJUNCTION;

    public SddNodeTerminal zero(final Sdd sf) {
        switch (this) {
            case CONJUNCTION:
                return sf.falsum();
            case DISJUNCTION:
                return sf.verum();
        }
        throw new RuntimeException("Unsupported operation");
    }

    public SddNodeTerminal one(final Sdd sf) {
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
                        && node.isFalse();
            case DISJUNCTION:
                return node instanceof SddNodeTerminal
                        && node.isTrue();
        }
        throw new RuntimeException("Unsupported operation");
    }

    public boolean isOne(final SddNode node) {
        switch (this) {
            case CONJUNCTION:
                return node instanceof SddNodeTerminal
                        && node.isTrue();
            case DISJUNCTION:
                return node instanceof SddNodeTerminal
                        && node.isFalse();
        }
        throw new RuntimeException("Unsupported operation");
    }
}
