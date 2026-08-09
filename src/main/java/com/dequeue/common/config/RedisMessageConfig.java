package com.dequeue.common.config;

import com.dequeue.notification.service.RedisOrderEventSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisMessageConfig {

    @Value("${dequeue.notification.channel:dequeue:order-status}")
    private String channel;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter orderStatusListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(orderStatusListenerAdapter, new ChannelTopic(channel));
        return container;
    }

    @Bean
    public MessageListenerAdapter orderStatusListenerAdapter(RedisOrderEventSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }
}
