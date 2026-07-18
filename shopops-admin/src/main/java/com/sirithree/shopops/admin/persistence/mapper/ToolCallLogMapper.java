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
                latency_ms = #{latencyMs},
                error_code = #{errorCode},
                error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int finish(ToolCallLog log);

    @Select("""
            SELECT *
            FROM tool_call_log
            WHERE task_id = #{taskId}
            ORDER BY id
            """)
    List<ToolCallLog> listByTaskId(@Param("taskId") Long taskId);
}
