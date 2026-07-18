package com.sirithree.shopops.admin.auth.component;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.auth.exception.AccessDeniedException;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireRole requireRole = requiredRole(handlerMethod);
        if (requireRole == null) {
            return true;
        }
        RequestContext context = RequestContextHolder.current();
        if (hasRequiredRole(context, requireRole.value())) {
            return true;
        }
        throw new AccessDeniedException("Access denied, required role: " + requireRole.value().name());
    }

    private RequireRole requiredRole(HandlerMethod handlerMethod) {
        RequireRole methodRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (methodRole != null) {
            return methodRole;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
    }

    private boolean hasRequiredRole(RequestContext context, AuthRole requiredRole) {
        return context.getRoles().stream()
                .map(AuthRole::parse)
                .anyMatch(role -> role != null && role.includes(requiredRole));
    }
}
