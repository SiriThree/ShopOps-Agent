package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventQueryParam;
import com.sirithree.shopops.admin.persistence.model.AuthAuditEvent;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthAuditEventMapper {
    @Insert("""
            INSERT INTO auth_audit_event (
              tenant_id, shop_id, user_id, username, event_type, event_status,
              auth_type, request_id, client_ip, user_agent, failure_reason, created_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{userId}, #{username}, #{eventType}, #{eventStatus},
              #{authType}, #{requestId}, #{clientIp}, #{userAgent}, #{failureReason}, #{createdAt}
            )
            """)
    int insert(AuthAuditEventCreateCommand command);

    @Select("""
            <script>
            SELECT *
            FROM auth_audit_event
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="param.eventType != null and param.eventType != ''">
                AND event_type = #{param.eventType}
              </if>
              <if test="param.eventStatus != null and param.eventStatus != ''">
                AND event_status = #{param.eventStatus}
              </if>
              <if test="param.userId != null">
                AND user_id = #{param.userId}
              </if>
              <if test="param.username != null and param.username != ''">
                AND username = #{param.username}
              </if>
              <if test="param.requestId != null and param.requestId != ''">
                AND request_id = #{param.requestId}
              </if>
              <if test="param.createdStart != null">
                AND created_at &gt;= #{param.createdStart}
              </if>
              <if test="param.createdEnd != null">
                AND created_at &lt;= #{param.createdEnd}
              </if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AuthAuditEvent> listByPage(@Param("tenantId") Long tenantId,
                                    @Param("shopId") Long shopId,
                                    @Param("param") AuthAuditEventQueryParam param,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM auth_audit_event
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="param.eventType != null and param.eventType != ''">
                AND event_type = #{param.eventType}
              </if>
              <if test="param.eventStatus != null and param.eventStatus != ''">
                AND event_status = #{param.eventStatus}
              </if>
              <if test="param.userId != null">
                AND user_id = #{param.userId}
              </if>
              <if test="param.username != null and param.username != ''">
                AND username = #{param.username}
              </if>
              <if test="param.requestId != null and param.requestId != ''">
                AND request_id = #{param.requestId}
              </if>
              <if test="param.createdStart != null">
                AND created_at &gt;= #{param.createdStart}
              </if>
              <if test="param.createdEnd != null">
                AND created_at &lt;= #{param.createdEnd}
              </if>
            </script>
            """)
    Long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("param") AuthAuditEventQueryParam param);
}
