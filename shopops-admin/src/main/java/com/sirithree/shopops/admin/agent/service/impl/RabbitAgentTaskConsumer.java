package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDispatchMessage;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "shopops.agent.dispatch-mode", havingValue = "rabbitmq")
public class RabbitAgentTaskConsumer {
    private final JdbcAgentTaskExecutionWorker executionWorker;

    public RabbitAgentTaskConsumer(JdbcAgentTaskExecutionWorker executionWorker) {
        this.executionWorker = executionWorker;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = "${shopops.agent.rabbitmq.queue:shopops.agent.task.execute}",
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "${shopops.agent.rabbitmq.dead-letter-exchange:shopops.agent.dlx}"),
                            @Argument(name = "x-dead-letter-routing-key", value = "${shopops.agent.rabbitmq.dead-letter-routing-key:agent.task.execute.dead}")
                    }
            ),
            exchange = @Exchange(value = "${shopops.agent.rabbitmq.exchange:shopops.agent.exchange}", type = ExchangeTypes.DIRECT, durable = "true"),
            key = "${shopops.agent.rabbitmq.routing-key:agent.task.execute}"
    ))
    public void consume(AgentTaskDispatchMessage message) {
        executionWorker.execute(message);
    }
}
