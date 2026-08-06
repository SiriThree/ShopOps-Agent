package com.sirithree.shopops.admin.auth.annotation;

import com.sirithree.shopops.admin.auth.domain.AuthRole;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    AuthRole value();
}
