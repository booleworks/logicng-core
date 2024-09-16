// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.serialization.ProtoBufDnnf.PbDnnf;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Serialization methods for DNNFs.
 * @version 3.0.0
 * @since 2.5.0
 */
public interface Dnnfs {

    /**
     * Serializes a DNNF to a protocol buffer.
     * @param dnnf the DNNF
     * @return the protocol buffer
     */
    static PbDnnf serializeDnnf(final Dnnf dnnf) {
        final PbDnnf.Builder builder = PbDnnf.newBuilder();
        builder.setFormula(Formulas.serializeFormula(dnnf.getFormula()));
        builder.addAllOriginalVariables(dnnf.getOriginalVariables().stream().map(Variable::getName).collect(Collectors.toList()));
        return builder.build();
    }

    /**
     * Deserializes a DNNF from a protocol buffer.
     * @param f   the formula factory to generate the DNNF's formula
     * @param bin the protocol buffer
     * @return the DNNF
     */
    static Dnnf deserializeDnnf(final FormulaFactory f, final PbDnnf bin) {
        final SortedSet<Variable> vars = bin.getOriginalVariablesList().stream()
                .map(f::variable)
                .collect(Collectors.toCollection(TreeSet::new));
        return new Dnnf(vars, Formulas.deserializeFormula(f, bin.getFormula()));
    }
}
