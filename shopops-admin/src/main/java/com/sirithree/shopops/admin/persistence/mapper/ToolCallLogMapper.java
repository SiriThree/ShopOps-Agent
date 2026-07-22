package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.ToolCallLog;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ToolCallLogMapper {
    @Insert("""
            INSERT INTO tool_call_log (
              tenant_id, shop_id, task_id, step_id, trace_id, span_id, user_id,
              tool_code, tool_version, input_json, output_json, status, risk_level,
              approval_id, latency_ms, retry_count, error_code, error_message, created_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{taskId}, #{stepId}, #{traceId}, #{spanId}, #{userId},
              #{toolCode}, #{toolVersion}, #{inputJson}, #{outputJson}, #{status}, #{riskLevel},
              #{approvalId}, #{latencyMs}, #{retryCount}, #{errorCode}, #{errorMessage}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ToolCallLog log);

    @Update("""
            UPDATE tool_call_log
            SET output_json = #{outputJson},
                status = #{status},
                risk_level = COALESCE(#{riskLevel}, risk_level),
                approval_id = COALESCE(#{approvalId}, approval_id),
                latency_ms = #{latencyMs},
                error_code = #{errorCode},
                error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int finish(ToolCallLog log);

    @Select("""
            <script>
            SELECT *
            FROM tool_call_log
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="logId != null">
                AND id = #{logId}
              </if>
              <if test="taskId != null">
                AND task_id = #{taskId}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="toolCode != null and toolCode != ''">
                AND tool_code = #{toolCode}
              </if>
            ORDER BY id
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ToolCallLog> listByPage(@Param("tenantId") Long tenantId,
                                 @Param("shopId") Long shopId,
                                 @Param("logId") Long logId,
                                 @Param("taskId") Long taskId,
                                 @Param("status") String status,
                                 @Param("toolCode") String toolCode,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM tool_call_log
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="logId != null">
                AND id = #{logId}
              </if>
              <if test="taskId != null">
                AND task_id = #{taskId}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="toolCode != null and toolCode != ''">
                AND tool_code = #{toolCode}
              </if>
            </script>
            """)
    Long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("logId") Long logId,
                     @Param("taskId") Long taskId,
                     @Param("status") String status,
                     @Param("toolCode") String toolCode);
}
