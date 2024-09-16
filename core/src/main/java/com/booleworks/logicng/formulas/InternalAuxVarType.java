package com.booleworks.logicng.formulas;

/**
 * Auxiliary variable types
 */
public enum InternalAuxVarType {
    CC("CC"),
    PBC("PB"),
    CNF("CNF");

    private final String prefix;

    /**
     * Constructs a new auxiliary variable type with a given string prefix
     */
    InternalAuxVarType(final String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix of this type.
     * @return the prefix of this type
     */
    public String getPrefix() {
        return prefix;
    }
}
