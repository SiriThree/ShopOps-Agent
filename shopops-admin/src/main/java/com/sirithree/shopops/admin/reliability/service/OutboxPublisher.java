package com.sirithree.shopops.admin.reliability.service;

import com.sirithree.shopops.admin.agent.config.AgentRabbitProperties;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultContext;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultController;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import com.sirithree.shopops.admin.reliability.persistence.OutboxEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name="shopops.agent.dispatch-mode", havingValue="rabbitmq")
public class OutboxPublisher {
    private final OutboxEventMapper mapper; private final RabbitTemplate rabbit; private final AgentRabbitProperties properties; private final ReliabilityFaultController faults;
    public OutboxPublisher(OutboxEventMapper mapper, RabbitTemplate rabbit, AgentRabbitProperties properties, ReliabilityFaultController faults){this.mapper=mapper;this.rabbit=rabbit;this.properties=properties;this.faults=faults;}
    public int publishPending(int limit){
        List<Map<String,Object>> events=mapper.findPending(LocalDateTime.now(), Math.max(1, Math.min(limit,500))); int published=0;
        for(Map<String,Object> event:events){
            Long id=((Number)event.get("id")).longValue();
            try{
                rabbit.convertAndSend(properties.getExchange(), "outbox."+event.get("eventType"), event.get("payloadJson"));
                faults.hit(ReliabilityFaultPoint.BEFORE_OUTBOX_MARK_PUBLISHED,
                        new ReliabilityFaultContext(String.valueOf(event.get("eventType")), String.valueOf(event.get("aggregateId")), String.valueOf(event.get("aggregateType")), null));
                if(mapper.markPublished(id,LocalDateTime.now())==1) published++;
            }catch(RuntimeException ex){
                int attempts=((Number)event.getOrDefault("attemptCount",0)).intValue()+1;
                long delay=Math.min(3600L, 1L << Math.min(attempts,10));
                mapper.markFailed(id,LocalDateTime.now().plusSeconds(delay), safe(ex.getMessage()),LocalDateTime.now());
            }
        }
        return published;
    }
    private String safe(String value){if(value==null)return "unknown publish error";return value.length()>1000?value.substring(0,1000):value;}
}
