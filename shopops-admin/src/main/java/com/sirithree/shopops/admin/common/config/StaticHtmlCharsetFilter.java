package com.sirithree.shopops.admin.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class StaticHtmlCharsetFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAdminHtml(request)) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8).toString());
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAdminHtml(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/admin/") && uri.endsWith(".html");
    }
}
