package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.persistence.model.AgentTask;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentTaskMapper {
    @Insert("""
            INSERT INTO agent_task (
              tenant_id, shop_id, user_id, task_no, task_type, user_input,
              status, priority, plan_json, result_summary, trace_id, report_id,
              error_code, error_message, created_at, started_at, finished_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{userId}, #{taskNo}, #{taskType}, #{userInput},
              #{status}, #{priority}, #{planJson}, #{resultSummary}, #{traceId}, #{reportId},
              #{errorCode}, #{errorMessage}, #{createdAt}, #{startedAt}, #{finishedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentTask task);

    @Select("""
            SELECT *
            FROM agent_task
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            """)
    AgentTask selectById(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId, @Param("id") Long id);

    @Select("""
            <script>
            SELECT *
            FROM agent_task
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="taskType != null and taskType != ''">
                AND task_type = #{taskType}
              </if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AgentTask> listByPage(@Param("tenantId") Long tenantId,
                               @Param("shopId") Long shopId,
                               @Param("status") String status,
                               @Param("taskType") String taskType,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM agent_task
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="taskType != null and taskType != ''">
                AND task_type = #{taskType}
              </if>
            </script>
            """)
    Long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("status") String status,
                     @Param("taskType") String taskType);

    @Update("""
            UPDATE agent_task
            SET status = #{status},
                result_summary = #{resultSummary},
                report_id = #{reportId},
                error_code = #{errorCode},
                error_message = #{errorMessage},
                started_at = #{startedAt},
                finished_at = #{finishedAt}
            WHERE id = #{id}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            """)
    int updateExecutionState(AgentTask task);
}
