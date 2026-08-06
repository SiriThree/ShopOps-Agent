package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.persistence.model.ModelCallLog;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ModelCallLogMapper {
    @Insert("""
            INSERT INTO model_call_log (
              tenant_id, shop_id, user_id, username, provider_code, model_name,
              prompt_code, prompt_version, trace_id, task_id, report_id, status,
              prompt_tokens, completion_tokens, total_tokens, latency_ms,
              error_code, error_message, prompt_preview, output_preview, metadata_json, created_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{userId}, #{username}, #{providerCode}, #{modelName},
              #{promptCode}, #{promptVersion}, #{traceId}, #{taskId}, #{reportId}, #{status},
              #{promptTokens}, #{completionTokens}, #{totalTokens}, #{latencyMs},
              #{errorCode}, #{errorMessage}, #{promptPreview}, #{outputPreview}, #{metadataJson}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelCallLog log);

    @Select("""
            <script>
            SELECT *
            FROM model_call_log
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.providerCode != null and query.providerCode != ''">
                AND provider_code = #{query.providerCode}
              </if>
              <if test="query.modelName != null and query.modelName != ''">
                AND model_name = #{query.modelName}
              </if>
              <if test="query.status != null and query.status != ''">
                AND status = #{query.status}
              </if>
              <if test="query.traceId != null and query.traceId != ''">
                AND trace_id = #{query.traceId}
              </if>
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ModelCallLog> listByPage(@Param("tenantId") Long tenantId,
                                  @Param("shopId") Long shopId,
                                  @Param("query") ModelCallLogQueryParam query,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM model_call_log
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.providerCode != null and query.providerCode != ''">
                AND provider_code = #{query.providerCode}
              </if>
              <if test="query.modelName != null and query.modelName != ''">
                AND model_name = #{query.modelName}
              </if>
              <if test="query.status != null and query.status != ''">
                AND status = #{query.status}
              </if>
              <if test="query.traceId != null and query.traceId != ''">
                AND trace_id = #{query.traceId}
              </if>
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
            </script>
            """)
    Long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("query") ModelCallLogQueryParam query);
}
