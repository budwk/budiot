# wk-starter-dao

`wk-starter-dao` 提供 Nutz Dao、Druid、多数据源，以及可选的多租户 SQL 自动追加能力。

## 基础配置

```yaml
wk:
  dao:
    interceptor:
      log:
        enabled: true
      time:
        enabled: true
    sqls:
      path: sqls/
    druid-web:
      enabled: true
      username: admin
      password: admin

jdbc:
  # many - 多数据源, single或不设置 - 单数据源
  mode: many
  default: user
  many:
    user:
      url: jdbc:mysql://localhost:3306/db_user?useSSL=false&serverTimezone=Asia/Shanghai
      username: root
      password: yourpassword
      driver-class-name: com.mysql.cj.jdbc.Driver
      initial-size: 5
      max-active: 20
      min-idle: 5
      max-wait: 60000
      pool-prepared-statements: true
      filters: stat,wall
      slave:
        url: jdbc:mysql://localhost:3307/db_user_slave
        max-active: 10
    order:
      url: jdbc:mysql://localhost:3306/db_order
      username: root
      password: yourpassword
      max-active: 50
```

## 数据库密码加密

```yaml
jdbc:
  mode: many
  many:
    userdb:
      url: jdbc:mysql://localhost:3306/user_db
      username: user_admin
      password: "User_Db_Enc_Password"
      filters: config
      connection-properties: "config.decrypt=true;config.decrypt.key=User_Db_Public_Key"
```

示例：

```yaml
jdbc:
  mode: many
  many:
    userdb:
      url: jdbc:mysql://localhost:3306/user_db
      username: user_admin
      password: "iQPh52xKE3MXvGp9WPGN/L+Mj6rp4Tm05Pl6M2dfRH2+7aIbZ39WMPc1GGZ6yp1+duYLY290MPcP9e8bZYJyBw=="
      filters: config
      connection-properties: "config.decrypt=true;config.decrypt.key=MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMfzC/Aa6LIJolqt7/M0FVTFB23T+zh94uXxvlLoi+hoHYSTlwCBZuQAspc11W+a8m5K5c7nK1CkSjCNCg7E63ECAwEAAQ=="
```

## 多租户机制

开启方式：

```yaml
wk:
  dao:
    tenant:
      enabled: true
      ignore-tables:
        - sys_tenant
        - sys_tenant_package
        - sys_tenant_package_menu
```

### 工作原理

启用后，DAO 拦截器会对当前执行语句自动处理租户字段：

- 查询 / 更新 / 删除：自动追加 `tenantId` 条件
- 插入：自动将 `tenantId` 赋值为当前登录用户所属租户

识别规则：

- 实体类存在 `tenantId` 字段
- 且实体带有 `@Table`
- 且当前表名不在 `wk.dao.tenant.ignore-tables` 中

支持的场景：

- `dao.query/fetch/count/update/delete` 这类基于 `Pojo + Cnd` 的 Nutz 查询
- `dao.insert(...)` / `dao.fastInsert(...)`
- `dao.insert(..., Chain)` 这类基于 `Chain` 的插入
- `Sqls.create(...)` 创建的自定义 SQL
- 组合 SQL / 子查询 / `UNION`
- 动态表名，例如 `@Table("sys_log_${month}")`

当前租户 ID 由 `WkDaoTenantProvider` 提供，业务项目需要自行注入一个 Bean，例如：

```java
@Bean
public WkDaoTenantProvider wkDaoTenantProvider() {
    return () -> {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return StpUtil.getSession().getString("tenantId");
    };
}
```

当 provider 返回 `null` 时，本次线程不会自动追加租户条件。

### 自动追加效果

例如原查询：

```sql
SELECT * FROM sys_user WHERE unitPath LIKE ? ORDER BY updatedAt DESC LIMIT 10 OFFSET 0
```

自动处理后：

```sql
SELECT * FROM sys_user
WHERE unitPath LIKE ? AND tenantId = ?
ORDER BY updatedAt DESC
LIMIT 10 OFFSET 0
```

对于子查询：

```sql
SELECT * FROM (
  SELECT * FROM sys_log_202603 WHERE delFlag = ?
) t
ORDER BY createdAt DESC
```

会递归改写为：

```sql
SELECT * FROM (
  SELECT * FROM sys_log_202603 WHERE delFlag = ? AND tenantId = ?
) t
ORDER BY createdAt DESC
```

### 插入自动回填效果

实体插入：

```java
Sys_user user = new Sys_user();
user.setLoginname("demo");
dao.insert(user);
```

如果 `Sys_user` 存在 `tenantId` 字段且不在忽略表中，执行时会自动回填为当前租户：

```java
user.getTenantId() == currentTenantId
```

`Chain` 插入同样生效：

```java
dao.insert("sys_unit_user", Chain.make("userId", userId).add("unitId", unitId));
```

执行时会自动补成等价于：

```java
dao.insert("sys_unit_user", Chain.make("userId", userId).add("unitId", unitId).add("tenantId", currentTenantId));
```

原生 `INSERT ... VALUES ...` 也会自动补列：

```sql
INSERT INTO sys_user(loginname, username) VALUES('a', 'b')
```

会改写为：

```sql
INSERT INTO sys_user(loginname, username, tenantId) VALUES('a', 'b', 'platform')
```

## 忽略租户条件

有些场景需要主动跳过租户拦截，例如平台级管理、跨租户初始化、全局统计等。

目前提供两种方式。

### 方式一：代码块忽略

推荐用于服务层临时绕过。

```java
WkDaoTenantContext.withoutTenant(() -> {
    dao.query(Sys_user.class, Cnd.where("loginname", "=", "superadmin"));
});
```

有返回值：

```java
Sys_user user = WkDaoTenantContext.withoutTenant(() ->
    dao.fetch(Sys_user.class, Cnd.where("id", "=", userId))
);
```

也可以手工控制：

```java
WkDaoTenantContext.setIgnoreTenant(true);
try {
    // do something
} finally {
    WkDaoTenantContext.clear();
}
```

### 方式二：SQL 注释 hint

已移除。  
对于需要显式跳过租户条件的场景，请统一使用 `WkDaoTenantContext.withoutTenant(...)` 或手工 `setIgnoreTenant(true)`。

## 什么时候应该忽略

建议忽略的典型场景：

- 租户开通、租户删除、租户套餐维护
- 平台管理员跨租户巡检
- 全局日志/监控/审计统计
- 初始化脚本或平台级批处理

不建议忽略的场景：

- 普通租户用户发起的业务查询
- 租户内增删改查
- 用户、角色、菜单、单位等默认租户隔离数据

## 建议

- 优先使用默认自动拼接机制
- 确实需要跨租户时，优先使用 `WkDaoTenantContext.withoutTenant(...)`
- 不要把大量业务表直接加入 `ignore-tables`，否则会破坏默认租户隔离
