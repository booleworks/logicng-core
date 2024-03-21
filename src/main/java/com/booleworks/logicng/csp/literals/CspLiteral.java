package com.booleworks.logicng.csp.literals;

public interface CspLiteral {
    boolean isValid();

    boolean isUnsat();
}
