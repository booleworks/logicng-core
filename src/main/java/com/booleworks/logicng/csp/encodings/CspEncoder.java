package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.CspAssignment;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.EncodingResult;

public class CspEncoder {

    private final Algorithm algorithm;
    private final Csp originalCsp;
    private Csp reducedCsp;

    public CspEncoder(final Csp csp) {
        this(csp, Algorithm.Order);
    }

    public CspEncoder(final Csp csp, final Algorithm algorithm) {
        this.originalCsp = csp;
        this.reducedCsp = null;
        this.algorithm = algorithm;
    }

    public void encode(final CspEncodingContext context, final EncodingResult result) {
        if (reducedCsp == null) {
            reduceOnly(context);
        }
        switch (algorithm) {
            case Order:
                OrderEncoding.encode(reducedCsp, context, result);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported csp encoding algorithm: " + algorithm);
        }
    }

    public void reduceOnly(final CspEncodingContext context) {
        switch (algorithm) {
            case Order:
                reducedCsp = OrderReduction.reduce(originalCsp, context);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported csp encoding algorithm: " + algorithm);
        }
    }

    public CspAssignment decode(final Assignment model, final CspEncodingContext context) {
        return OrderDecoding.decode(model, reducedCsp, context);
    }

    public Csp getOriginalCsp() {
        return originalCsp;
    }

    public Csp getReducedCsp() {
        return reducedCsp;
    }

    public enum Algorithm {
        Order
    }
}
