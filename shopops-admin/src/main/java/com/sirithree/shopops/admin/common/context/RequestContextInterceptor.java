package com.sirithree.shopops.admin.common.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestContextInterceptor implements HandlerInterceptor {
    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("SHOPOPS_ACCESS");
    private static final String ATTR_STARTED_AT = RequestContextInterceptor.class.getName() + ".STARTED_AT";

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_SHOP_ID = "X-Shop-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_STARTED_AT, System.currentTimeMillis());
        String requestId = headerOrDefault(request, HEADER_REQUEST_ID, "req_" + UUID.randomUUID().toString().replace("-", ""));
        RequestContext context = new RequestContext(
                longHeader(request, HEADER_TENANT_ID, 1L),
                longHeader(request, HEADER_SHOP_ID, 1L),
                longHeader(request, HEADER_USER_ID, 1L),
                requestId
        );
        RequestContextHolder.set(context);
        MDC.put("tenantId", String.valueOf(context.getTenantId()));
        MDC.put("shopId", String.valueOf(context.getShopId()));
        MDC.put("userId", String.valueOf(context.getUserId()));
        MDC.put("requestId", context.getRequestId());
        response.setHeader(HEADER_REQUEST_ID, context.getRequestId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            RequestContext context = RequestContextHolder.current();
            long latencyMs = System.currentTimeMillis() - startedAt(request);
            if (ex == null) {
                ACCESS_LOGGER.info("api_access method={} path={} status={} latencyMs={} tenantId={} shopId={} userId={} requestId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        latencyMs,
                        context.getTenantId(),
                        context.getShopId(),
                        context.getUserId(),
                        context.getRequestId());
            } else {
                ACCESS_LOGGER.warn("api_access_error method={} path={} status={} latencyMs={} tenantId={} shopId={} userId={} requestId={} error={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        latencyMs,
                        context.getTenantId(),
                        context.getShopId(),
                        context.getUserId(),
                        context.getRequestId(),
                        ex.getMessage());
            }
        } finally {
            RequestContextHolder.clear();
            MDC.clear();
        }
    }

    private Long longHeader(HttpServletRequest request, String name, Long defaultValue) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("请求头类型错误: " + name);
        }
    }

    private String headerOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private long startedAt(HttpServletRequest request) {
        Object startedAt = request.getAttribute(ATTR_STARTED_AT);
        return startedAt instanceof Long value ? value : System.currentTimeMillis();
    }
}
