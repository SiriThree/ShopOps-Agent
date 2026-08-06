package com.sirithree.shopops.mcp.commerce.security;

import com.sirithree.shopops.mcp.commerce.config.CommerceMcpServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommerceMcpBearerTokenFilter extends OncePerRequestFilter {
    private final CommerceMcpServerProperties properties;

    public CommerceMcpBearerTokenFilter(CommerceMcpServerProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = "Bearer " + properties.getBearerToken();
        String actual = request.getHeader("Authorization");
        if (actual == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"MCP_UNAUTHORIZED\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
