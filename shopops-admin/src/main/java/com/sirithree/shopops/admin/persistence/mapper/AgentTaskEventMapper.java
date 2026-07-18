package com.sirithree.shopops.admin.persistence.mapper;

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
}
