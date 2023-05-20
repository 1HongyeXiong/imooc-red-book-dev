package com.imooc.base;


import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 定义交换机的名称
    public static final String EXCHANGE_MSG = "exchange_msg";

    // 定义队列的名称
    public static final String QUEUE_SYS_MSG = "queue_msg";

    // 创建交换机，放入springboot容器
    @Bean(EXCHANGE_MSG)
    public Exchange exchange() {
        return ExchangeBuilder                      // 构建交换机
                .topicExchange(EXCHANGE_MSG)    // 使用topic类型，并定义交换机的名称。https://www.rabbitmq.com/getstarted.html
                .durable(true)                      // 设置持久化，重启MQ后依然存在
                .build();
    }

    // 创建队列
    @Bean(QUEUE_SYS_MSG)
    public Queue queue() {
        return new Queue(QUEUE_SYS_MSG);
    }

    // 队列绑定交换机
    @Bean
    public Binding binding(
            @Qualifier(QUEUE_SYS_MSG) Queue queue,
            @Qualifier(EXCHANGE_MSG) Exchange exchange) {
        return BindingBuilder               // 定义绑定关系
                .bind(queue)                // 绑定队列
                .to(exchange)               // 到交换机
                .with("sys.msg.*")   // 定义路由规则（requestMapping映射）
                .noargs();                  // 执行绑定
    }
}

