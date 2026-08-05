package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.auth.domain.LoginUserRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthUserMapper {
    @Select("""
            SELECT username
            FROM user_account
            WHERE id = #{userId}
              AND status = 1
            LIMIT 1
            """)
    String selectUsernameById(@Param("userId") Long userId);

    @Select("""
            SELECT id AS user_id, username, password_hash
            FROM user_account
            WHERE username = #{username}
              AND status = 1
            LIMIT 1
            """)
    LoginUserRecord selectLoginUserByUsername(@Param("username") String username);

    @Select("""
            SELECT role_code
            FROM tenant_member
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND status = 1
            UNION
            SELECT role_code
            FROM shop_member
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND user_id = #{userId}
              AND status = 1
            """)
    List<String> listActiveRoleCodes(@Param("tenantId") Long tenantId,
                                     @Param("shopId") Long shopId,
                                     @Param("userId") Long userId);
    @Select("""
            SELECT DISTINCT sm.shop_id
            FROM shop_member sm
            JOIN shop s ON s.id = sm.shop_id AND s.tenant_id = sm.tenant_id AND s.status = 1
            WHERE sm.tenant_id = #{tenantId}
              AND sm.user_id = #{userId}
              AND sm.status = 1
            UNION
            SELECT DISTINCT s.id
            FROM tenant_member tm
            JOIN shop s ON s.tenant_id = tm.tenant_id AND s.status = 1
            WHERE tm.tenant_id = #{tenantId}
              AND tm.user_id = #{userId}
              AND tm.status = 1
            """)
    List<Long> listAccessibleShopIds(@Param("tenantId") Long tenantId,
                                     @Param("userId") Long userId);

}
