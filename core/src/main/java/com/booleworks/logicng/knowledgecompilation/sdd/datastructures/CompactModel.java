package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.List;

public class CompactModel {
    private final List<Literal> literals;
    private final List<Variable> dontCareVariables;

    public CompactModel(final List<Literal> literals, final List<Variable> dontCareVariables) {
        this.literals = literals;
        this.dontCareVariables = dontCareVariables;
    }

    public CompactModel(final CompactModel model) {
        this.literals = new ArrayList<>(model.literals);
        this.dontCareVariables = new ArrayList<>(model.dontCareVariables);
    }

    public List<Literal> getLiterals() {
        return literals;
    }

    public List<Variable> getDontCareVariables() {
        return dontCareVariables;
    }
}
