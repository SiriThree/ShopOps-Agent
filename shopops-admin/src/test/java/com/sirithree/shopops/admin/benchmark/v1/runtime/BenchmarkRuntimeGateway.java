package com.sirithree.shopops.admin.benchmark.v1.runtime;

public interface BenchmarkRuntimeGateway {
    BenchmarkRuntimeResult execute(BenchmarkRuntimeRequest request, BenchmarkRunRequest runRequest);
}
