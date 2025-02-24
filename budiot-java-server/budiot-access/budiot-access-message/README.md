#

## RocketMQ

版本：5.2.x

```yaml
rocketmq:
  enable: true
  # 集群环境多个nameserver用;分割
  nameserver-address: 127.0.0.1:9876
  # 生产者组
  producer-group: gateway_producer
  producer-timeout: 1
  consumer-thread-max: 100
  consumer-thread-min: 5

```

## RabbitMQ

版本：3.10.x
```yaml
rabbitmq:
  enable: true
  host: 10.10.10.10
  port: 5672
  username: guest
  password: guest
  thread:
    max: 8
    min: 4
  queue:
    size: 10000
```