package com.sirithree.shopops.admin.persistence.mapper;

import com.sirithree.shopops.admin.agent.domain.AgentTaskEventQueryParam;
import com.sirithree.shopops.admin.persistence.model.AgentTaskEvent;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentTaskEventMapper {
    @Insert("""
            INSERT INTO agent_task_event (
              tenant_id, shop_id, task_id, event_type, from_status, to_status,
              event_data_json, operator_id, created_at
            ) VALUES (
              #{tenantId}, #{shopId}, #{taskId}, #{eventType}, #{fromStatus}, #{toStatus},
              #{eventDataJson}, #{operatorId}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentTaskEvent event);

    @Select("""
            SELECT *
            FROM agent_task_event
            WHERE task_id = #{taskId}
              AND tenant_id = #{tenantId}
              AND shop_id = #{shopId}
            ORDER BY id
            """)
    List<AgentTaskEvent> listByTaskId(@Param("tenantId") Long tenantId, @Param("shopId") Long shopId, @Param("taskId") Long taskId);

    @Select("""
            <script>
            SELECT *
            FROM agent_task_event
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
              <if test="query.eventType != null and query.eventType != ''">
                AND event_type = #{query.eventType}
              </if>
              <if test="query.fromStatus != null and query.fromStatus != ''">
                AND from_status = #{query.fromStatus}
              </if>
              <if test="query.toStatus != null and query.toStatus != ''">
                AND to_status = #{query.toStatus}
              </if>
              <if test="query.operatorId != null">
                AND operator_id = #{query.operatorId}
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
    List<AgentTaskEvent> listByPage(@Param("tenantId") Long tenantId,
                                    @Param("shopId") Long shopId,
                                    @Param("query") AgentTaskEventQueryParam query,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM agent_task_event
            WHERE tenant_id = #{tenantId}
              AND shop_id = #{shopId}
              <if test="query.taskId != null">
                AND task_id = #{query.taskId}
              </if>
              <if test="query.eventType != null and query.eventType != ''">
                AND event_type = #{query.eventType}
              </if>
              <if test="query.fromStatus != null and query.fromStatus != ''">
                AND from_status = #{query.fromStatus}
              </if>
              <if test="query.toStatus != null and query.toStatus != ''">
                AND to_status = #{query.toStatus}
              </if>
              <if test="query.operatorId != null">
                AND operator_id = #{query.operatorId}
              </if>
              <if test="query.createdStart != null">
                AND created_at &gt;= #{query.createdStart}
              </if>
              <if test="query.createdEnd != null">
                AND created_at &lt;= #{query.createdEnd}
              </if>
            </script>
            """)
    Long countByPage(@Param("tenantId") Long tenantId,
                     @Param("shopId") Long shopId,
                     @Param("query") AgentTaskEventQueryParam query);
}
