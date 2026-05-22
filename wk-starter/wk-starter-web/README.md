# wk-starter-web

Web 模块通用组件，包含：跨域配置、日志切面、XSS 防护、防重复提交。

## 依赖

```xml
<dependency>
    <groupId>com.budwk.sp</groupId>
    <artifactId>wk-starter-web</artifactId>
</dependency>
```

## 配置说明

### 完整配置示例

```yaml
wk:
  web:
    # 异常返回配置
    exception:
      expose-message: false  # 是否向前端返回底层异常明细，默认 false
    # 请求日志配置
    log:
      enabled: true
      slow-threshold: 1000  # 慢请求阈值(ms)
      ignore-arg-paths:
        - /open/image/recognize
        - /platform/video/**    # 这些路径不打印请求参数

    # XSS 防护配置
    xss:
      enabled: true
      exclude-urls:
        - /platform/sys/**          # 后台管理接口
        - /platform/editor/upload   # 富文本编辑器上传
```

### 异常返回配置

`GlobalExceptionHandler` 默认会在服务端日志中保留完整异常，但前端只返回通用文案：

```json
{
  "code": 500,
  "msg": "系统服务异常，请联系管理员"
}
```

这样可以避免把 SQL、数据库结构、堆栈等内部细节暴露给客户端。

如需在本地联调时临时查看真实异常消息，可开启：

```yaml
wk:
  web:
    exception:
      expose-message: true
```

开启后，前端会收到：

```json
{
  "code": 500,
  "msg": "系统服务异常: 具体异常消息"
}
```

建议：

- 生产环境保持 `false`
- 仅在本地或受控测试环境临时开启
- 排查完成后及时关闭

### 请求日志按路径忽略参数

如果某些接口会提交超大参数（例如图片 base64、富文本、长 JSON），可以配置这些路径只打印 URL / Class / Time，不打印 `Args`：

```yaml
wk:
  web:
    log:
      ignore-arg-paths:
        - /open/image/recognize
        - /platform/video/ai/**
```

支持 Spring `AntPathMatcher` 风格路径，例如 `*`、`**`。

### 防重复提交

```java
@Operation(summary = "提交订单")
@PostMapping("/order/submit")
@RepeatSubmit(interval = 3000, message = "订单处理中，请不要反复点击")
public Result<String> submitOrder(@RequestBody OrderDTO order) {
    // 处理订单逻辑
    return Result.success("订单创建成功");
}
```

### DTO 参数校验

```java
@Data
@Schema(description = "用户创建请求")
public class UserCreateDTO {

    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;

    @Schema(description = "邮箱", example = "admin@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "角色ID列表")
    @NotEmpty(message = "至少选择一个角色")
    private List<Long> roleIds;
}
```

## 常用 OpenAPI 3 注解

| 注解 | 说明 |
|------|------|
| `@Tag` | 接口分组（类级别） |
| `@Operation` | 接口说明（方法级别） |
| `@Parameter` | 参数说明 |
| `@ApiResponse` | 响应说明 |
| `@Schema` | 模型属性说明 |
| `@Hidden` | 隐藏接口 |

## 注意事项

1. **生产环境安全**：生产环境务必设置 `wk.web.swagger.enabled: false`
2. **包扫描优化**：大型项目建议配置 `base-packages` 限定扫描范围
3. **XSS 排除**：富文本相关接口需配置 `xss.exclude-urls` 排除
