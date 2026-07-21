package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.organization.domain.OrganizationUserDto;
import com.sirithree.shopops.admin.organization.domain.ShopDto;
import com.sirithree.shopops.admin.organization.domain.ShopMemberDto;
import com.sirithree.shopops.admin.organization.domain.TenantDto;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrganizationAdminMapper {
    @Select("""
            <script>
            SELECT
              u.id AS userId,
              u.username,
              u.display_name AS displayName,
              u.email,
              u.phone,
              CASE WHEN u.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              u.created_at AS createdAt
            FROM user_account u
            WHERE EXISTS (
              SELECT 1 FROM tenant_member tm
              WHERE tm.tenant_id = #{tenantId}
                AND tm.user_id = u.id
            )
            <if test="keyword != null and keyword != ''">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.email LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND u.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            ORDER BY u.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<OrganizationUserDto> listUsers(@Param("tenantId") Long tenantId,
                                        @Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM user_account u
            WHERE EXISTS (
              SELECT 1 FROM tenant_member tm
              WHERE tm.tenant_id = #{tenantId}
                AND tm.user_id = u.id
            )
            <if test="keyword != null and keyword != ''">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.email LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND u.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            </script>
            """)
    Long countUsers(@Param("tenantId") Long tenantId,
                    @Param("keyword") String keyword,
                    @Param("status") String status);

    @Select("""
            SELECT
              u.id AS userId,
              u.username,
              u.display_name AS displayName,
              u.email,
              u.phone,
              CASE WHEN u.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              u.created_at AS createdAt
            FROM user_account u
            WHERE u.id = #{userId}
              AND EXISTS (
                SELECT 1 FROM tenant_member tm
                WHERE tm.tenant_id = #{tenantId}
                  AND tm.user_id = u.id
              )
            LIMIT 1
            """)
    OrganizationUserDto findUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM user_account WHERE username = #{username}")
    Long countUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO user_account
              (username, password_hash, display_name, phone, email, status, created_at, updated_at)
            VALUES
              (#{user.username}, #{passwordHash}, #{user.displayName}, #{user.phone}, #{user.email},
               CASE WHEN #{user.status} = 'ENABLED' THEN 1 ELSE 0 END, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "user.userId")
    int insertUser(@Param("user") OrganizationUserDto user, @Param("passwordHash") String passwordHash);

    @Insert("""
            INSERT INTO tenant_member
              (tenant_id, user_id, role_code, status, joined_at)
            VALUES
              (#{tenantId}, #{userId}, #{roleCode}, CASE WHEN #{status} = 'ENABLED' THEN 1 ELSE 0 END, NOW())
            """)
    int insertTenantMember(@Param("tenantId") Long tenantId,
                           @Param("userId") Long userId,
                           @Param("roleCode") String roleCode,
                           @Param("status") String status);

    @Insert("""
            INSERT INTO shop_member
              (tenant_id, shop_id, user_id, role_code, status, joined_at)
            VALUES
              (#{tenantId}, #{shopId}, #{userId}, #{roleCode}, CASE WHEN #{status} = 'ENABLED' THEN 1 ELSE 0 END, NOW())
            """)
    int insertShopMember(@Param("tenantId") Long tenantId,
                         @Param("shopId") Long shopId,
                         @Param("userId") Long userId,
                         @Param("roleCode") String roleCode,
                         @Param("status") String status);

    @Update("""
            UPDATE user_account
            SET password_hash = #{passwordHash},
                updated_at = NOW()
            WHERE id = #{userId}
            """)
    int updateUserPassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Select("""
            SELECT role_code
            FROM tenant_member
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
            ORDER BY role_code ASC
            """)
    List<String> listTenantRoles(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("""
            SELECT role_code
            FROM shop_member
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND user_id = #{userId}
            ORDER BY role_code ASC
            """)
    List<String> listShopRoles(@Param("tenantId") Long tenantId,
                               @Param("shopId") Long shopId,
                               @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT
              t.id AS tenantId,
              t.tenant_no AS tenantNo,
              t.tenant_name AS tenantName,
              CASE WHEN t.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              t.plan_type AS planType,
              t.contact_name AS contactName,
              t.contact_phone AS contactPhone,
              (SELECT COUNT(1) FROM shop s WHERE s.tenant_id = t.id) AS shopCount,
              (SELECT COUNT(1) FROM tenant_member tm WHERE tm.tenant_id = t.id) AS memberCount,
              t.created_at AS createdAt
            FROM tenant t
            WHERE t.id = #{tenantId}
            <if test="keyword != null and keyword != ''">
              AND (
                t.tenant_no LIKE CONCAT('%', #{keyword}, '%')
                OR t.tenant_name LIKE CONCAT('%', #{keyword}, '%')
                OR t.contact_name LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND t.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            ORDER BY t.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<TenantDto> listTenants(@Param("tenantId") Long tenantId,
                                @Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM tenant t
            WHERE t.id = #{tenantId}
            <if test="keyword != null and keyword != ''">
              AND (
                t.tenant_no LIKE CONCAT('%', #{keyword}, '%')
                OR t.tenant_name LIKE CONCAT('%', #{keyword}, '%')
                OR t.contact_name LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND t.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            </script>
            """)
    Long countTenants(@Param("tenantId") Long tenantId,
                      @Param("keyword") String keyword,
                      @Param("status") String status);

    @Select("""
            SELECT
              t.id AS tenantId,
              t.tenant_no AS tenantNo,
              t.tenant_name AS tenantName,
              CASE WHEN t.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              t.plan_type AS planType,
              t.contact_name AS contactName,
              t.contact_phone AS contactPhone,
              (SELECT COUNT(1) FROM shop s WHERE s.tenant_id = t.id) AS shopCount,
              (SELECT COUNT(1) FROM tenant_member tm WHERE tm.tenant_id = t.id) AS memberCount,
              t.created_at AS createdAt
            FROM tenant t
            WHERE t.id = #{tenantId}
            LIMIT 1
            """)
    TenantDto findTenant(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(1) FROM tenant WHERE tenant_no = #{tenantNo} AND id != #{tenantId}")
    Long countTenantNoExcept(@Param("tenantNo") String tenantNo, @Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO tenant
              (tenant_no, tenant_name, status, plan_type, contact_name, contact_phone, created_at, updated_at)
            VALUES
              (#{tenant.tenantNo}, #{tenant.tenantName}, CASE WHEN #{tenant.status} = 'ENABLED' THEN 1 ELSE 0 END,
               #{tenant.planType}, #{tenant.contactName}, #{tenant.contactPhone}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "tenant.tenantId")
    int insertTenant(@Param("tenant") TenantDto tenant);

    @Update("""
            UPDATE tenant
            SET tenant_no = #{tenant.tenantNo},
                tenant_name = #{tenant.tenantName},
                status = CASE WHEN #{tenant.status} = 'ENABLED' THEN 1 ELSE 0 END,
                plan_type = #{tenant.planType},
                contact_name = #{tenant.contactName},
                contact_phone = #{tenant.contactPhone},
                updated_at = NOW()
            WHERE id = #{tenant.tenantId}
            """)
    int updateTenant(@Param("tenant") TenantDto tenant);

    @Select("""
            <script>
            SELECT
              s.id AS shopId,
              s.tenant_id AS tenantId,
              s.shop_no AS shopNo,
              s.shop_name AS shopName,
              s.platform_type AS platformType,
              s.owner_id AS ownerId,
              CASE WHEN s.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              (SELECT COUNT(1) FROM shop_member sm WHERE sm.shop_id = s.id) AS memberCount,
              s.created_at AS createdAt
            FROM shop s
            WHERE s.tenant_id = #{tenantId}
            <if test="keyword != null and keyword != ''">
              AND (
                s.shop_no LIKE CONCAT('%', #{keyword}, '%')
                OR s.shop_name LIKE CONCAT('%', #{keyword}, '%')
                OR s.platform_type LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND s.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            ORDER BY s.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ShopDto> listShops(@Param("tenantId") Long tenantId,
                            @Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM shop s
            WHERE s.tenant_id = #{tenantId}
            <if test="keyword != null and keyword != ''">
              AND (
                s.shop_no LIKE CONCAT('%', #{keyword}, '%')
                OR s.shop_name LIKE CONCAT('%', #{keyword}, '%')
                OR s.platform_type LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND s.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            </script>
            """)
    Long countShops(@Param("tenantId") Long tenantId,
                    @Param("keyword") String keyword,
                    @Param("status") String status);

    @Select("""
            SELECT
              s.id AS shopId,
              s.tenant_id AS tenantId,
              s.shop_no AS shopNo,
              s.shop_name AS shopName,
              s.platform_type AS platformType,
              s.owner_id AS ownerId,
              CASE WHEN s.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              (SELECT COUNT(1) FROM shop_member sm WHERE sm.shop_id = s.id) AS memberCount,
              s.created_at AS createdAt
            FROM shop s
            WHERE s.tenant_id = #{tenantId}
              AND s.id = #{shopId}
            LIMIT 1
            """)
    ShopDto findShop(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId);

    @Select("SELECT COUNT(1) FROM shop WHERE tenant_id = #{tenantId} AND shop_no = #{shopNo} AND id != #{shopId}")
    Long countShopNoExcept(@Param("tenantId") Long tenantId,
                           @Param("shopNo") String shopNo,
                           @Param("shopId") Long shopId);

    @Insert("""
            INSERT INTO shop
              (tenant_id, shop_no, shop_name, platform_type, owner_id, status, created_at, updated_at)
            VALUES
              (#{shop.tenantId}, #{shop.shopNo}, #{shop.shopName}, #{shop.platformType}, #{shop.ownerId},
               CASE WHEN #{shop.status} = 'ENABLED' THEN 1 ELSE 0 END, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "shop.shopId")
    int insertShop(@Param("shop") ShopDto shop);

    @Update("""
            UPDATE shop
            SET shop_no = #{shop.shopNo},
                shop_name = #{shop.shopName},
                platform_type = #{shop.platformType},
                owner_id = #{shop.ownerId},
                status = CASE WHEN #{shop.status} = 'ENABLED' THEN 1 ELSE 0 END,
                updated_at = NOW()
            WHERE tenant_id = #{shop.tenantId}
              AND id = #{shop.shopId}
            """)
    int updateShop(@Param("shop") ShopDto shop);

    @Select("""
            <script>
            SELECT
              sm.id AS memberId,
              sm.tenant_id AS tenantId,
              sm.shop_id AS shopId,
              s.shop_name AS shopName,
              sm.user_id AS userId,
              u.username,
              u.display_name AS displayName,
              sm.role_code AS roleCode,
              CASE
                WHEN sm.role_code IN ('SHOP_OWNER', 'SHOP_ADMIN') THEN 'ADMIN'
                WHEN sm.role_code = 'SHOP_OPERATOR' THEN 'OPERATOR'
                ELSE 'VIEWER'
              END AS normalizedRole,
              CASE WHEN sm.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              sm.joined_at AS joinedAt
            FROM shop_member sm
            JOIN user_account u ON u.id = sm.user_id
            JOIN shop s ON s.id = sm.shop_id AND s.tenant_id = sm.tenant_id
            WHERE sm.tenant_id = #{tenantId}
              AND sm.shop_id = #{shopId}
            <if test="keyword != null and keyword != ''">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                OR sm.role_code LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND sm.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            ORDER BY sm.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ShopMemberDto> listShopMembers(@Param("tenantId") Long tenantId,
                                        @Param("shopId") Long shopId,
                                        @Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM shop_member sm
            JOIN user_account u ON u.id = sm.user_id
            WHERE sm.tenant_id = #{tenantId}
              AND sm.shop_id = #{shopId}
            <if test="keyword != null and keyword != ''">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                OR sm.role_code LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            <if test="status != null and status != ''">
              AND sm.status = CASE WHEN UPPER(#{status}) = 'ENABLED' THEN 1 ELSE 0 END
            </if>
            </script>
            """)
    Long countShopMembers(@Param("tenantId") Long tenantId,
                          @Param("shopId") Long shopId,
                          @Param("keyword") String keyword,
                          @Param("status") String status);

    @Select("""
            SELECT
              sm.id AS memberId,
              sm.tenant_id AS tenantId,
              sm.shop_id AS shopId,
              s.shop_name AS shopName,
              sm.user_id AS userId,
              u.username,
              u.display_name AS displayName,
              sm.role_code AS roleCode,
              CASE
                WHEN sm.role_code IN ('SHOP_OWNER', 'SHOP_ADMIN') THEN 'ADMIN'
                WHEN sm.role_code = 'SHOP_OPERATOR' THEN 'OPERATOR'
                ELSE 'VIEWER'
              END AS normalizedRole,
              CASE WHEN sm.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              sm.joined_at AS joinedAt
            FROM shop_member sm
            JOIN user_account u ON u.id = sm.user_id
            JOIN shop s ON s.id = sm.shop_id AND s.tenant_id = sm.tenant_id
            WHERE sm.tenant_id = #{tenantId}
              AND sm.shop_id = #{shopId}
              AND sm.id = #{memberId}
            LIMIT 1
            """)
    ShopMemberDto findShopMember(@Param("tenantId") Long tenantId,
                                 @Param("shopId") Long shopId,
                                 @Param("memberId") Long memberId);

    @Select("""
            SELECT
              sm.id AS memberId,
              sm.tenant_id AS tenantId,
              sm.shop_id AS shopId,
              s.shop_name AS shopName,
              sm.user_id AS userId,
              u.username,
              u.display_name AS displayName,
              sm.role_code AS roleCode,
              CASE
                WHEN sm.role_code IN ('SHOP_OWNER', 'SHOP_ADMIN') THEN 'ADMIN'
                WHEN sm.role_code = 'SHOP_OPERATOR' THEN 'OPERATOR'
                ELSE 'VIEWER'
              END AS normalizedRole,
              CASE WHEN sm.status = 1 THEN 'ENABLED' ELSE 'DISABLED' END AS status,
              sm.joined_at AS joinedAt
            FROM shop_member sm
            JOIN user_account u ON u.id = sm.user_id
            JOIN shop s ON s.id = sm.shop_id AND s.tenant_id = sm.tenant_id
            WHERE sm.tenant_id = #{tenantId}
              AND sm.shop_id = #{shopId}
              AND sm.user_id = #{userId}
            LIMIT 1
            """)
    ShopMemberDto findShopMemberByUser(@Param("tenantId") Long tenantId,
                                       @Param("shopId") Long shopId,
                                       @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM shop_member WHERE tenant_id = #{tenantId} AND shop_id = #{shopId} AND user_id = #{userId}")
    Long countShopMemberByUser(@Param("tenantId") Long tenantId,
                               @Param("shopId") Long shopId,
                               @Param("userId") Long userId);

    @Update("""
            UPDATE shop_member
            SET role_code = #{roleCode},
                status = CASE WHEN #{status} = 'ENABLED' THEN 1 ELSE 0 END
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND id = #{memberId}
            """)
    int updateShopMember(@Param("tenantId") Long tenantId,
                         @Param("shopId") Long shopId,
                         @Param("memberId") Long memberId,
                         @Param("roleCode") String roleCode,
                         @Param("status") String status);

    @Select("SELECT COUNT(1) FROM tenant WHERE id = #{tenantId}")
    Long tenantTotal(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(1) FROM shop WHERE tenant_id = #{tenantId}")
    Long shopTotal(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(1)
            FROM user_account u
            WHERE EXISTS (
              SELECT 1 FROM tenant_member tm
              WHERE tm.tenant_id = #{tenantId}
                AND tm.user_id = u.id
            )
            """)
    Long userTotal(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(1) FROM shop_member WHERE tenant_id = #{tenantId} AND shop_id = #{shopId} AND status = 1")
    Long activeMemberTotal(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId);

    @Select("SELECT COUNT(1) FROM shop_member WHERE tenant_id = #{tenantId} AND shop_id = #{shopId} AND status = 0")
    Long disabledMemberTotal(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId);
}
