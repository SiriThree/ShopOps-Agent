package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.AgentTaskStep;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentTaskStepMapper {
    @Insert("""
            INSERT INTO agent_task_step (
              tenant_id, shop_id, task_id, step_no, step_name, tool_code, status,
              depends_on, input_json, output_json, retry_count, approval_id,
              error_code, error_message, started_at, finished_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{taskId}, #{stepNo}, #{stepName}, #{toolCode}, #{status},
              #{dependsOn}, #{inputJson}, #{outputJson}, #{retryCount}, #{approvalId},
              #{errorCode}, #{errorMessage}, #{startedAt}, #{finishedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentTaskStep step);

    @Select("""
            SELECT *
            FROM agent_task_step
            WHERE task_id = #{taskId}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            ORDER BY step_no
            """)
    List<AgentTaskStep> listByTaskId(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId, @Param("taskId") Long taskId);

    @Update("""
            UPDATE agent_task_step
            SET status = #{status},
                input_json = #{inputJson},
                output_json = #{outputJson},
                retry_count = #{retryCount},
                approval_id = #{approvalId},
                error_code = #{errorCode},
                error_message = #{errorMessage},
                started_at = #{startedAt},
                finished_at = #{finishedAt}
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            """)
    int updateExecutionState(AgentTaskStep step);
}
