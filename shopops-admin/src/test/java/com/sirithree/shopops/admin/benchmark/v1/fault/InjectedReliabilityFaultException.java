package com.sirithree.shopops.admin.benchmark.v1.fault;

public class InjectedReliabilityFaultException extends RuntimeException {
    public InjectedReliabilityFaultException(String message) {
        super(message);
    }
}
