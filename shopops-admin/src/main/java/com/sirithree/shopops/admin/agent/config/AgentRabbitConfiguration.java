package com.sirithree.shopops.admin.agent.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
    public DirectExchange agentTaskDeadLetterExchange() {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue agentTaskQueue() {
        return QueueBuilder.durable(properties.getQueue())
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue agentTaskDeadLetterQueue() {
        return new Queue(properties.getDeadLetterQueue(), true);
    }

    @Bean
    public Binding agentTaskBinding(DirectExchange agentTaskExchange, Queue agentTaskQueue) {
        return BindingBuilder.bind(agentTaskQueue).to(agentTaskExchange).with(properties.getRoutingKey());
    }

    @Bean
    public Binding agentTaskDeadLetterBinding(DirectExchange agentTaskDeadLetterExchange, Queue agentTaskDeadLetterQueue) {
        return BindingBuilder.bind(agentTaskDeadLetterQueue).to(agentTaskDeadLetterExchange).with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageConverter agentRabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
