package com.sirithree.shopops.admin.agent.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "shopops.agent.dispatch-mode", havingValue = "rabbitmq")
public class AgentRabbitConfiguration {
    private final AgentRabbitProperties properties;

    public AgentRabbitConfiguration(AgentRabbitProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DirectExchange agentTaskExchange() {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue agentTaskQueue() {
        return new Queue(properties.getQueue(), true);
    }

    @Bean
    public Binding agentTaskBinding(DirectExchange agentTaskExchange, Queue agentTaskQueue) {
        return BindingBuilder.bind(agentTaskQueue).to(agentTaskExchange).with(properties.getRoutingKey());
    }

    @Bean
    public MessageConverter agentRabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
