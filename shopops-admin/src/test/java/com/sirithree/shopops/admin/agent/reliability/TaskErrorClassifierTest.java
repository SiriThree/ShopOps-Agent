package com.sirithree.shopops.admin.agent.reliability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class TaskErrorClassifierTest {
    private final TaskErrorClassifier classifier = new TaskErrorClassifier();
    @Test void classifiesPermission() { assertEquals(TaskErrorType.PERMISSION_DENIED, classifier.classify(new SecurityException())); }
    @Test void classifiesRateLimit() { assertEquals(TaskErrorType.RATE_LIMITED, classifier.classify(new RuntimeException("HTTP 429"))); }
    @Test void classifiesTimeout() { assertEquals(TaskErrorType.NETWORK_TIMEOUT, classifier.classify(new RuntimeException(new SocketTimeoutException()))); }
    @Test void classifiesUnknownExternalResult() { assertEquals(TaskErrorType.EXTERNAL_RESULT_UNKNOWN, classifier.classify(new RuntimeException("external result unknown"))); }
}
