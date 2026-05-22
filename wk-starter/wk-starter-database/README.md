# wk-starter-database

```yaml
wk:
  database:
    enabled: true
    snowflake: true 
    table:
        create: true #是否自动建表 默认false
        migration: true #是否自动变更 默认false
        add: true # 是否添加列 默认false
        delete: false # 是否删除列 默认false
        check: false # 是否检查索引 默认false
        package: # 相关实体所在包
          - com.budwk

# 如果是 snowflake 模式，下面的 redis 需要配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: yourpassword
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

* 数据脱敏

```java
@Data
public class UserDTO {
    private String username;

    @Desensitize(strategy = DesensitizeStrategy.PHONE)
    private String mobile;

    @Desensitize(strategy = DesensitizeStrategy.ID_CARD)
    private String idCard;

    @Desensitize(strategy = DesensitizeStrategy.EMAIL)
    private String email;
}
```