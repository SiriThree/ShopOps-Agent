package com.sirithree.shopops.admin.common.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestContextInterceptor implements HandlerInterceptor {
    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("SHOPOPS_ACCESS");
    private static final String ATTR_STARTED_AT = RequestContextInterceptor.class.getName() + ".STARTED_AT";

    private final RequestContextResolver requestContextResolver;

    public RequestContextInterceptor(RequestContextResolver requestContextResolver) {
        this.requestContextResolver = requestContextResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_STARTED_AT, System.currentTimeMillis());
        RequestContext context = requestContextResolver.resolve(request);
        RequestContextHolder.set(context);
        MDC.put("tenantId", String.valueOf(context.getTenantId()));
        MDC.put("shopId", String.valueOf(context.getShopId()));
        MDC.put("userId", String.valueOf(context.getUserId()));
        MDC.put("requestId", context.getRequestId());
        response.setHeader(RequestContextResolver.HEADER_REQUEST_ID, context.getRequestId());
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

    private long startedAt(HttpServletRequest request) {
        Object startedAt = request.getAttribute(ATTR_STARTED_AT);
        return startedAt instanceof Long value ? value : System.currentTimeMillis();
    }
}
