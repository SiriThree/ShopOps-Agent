package com.sirithree.shopops.admin.auth.component;

import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.auth.exception.AccessDeniedException;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String permission = requiredPermission(request);
        if (permission == null) {
            return true;
        }
        RequestContext context = RequestContextHolder.current();
        if (context.hasPermission(permission)) {
            return true;
        }
        throw new AccessDeniedException("Access denied, required permission: " + permission);
    }

    private String requiredPermission(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/admin/auth/login") || path.startsWith("/api/system/")) return null;
        if (path.startsWith("/api/admin/organization")) {
            return "GET".equals(method) ? PermissionCode.DASHBOARD_READ : PermissionCode.USER_MANAGE;
        }
        if (path.startsWith("/api/admin/audit") || path.startsWith("/api/traces") || path.startsWith("/api/admin/auth/audit")) return PermissionCode.AUDIT_READ;
        if (path.startsWith("/api/admin/connectors")) return "GET".equals(method) ? PermissionCode.CONNECTOR_READ : PermissionCode.CONNECTOR_MANAGE;
        if (path.startsWith("/api/admin/approvals") || path.startsWith("/api/approvals")) {
            if ("GET".equals(method)) return PermissionCode.APPROVAL_READ;
            if (path.equals("/api/admin/approvals") || path.equals("/api/approvals") || path.endsWith("/withdraw")) {
                return PermissionCode.AGENT_EXECUTE;
            }
            return PermissionCode.APPROVAL_REVIEW;
        }
        if (path.startsWith("/api/tools")) return "GET".equals(method) ? PermissionCode.TOOL_READ : PermissionCode.TOOL_EXECUTE;
        if (path.startsWith("/api/agent") || path.startsWith("/api/evaluation")) return "GET".equals(method) ? PermissionCode.TASK_READ : PermissionCode.AGENT_EXECUTE;
        if (path.startsWith("/api/reports")) return "GET".equals(method) ? PermissionCode.TASK_READ : PermissionCode.REPORT_GENERATE;
        if (path.startsWith("/api/admin/dashboard")) return PermissionCode.DASHBOARD_READ;
        if (path.startsWith("/api/admin/model-gateway/invoke")) return PermissionCode.AGENT_EXECUTE;
        if (path.startsWith("/api/admin/model-gateway/call-logs")) return PermissionCode.AUDIT_READ;
        if (path.startsWith("/api/admin/model") || path.startsWith("/api/admin/prompts")) return PermissionCode.USER_MANAGE;
        return null;
    }
}
