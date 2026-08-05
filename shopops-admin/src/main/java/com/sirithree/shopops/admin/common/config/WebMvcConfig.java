package com.sirithree.shopops.admin.common.config;

import com.sirithree.shopops.admin.auth.component.RoleAuthorizationInterceptor;
import com.sirithree.shopops.admin.auth.component.PermissionAuthorizationInterceptor;
import com.sirithree.shopops.admin.common.context.RequestContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final RequestContextInterceptor requestContextInterceptor;
    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;
    private final PermissionAuthorizationInterceptor permissionAuthorizationInterceptor;

    public WebMvcConfig(RequestContextInterceptor requestContextInterceptor,
                        RoleAuthorizationInterceptor roleAuthorizationInterceptor,
                        PermissionAuthorizationInterceptor permissionAuthorizationInterceptor) {
        this.requestContextInterceptor = requestContextInterceptor;
        this.roleAuthorizationInterceptor = roleAuthorizationInterceptor;
        this.permissionAuthorizationInterceptor = permissionAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestContextInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(roleAuthorizationInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(permissionAuthorizationInterceptor)
                .addPathPatterns("/api/**");
    }
}
