package com.sirithree.shopops.admin.common;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.exception.AccessDeniedException;
import com.sirithree.shopops.admin.auth.exception.AuthenticationException;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonResult;
import com.sirithree.shopops.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuthAuditService authAuditService;

    public GlobalExceptionHandler(AuthAuditService authAuditService) {
        this.authAuditService = authAuditService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResult<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldErrorMessage)
                .collect(Collectors.joining("; "));
        return badRequest(message.isBlank() ? "请求参数校验失败" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResult<?>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return badRequest(message.isBlank() ? "请求参数校验失败" : message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResult<?>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        return badRequest("缺少请求参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResult<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("请求参数类型错误: " + ex.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResult<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResult<?>> handleIllegalArgument(IllegalArgumentException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResult<?>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        recordAccessDenied(ex, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CommonResult.failed(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResult<?>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResult.failed(ResultCode.UNAUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResult<?>> handleException(Exception ex) {
        LOGGER.error("Unhandled request exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResult.failed("系统异常，请稍后重试"));
    }

    private ResponseEntity<CommonResult<?>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonResult.validateFailed(message));
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getField() + " " + (error.getDefaultMessage() == null ? "不合法" : error.getDefaultMessage());
    }

    private void recordAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        RequestContext context = currentContext();
        if (context == null) {
            return;
        }
        AuthAuditEventCreateCommand command = new AuthAuditEventCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setUserId(context.getUserId());
        command.setUsername(context.getUsername());
        command.setEventType("ACCESS_DENIED");
        command.setEventStatus("FAILURE");
        command.setAuthType(context.getAuthType());
        command.setRequestId(context.getRequestId());
        command.setClientIp(clientIp(request));
        command.setUserAgent(request.getHeader("User-Agent"));
        command.setFailureReason(ex.getMessage());
        authAuditService.record(command);
    }

    private RequestContext currentContext() {
        try {
            return RequestContextHolder.current();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
