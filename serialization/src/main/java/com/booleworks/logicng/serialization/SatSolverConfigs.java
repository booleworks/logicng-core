// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import com.booleworks.logicng.serialization.ProtoBufSolverCommons.PbClauseMinimization;
import com.booleworks.logicng.serialization.ProtoBufSolverCommons.PbCnfMethod;
import com.booleworks.logicng.serialization.ProtoBufSolverCommons.PbSatSolverConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization;
import com.booleworks.logicng.solvers.sat.SatSolverConfig.CnfMethod;
import com.booleworks.logicng.solvers.sat.SatSolverLowLevelConfig;

/**
 * Serialization methods for SAT solver configurations.
 * @version 3.0.0
 * @since 2.5.0
 */
public interface SatSolverConfigs {

    /**
     * Serializes a SAT solver configuration to a protocol buffer.
     * @param config the configuration
     * @return the protocol buffer
     */
    static PbSatSolverConfig serializeSatSolverConfig(final SatSolverConfig config) {
        return PbSatSolverConfig.newBuilder()
                .setProofGeneration(config.isProofGeneration())
                .setUseAtMostClauses(config.isUseAtMostClauses())
                .setCnfMethod(serializeCnfMode(config.getCnfMethod()))
                .setClauseMinimization(serializeMinMode(config.getClauseMinimization()))
                .setInitialPhase(config.getInitialPhase())

                .setVarDecay(config.getLowLevelConfig().getVarDecay())
                .setVarInc(config.getLowLevelConfig().getVarInc())
                .setRestartFirst(config.getLowLevelConfig().getRestartFirst())
                .setRestartInc(config.getLowLevelConfig().getRestartInc())
                .setClauseDecay(config.getLowLevelConfig().getClauseDecay())

                .setLbLbdMinimizingClause(config.getLowLevelConfig().getLbLbdMinimizingClause())
                .setLbLbdFrozenClause(config.getLowLevelConfig().getLbLbdFrozenClause())
                .setLbSizeMinimizingClause(config.getLowLevelConfig().getLbSizeMinimizingClause())
                .setFirstReduceDb(config.getLowLevelConfig().getFirstReduceDb())
                .setSpecialIncReduceDb(config.getLowLevelConfig().getSpecialIncReduceDb())
                .setIncReduceDb(config.getLowLevelConfig().getIncReduceDb())
                .setFactorK(config.getLowLevelConfig().getFactorK())
                .setFactorR(config.getLowLevelConfig().getFactorR())
                .setSizeLbdQueue(config.getLowLevelConfig().getSizeLbdQueue())
                .setSizeTrailQueue(config.getLowLevelConfig().getSizeTrailQueue())
                .setReduceOnSize(config.getLowLevelConfig().isReduceOnSize())
                .setReduceOnSizeSize(config.getLowLevelConfig().getReduceOnSizeSize())
                .setMaxVarDecay(config.getLowLevelConfig().getMaxVarDecay())

                .build();
    }

    /**
     * Deserializes a SAT solver from a protocol buffer.
     * @param bin the protocol buffer
     * @return the configuration
     */
    static SatSolverConfig deserializeSatSolverConfig(final PbSatSolverConfig bin) {
        final var llConfig = SatSolverLowLevelConfig.builder()
                .varDecay(bin.getVarDecay())
                .varInc(bin.getVarInc())
                .restartFirst(bin.getRestartFirst())
                .restartInc(bin.getRestartInc())
                .clauseDecay(bin.getClauseDecay())

                .lbLbdMinimizingClause(bin.getLbLbdMinimizingClause())
                .lbLbdFrozenClause(bin.getLbLbdFrozenClause())
                .lbSizeMinimizingClause(bin.getLbSizeMinimizingClause())
                .firstReduceDb(bin.getFirstReduceDb())
                .specialIncReduceDb(bin.getSpecialIncReduceDb())
                .incReduceDb(bin.getIncReduceDb())
                .factorK(bin.getFactorK())
                .factorR(bin.getFactorR())
                .sizeLbdQueue(bin.getSizeLbdQueue())
                .sizeTrailQueue(bin.getSizeTrailQueue())
                .reduceOnSize(bin.getReduceOnSize())
                .reduceOnSizeSize(bin.getReduceOnSizeSize())
                .maxVarDecay(bin.getMaxVarDecay())

                .build();

        return SatSolverConfig.builder()
                .proofGeneration(bin.getProofGeneration())
                .useAtMostClauses(bin.getUseAtMostClauses())
                .cnfMethod(deserializeCnfMode(bin.getCnfMethod()))
                .clauseMinimization(deserializeMinMode(bin.getClauseMinimization()))
                .initialPhase(bin.getInitialPhase())
                .lowLevelConfig(llConfig)
                .build();
    }

    /**
     * Serializes the clause minimization algorithm to a protocol buffer.
     * @param minimization the algorithm
     * @return the protocol buffer
     */
    static PbClauseMinimization serializeMinMode(final ClauseMinimization minimization) {
        switch (minimization) {
            case NONE:
                return PbClauseMinimization.NONE;
            case BASIC:
                return PbClauseMinimization.BASIC;
            case DEEP:
                return PbClauseMinimization.DEEP;
            default:
                throw new IllegalArgumentException("Unknown clause minimization: " + minimization);
        }
    }

    /**
     * Deserializes the clause minimization algorithm from a protocol buffer.
     * @param bin the protocol buffer
     * @return the algorithm
     */
    static ClauseMinimization deserializeMinMode(final PbClauseMinimization bin) {
        switch (bin) {
            case NONE:
                return ClauseMinimization.NONE;
            case BASIC:
                return ClauseMinimization.BASIC;
            case DEEP:
                return ClauseMinimization.DEEP;
            default:
                throw new IllegalArgumentException("Unknown clause minimization: " + bin);
        }
    }

    /**
     * Serializes the CNF algorithm to a protocol buffer.
     * @param cnf the algorithm
     * @return the protocol buffer
     */
    static PbCnfMethod serializeCnfMode(final CnfMethod cnf) {
        switch (cnf) {
            case FACTORY_CNF:
                return PbCnfMethod.FACTORY_CNF;
            case PG_ON_SOLVER:
                return PbCnfMethod.PG_ON_SOLVER;
            case FULL_PG_ON_SOLVER:
                return PbCnfMethod.FULL_PG_ON_SOLVER;
            default:
                throw new IllegalArgumentException("Unknown CNF method: " + cnf);
        }
    }

    /**
     * Deserializes the CNF algorithm from a protocol buffer.
     * @param bin the protocol buffer
     * @return the algorithm
     */
    static CnfMethod deserializeCnfMode(final PbCnfMethod bin) {
        switch (bin) {
            case FACTORY_CNF:
                return CnfMethod.FACTORY_CNF;
            case PG_ON_SOLVER:
                return CnfMethod.PG_ON_SOLVER;
            case FULL_PG_ON_SOLVER:
                return CnfMethod.FULL_PG_ON_SOLVER;
            default:
                throw new IllegalArgumentException("Unknown CNF method: " + bin);
        }
    }
}
