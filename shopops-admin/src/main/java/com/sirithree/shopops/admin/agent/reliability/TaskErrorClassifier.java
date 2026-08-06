package com.sirithree.shopops.admin.agent.reliability;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class TaskErrorClassifier {
    public TaskErrorType classify(Throwable error) {
        if (error instanceof SecurityException) return TaskErrorType.PERMISSION_DENIED;
        if (error instanceof IllegalArgumentException) return TaskErrorType.VALIDATION_ERROR;
        if (error instanceof DataIntegrityViolationException) return TaskErrorType.BUSINESS_CONFLICT;
        if (hasCause(error, SocketTimeoutException.class) || hasCause(error, TimeoutException.class)) {
            return TaskErrorType.NETWORK_TIMEOUT;
        }
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        if (message.contains("429") || message.contains("rate limit")) return TaskErrorType.RATE_LIMITED;
        if (message.contains("result unknown") || message.contains("结果未知")) return TaskErrorType.EXTERNAL_RESULT_UNKNOWN;
        if (message.contains("unavailable") || message.contains("503")) return TaskErrorType.DEPENDENCY_UNAVAILABLE;
        return TaskErrorType.INTERNAL_ERROR;
    }
    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }
}
