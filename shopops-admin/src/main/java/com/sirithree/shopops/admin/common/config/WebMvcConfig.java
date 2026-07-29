package com.sirithree.shopops.admin.common.config;

import com.sirithree.shopops.admin.auth.component.RoleAuthorizationInterceptor;
import com.sirithree.shopops.admin.common.context.RequestContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final RequestContextInterceptor requestContextInterceptor;
    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;

    public WebMvcConfig(RequestContextInterceptor requestContextInterceptor,
                        RoleAuthorizationInterceptor roleAuthorizationInterceptor) {
        this.requestContextInterceptor = requestContextInterceptor;
        this.roleAuthorizationInterceptor = roleAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestContextInterceptor)
                .addPathPatterns("/api/**", "/mcp");
        registry.addInterceptor(roleAuthorizationInterceptor)
                .addPathPatterns("/api/**", "/mcp");
    }
}
