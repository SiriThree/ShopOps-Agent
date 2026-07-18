package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.AuthTokenSession;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthTokenSessionMapper {
    @Insert("""
            INSERT INTO auth_token_session (
              token_id, tenant_id, shop_id, user_id, username, roles_json, status,
              issued_at, expires_at, last_seen_at, created_at, updated_at
            ) VALUES (
              #{tokenId}, #{tenantId}, #{shopId}, #{userId}, #{username}, #{rolesJson}, #{status},
              #{issuedAt}, #{expiresAt}, #{lastSeenAt}, #{createdAt}, #{updatedAt}
            )
            """)
    int insert(AuthTokenSession session);

    @Select("""
            SELECT *
            FROM auth_token_session
            WHERE token_id = #{tokenId}
            LIMIT 1
            """)
    AuthTokenSession selectByTokenId(@Param("tokenId") String tokenId);

    @Update("""
            UPDATE auth_token_session
            SET last_seen_at = #{lastSeenAt},
                updated_at = #{lastSeenAt}
            WHERE token_id = #{tokenId}
              AND status = 'ACTIVE'
            """)
    int touch(@Param("tokenId") String tokenId, @Param("lastSeenAt") LocalDateTime lastSeenAt);

    @Update("""
            UPDATE auth_token_session
            SET status = 'REVOKED',
                revoked_at = #{revokedAt},
                updated_at = #{revokedAt}
            WHERE token_id = #{tokenId}
              AND status = 'ACTIVE'
            """)
    int revoke(@Param("tokenId") String tokenId, @Param("revokedAt") LocalDateTime revokedAt);
}
