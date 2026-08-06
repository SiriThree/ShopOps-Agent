package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobQueryParam;
import com.sirithree.shopops.admin.persistence.model.ConnectorSyncJob;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConnectorSyncJobMapper {
    @Insert("""
            INSERT INTO connector_sync_job (
              tenant_id, shop_id, connector_code, status, attempt, max_attempts,
              trigger_type, created_by, request_id, message, detail_json,
              started_at, finished_at, created_at, updated_at, cursor_value, checkpoint_json, error_type, next_retry_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{connectorCode}, #{status}, #{attempt}, #{maxAttempts},
              #{triggerType}, #{createdBy}, #{requestId}, #{message}, #{detailJson},
              #{startedAt}, #{finishedAt}, #{createdAt}, #{updatedAt}, #{cursorValue}, #{checkpointJson}, #{errorType}, #{nextRetryAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConnectorSyncJob job);

    @Update("""
            UPDATE connector_sync_job
            SET status = #{status},
                attempt = #{attempt},
                request_id = #{requestId},
                message = #{message},
                detail_json = #{detailJson},
                cursor_value = #{cursorValue}, checkpoint_json = #{checkpointJson},
                error_type = #{errorType}, next_retry_at = #{nextRetryAt},
                started_at = #{startedAt},
                finished_at = #{finishedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            """)
    int updateResult(ConnectorSyncJob job);

    @Select("""
            SELECT * FROM connector_sync_job
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              AND id = #{jobId}
            LIMIT 1
            """)
    ConnectorSyncJob find(@Param("tenantId") Long tenantId,
                          @Param("shopId") Long shopId,
                          @Param("jobId") Long jobId);

    @Select("""
            <script>
            SELECT * FROM connector_sync_job
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            <if test="query.jobId != null">
              AND id = #{query.jobId}
            </if>
            <if test="query.connectorCode != null and query.connectorCode != ''">
              AND connector_code = #{query.connectorCode}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            <if test="query.triggerType != null and query.triggerType != ''">
              AND trigger_type = #{query.triggerType}
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
    List<ConnectorSyncJob> listByPage(@Param("tenantId") Long tenantId,
                                      @Param("shopId") Long shopId,
                                      @Param("query") ConnectorSyncJobQueryParam query,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1) FROM connector_sync_job
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            <if test="query.jobId != null">
              AND id = #{query.jobId}
            </if>
            <if test="query.connectorCode != null and query.connectorCode != ''">
              AND connector_code = #{query.connectorCode}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            <if test="query.triggerType != null and query.triggerType != ''">
              AND trigger_type = #{query.triggerType}
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
                     @Param("query") ConnectorSyncJobQueryParam query);
}
