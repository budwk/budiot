# wk-starter-log

`wk-starter-log` 提供 `@SLog` 注解和自动切面，可记录登录日志、退出日志和业务操作日志，并自动采集请求参数、执行结果、异常信息、浏览器和操作系统。

## 依赖

```xml
<dependency>
    <groupId>com.budwk.sp</groupId>
    <artifactId>wk-starter-log</artifactId>
</dependency>
```

## 配置示例

```yaml
wk:
  log:
    enabled: true
    max-params-length: 4000
    max-result-length: 2000
    sensitive-fields:
      - password
      - token
      - accessToken
      - smscode
      - captchaCode
      - rsaKey
```

## 使用示例

```java
@RestController
@RequestMapping("/sys/user")
@SLog(tag = "用户管理")
public class SysUserController {

    @PostMapping("/create")
    @SLog(type = "CREATE", msg = "创建用户")
    public Result<?> create(@RequestBody UserDTO dto) {
        return Result.success();
    }
}
```

## 说明

1. `@SLog` 支持标注在类和方法上，方法级配置优先级更高。
2. 切面会自动脱敏 `password/token/smscode` 等敏感字段。
3. Starter 只负责采集日志上下文，具体落库由业务模块实现 `SLogHandler`。
