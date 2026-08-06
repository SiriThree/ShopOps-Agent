package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogQueryParam;
import com.sirithree.shopops.admin.persistence.model.ConnectorApiCallLog;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConnectorApiCallLogMapper {
    @Insert("""
            INSERT INTO connector_api_call_log (
              tenant_id, shop_id, job_id, connector_code, request_method, endpoint,
              request_target, status, status_code, latency_ms, error_code, error_message,
              request_id, detail_json, created_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{jobId}, #{connectorCode}, #{requestMethod}, #{endpoint},
              #{requestTarget}, #{status}, #{statusCode}, #{latencyMs}, #{errorCode}, #{errorMessage},
              #{requestId}, #{detailJson}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConnectorApiCallLog log);

    @Select("""
            <script>
            SELECT * FROM connector_api_call_log
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            <if test="query.logId != null">
              AND id = #{query.logId}
            </if>
            <if test="query.jobId != null">
              AND job_id = #{query.jobId}
            </if>
            <if test="query.connectorCode != null and query.connectorCode != ''">
              AND connector_code = #{query.connectorCode}
            </if>
            <if test="query.endpoint != null and query.endpoint != ''">
              AND endpoint = #{query.endpoint}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
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
    List<ConnectorApiCallLog> listByPage(@Param("tenantId") Long tenantId,
                                         @Param("shopId") Long shopId,
                                         @Param("query") ConnectorApiCallLogQueryParam query,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1) FROM connector_api_call_log
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            <if test="query.logId != null">
              AND id = #{query.logId}
            </if>
            <if test="query.jobId != null">
              AND job_id = #{query.jobId}
            </if>
            <if test="query.connectorCode != null and query.connectorCode != ''">
              AND connector_code = #{query.connectorCode}
            </if>
            <if test="query.endpoint != null and query.endpoint != ''">
              AND endpoint = #{query.endpoint}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
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
                     @Param("query") ConnectorApiCallLogQueryParam query);
}
