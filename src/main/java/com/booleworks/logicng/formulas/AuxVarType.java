package com.booleworks.logicng.formulas;

/**
 * Auxiliary variable types
 */
public enum AuxVarType {
    CC("@RESERVED_CC_"),
    PBC("@RESERVED_PB_"),
    CNF("@RESERVED_CNF_");

    private final String prefix;

    /**
     * Constructs a new auxiliary variable type with a given string prefix
     */
    AuxVarType(final String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix of this type.
     * @return the prefix of this type
     */
    public String prefix() {
        return prefix;
    }
}
