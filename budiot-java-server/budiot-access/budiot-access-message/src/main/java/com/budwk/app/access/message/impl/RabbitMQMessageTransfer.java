package com.budwk.app.access.message.impl;

import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.message.utils.FastSerializeUtil;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

@IocBean(create = "init", depose = "close")
@Slf4j
public class RabbitMQMessageTransfer implements MessageTransfer {
    @Inject
    private PropertiesProxy conf;

    private Connection connection;
    private Channel channel;
    private List<Channel> consumerChannels = new ArrayList<>();
    private ExecutorService executorService;

    private static final String RABBIT_ENABLED = "rabbitmq.enable";
    private static final String RABBIT_HOST = "rabbitmq.host";
    private static final String RABBIT_PORT = "rabbitmq.port";
    private static final String RABBIT_USERNAME = "rabbitmq.username";
    private static final String RABBIT_PASSWORD = "rabbitmq.password";
    private static final String RABBIT_THREAD_MIN = "rabbitmq.thread.min";
    private static final String RABBIT_THREAD_MAX = "rabbitmq.thread.max";
    private static final String RABBIT_QUEUE_SIZE = "rabbitmq.queue.size";

    public void init() {
        // 判断是否启用 RabbitMQ
        if (!conf.getBoolean(RABBIT_ENABLED, false)) {
            log.info("RabbitMQ 未启用");
            return;
        }

        try {
            // 初始化线程池
            int minThreads = conf.getInt(RABBIT_THREAD_MIN, 4);
            int maxThreads = conf.getInt(RABBIT_THREAD_MAX, 8);
            int queueSize = conf.getInt(RABBIT_QUEUE_SIZE, 10000);

            executorService = new ThreadPoolExecutor(
                    minThreads,
                    maxThreads,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueSize),
                    new ThreadFactoryBuilder().setNameFormat("rabbitmq-consumer-%d").build(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(conf.get(RABBIT_HOST, "localhost"));
            factory.setPort(conf.getInt(RABBIT_PORT, 5672));
            factory.setUsername(conf.get(RABBIT_USERNAME, "guest"));
            factory.setPassword(conf.get(RABBIT_PASSWORD, "guest"));

            connection = factory.newConnection(executorService);  // 使用线程池创建连接
            channel = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ 连接初始化失败", e);
        }
    }

    @Override
    public <T extends Serializable> void publish(Message<T> message) {
        try {
            channel.exchangeDeclare(message.getTo(), "topic", true);
            channel.basicPublish(message.getTo(), "", null, FastSerializeUtil.serialize(message));
        } catch (Exception e) {
            log.error("topic {} 消息发送失败: {}", message.getTo(), e.getMessage());
        }
    }

    @Override
    public <T extends Serializable> void subscribe(String consumerGroup, String topic, String tag,
                                                   MessageModel messageModel, ConsumeMode consumeMode, Consumer<Message<T>> consumer) {
        try {
            Channel consumerChannel = connection.createChannel();
            consumerChannels.add(consumerChannel);

            consumerChannel.exchangeDeclare(topic, "topic", true);
            String queueName = consumerGroup + "_" + topic;

            // 根据消息模式设置队列属性
            boolean exclusive = messageModel == MessageModel.BROADCASTING;
            consumerChannel.queueDeclare(queueName, true, exclusive, false, null);
            consumerChannel.queueBind(queueName, topic, tag);

            // 设置消费者预取数量
            if (consumeMode == ConsumeMode.ORDERLY) {
                consumerChannel.basicQos(1); // 顺序消费时一次只处理一条消息
            }

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    Message<T> message = FastSerializeUtil.deserialize(delivery.getBody());
                    consumer.accept(message);
                    consumerChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    log.error("消息处理失败", e);
                    consumerChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                }
            };

            consumerChannel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            log.error("订阅失败: topic={}, consumerGroup={}", topic, consumerGroup, e);
        }
    }

    @Override
    public <T extends Serializable> void subscribe(String consumerGroup, String topic, Consumer<Message<T>> consumer) {
        this.subscribe(consumerGroup, topic, "#", MessageModel.CLUSTERING, ConsumeMode.ORDERLY, consumer);
    }

    @Override
    public void clearMessage() {
        // RabbitMQ 没有直接清除消息的 API，可以通过删除队列来实现
    }

    public void close() {
        try {
            for (Channel consumerChannel : consumerChannels) {
                if (consumerChannel != null && consumerChannel.isOpen()) {
                    consumerChannel.close();
                }
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            // 关闭线程池
            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                }
            }
        } catch (Exception e) {
            log.error("关闭 RabbitMQ 连接失败", e);
        }
    }
}
