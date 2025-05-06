package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
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

    public CompactModel with(final Collection<Literal> literals, final Collection<Variable> dontCareVariables) {
        final CompactModel copy = new CompactModel(this);
        copy.literals.addAll(literals);
        copy.dontCareVariables.addAll(dontCareVariables);
        return copy;
    }

    public CompactModel withLiterals(final Collection<Literal> literals) {
        final CompactModel copy = new CompactModel(this);
        copy.literals.addAll(literals);
        return copy;
    }

    public CompactModel withDontCare(final Collection<Variable> dontCareVariables) {
        final CompactModel copy = new CompactModel(this);
        copy.dontCareVariables.addAll(dontCareVariables);
        return copy;
    }

    public List<Literal> getLiterals() {
        return literals;
    }

    public List<Variable> getDontCareVariables() {
        return dontCareVariables;
    }

    public List<Model> expand() {
        List<List<Literal>> result = List.of(literals);
        for (final Variable var : dontCareVariables) {
            final List<List<Literal>> extended = new ArrayList<>(result.size() * 2);
            for (final List<Literal> literals : result) {
                extended.add(extendedByLiteral(literals, var));
                extended.add(extendedByLiteral(literals, var.negate(var.getFactory())));
            }
            result = extended;
        }
        final List<Model> models = new ArrayList<>(result.size());
        for (final List<Literal> lits : result) {
            models.add(new Model(lits));
        }
        return models;
    }

    private static List<Literal> extendedByLiteral(final List<Literal> literals, final Literal lit) {
        final ArrayList<Literal> extended = new ArrayList<>(literals);
        extended.add(lit);
        return extended;
    }
}
