package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.config.AgentRabbitProperties;
import com.sirithree.shopops.admin.agent.domain.AgentDispatchResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDispatchMessage;
import com.sirithree.shopops.admin.agent.service.AgentTaskDispatcher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.agent.dispatch-mode", havingValue = "rabbitmq")
public class RabbitAgentTaskDispatcher implements AgentTaskDispatcher {
    private final RabbitTemplate rabbitTemplate;
    private final AgentRabbitProperties properties;

    public RabbitAgentTaskDispatcher(RabbitTemplate rabbitTemplate, AgentRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public boolean isAsynchronous() {
        return true;
    }

    @Override
    public AgentDispatchResult dispatch(AgentTaskContext context) {
        AgentTaskDispatchMessage message = new AgentTaskDispatchMessage();
        message.setTenantId(context.getTenantId());
        message.setShopId(context.getShopId());
        message.setUserId(context.getUserId());
        message.setTaskId(context.getTaskId());
        message.setTraceId(context.getTraceId());
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getRoutingKey(), message);
        return AgentDispatchResult.accepted();
    }
}
