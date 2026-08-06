package com.sirithree.shopops.admin.reliability.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OutboxEventMapper {
 @Insert("INSERT INTO outbox_event(tenant_id,shop_id,aggregate_type,aggregate_id,event_type,payload_json,status,attempt_count,next_attempt_at,created_at,updated_at) VALUES(#{tenantId},#{shopId},#{aggregateType},#{aggregateId},#{eventType},CAST(#{payloadJson} AS JSON),'PENDING',0,#{now},#{now},#{now})") int insert(@Param("tenantId") Long tenantId,@Param("shopId") Long shopId,@Param("aggregateType") String aggregateType,@Param("aggregateId") String aggregateId,@Param("eventType") String eventType,@Param("payloadJson") String payloadJson,@Param("now") LocalDateTime now);
 @Select("SELECT id,tenant_id tenantId,shop_id shopId,aggregate_type aggregateType,aggregate_id aggregateId,event_type eventType,payload_json payloadJson,attempt_count attemptCount FROM outbox_event WHERE status='PENDING' AND next_attempt_at<=#{now} ORDER BY id LIMIT #{limit}") List<Map<String,Object>> findPending(@Param("now") LocalDateTime now,@Param("limit") int limit);
 @Update("UPDATE outbox_event SET status='PUBLISHED',published_at=#{now},updated_at=#{now} WHERE id=#{id} AND status='PENDING'") int markPublished(@Param("id") Long id,@Param("now") LocalDateTime now);
 @Update("UPDATE outbox_event SET attempt_count=attempt_count+1,next_attempt_at=#{nextAttempt},last_error=#{error},updated_at=#{now} WHERE id=#{id} AND status='PENDING'") int markFailed(@Param("id") Long id,@Param("nextAttempt") LocalDateTime nextAttempt,@Param("error") String error,@Param("now") LocalDateTime now);
}
