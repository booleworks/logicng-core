package com.booleworks.logicng.solvers.maxsat.algorithms;

import com.booleworks.logicng.solvers.MaxSatSolver;

import java.util.Arrays;
import java.util.Objects;

/**
 * The state of a {@link MaxSatSolver}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class MaxSatState {
    protected final int stateId;
    protected final int nbVars;
    protected final int nbHard;
    protected final int nbSoft;
    protected final int ubCost;
    protected final int currentWeight;
    protected final int[] softWeights;

    /**
     * Creates a new MaxSAT state with the given parameters.
     * @param stateId       the ID of the state in the solver
     * @param nbVars        the number of variables
     * @param nbHard        the number of hard clauses
     * @param nbSoft        the number of soft clauses
     * @param ubCost        the ub cost
     * @param currentWeight the current weight
     * @param softWeights   the weights in each soft clause,
     *                      must have length {@code nbSoft}
     */
    public MaxSatState(final int stateId, final int nbVars, final int nbHard, final int nbSoft, final int ubCost, final int currentWeight,
                       final int[] softWeights) {
        this.stateId = stateId;
        this.nbVars = nbVars;
        this.nbHard = nbHard;
        this.nbSoft = nbSoft;
        this.ubCost = ubCost;
        this.currentWeight = currentWeight;
        this.softWeights = softWeights;
    }

    /**
     * Returns the ID of the state in the solver.
     * @return the ID of the state in the solver
     */
    public int getStateId() {
        return stateId;
    }

    /**
     * Returns the number of variables.
     * @return the number of variables
     */
    public int getNbVars() {
        return nbVars;
    }

    /**
     * Returns the number of hard clauses.
     * @return the number of hard clauses
     */
    public int getNbHard() {
        return nbHard;
    }

    /**
     * Returns the number of soft clauses.
     * @return the number of soft clauses
     */
    public int getNbSoft() {
        return nbSoft;
    }

    /**
     * Returns the ub cost.
     * @return the ub cost
     */
    public int getUbCost() {
        return ubCost;
    }

    /**
     * Returns the current weight.
     * @return the current weight
     */
    public int getCurrentWeight() {
        return currentWeight;
    }

    /**
     * Returns the weights in each soft clause, must have length {@code nbSoft}.
     * @return the weights in each soft clause
     */
    public int[] getSoftWeights() {
        return softWeights;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaxSatState that = (MaxSatState) o;
        return stateId == that.stateId && nbVars == that.nbVars && nbHard == that.nbHard && nbSoft == that.nbSoft
                && ubCost == that.ubCost && currentWeight == that.currentWeight
                && Objects.deepEquals(softWeights, that.softWeights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stateId, nbVars, nbHard, nbSoft, ubCost, currentWeight, Arrays.hashCode(softWeights));
    }

    @Override
    public String toString() {
        return "MaxSatState{" +
                "stateId=" + stateId +
                ", nbVars=" + nbVars +
                ", nbHard=" + nbHard +
                ", nbSoft=" + nbSoft +
                ", ubCost=" + ubCost +
                ", currentWeight=" + currentWeight +
                ", softWeights=" + Arrays.toString(softWeights) +
                '}';
    }
}
