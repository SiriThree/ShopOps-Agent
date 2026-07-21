package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventQueryParam;
import com.sirithree.shopops.admin.persistence.model.ConnectorAuditEvent;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConnectorAuditEventMapper {
    @Insert("""
            INSERT INTO connector_audit_event (
              tenant_id, shop_id, user_id, username, connector_code, event_type,
              event_status, request_id, message, detail_json, created_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{userId}, #{username}, #{connectorCode}, #{eventType},
              #{eventStatus}, #{requestId}, #{message}, #{detailJson}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConnectorAuditEvent event);

    @Select("""
            <script>
            SELECT * FROM connector_audit_event
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            <if test="query.eventId != null">
              AND id = #{query.eventId}
            </if>
            <if test="query.connectorCode != null and query.connectorCode != ''">
              AND connector_code = #{query.connectorCode}
            </if>
            <if test="query.eventType != null and query.eventType != ''">
              AND event_type = #{query.eventType}
            </if>
            <if test="query.eventStatus != null and query.eventStatus != ''">
              AND event_status = #{query.eventStatus}
            </if>
            <if test="query.userId != null">
              AND user_id = #{query.userId}
            </if>
            <if test="query.username != null and query.username != ''">
              AND username = #{query.username}
            </if>
            <if test="query.createdStart != null">
              AND created_at &gt;= #{query.createdStart}
            </if>
            <if test="query.createdEnd != null">
              AND created_at &lt;= #{query.createdEnd}
            </if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ConnectorAuditEvent> listByPage(@Param("tenantId") Long tenantId,
                                         @Param("shopId") Long shopId,
                                         @Param("query") ConnectorAuditEventQueryParam query,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1) FROM connector_audit_event
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            <if test="query.eventId != null">
              AND id = #{query.eventId}
            </if>
            <if test="query.connectorCode != null and query.connectorCode != ''">
              AND connector_code = #{query.connectorCode}
            </if>
            <if test="query.eventType != null and query.eventType != ''">
              AND event_type = #{query.eventType}
            </if>
            <if test="query.eventStatus != null and query.eventStatus != ''">
              AND event_status = #{query.eventStatus}
            </if>
            <if test="query.userId != null">
              AND user_id = #{query.userId}
            </if>
            <if test="query.username != null and query.username != ''">
              AND username = #{query.username}
            </if>
            <if test="query.createdStart != null">
              AND created_at &gt;= #{query.createdStart}
            </if>
            <if test="query.createdEnd != null">
              AND created_at &lt;= #{query.createdEnd}
            </if>
            </script>
            """)
    long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("query") ConnectorAuditEventQueryParam query);
}
