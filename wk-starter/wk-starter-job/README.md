# wk-starter-job

XXL-JOB 执行器 Starter，负责自动装配 `XxlJobSpringExecutor`，统一通过 `wk.job.*` 配置项完成接入。

## 依赖

```xml
<dependency>
    <groupId>com.budwk.sp</groupId>
    <artifactId>wk-starter-job</artifactId>
</dependency>
```

## 配置说明

```yaml
wk:
  job:
    enabled: true
    admin-addresses: http://127.0.0.1:8080/xxl-job-admin
    access-token: default_token
    timeout: 3
    executor:
      appname: budwk-platform-job
      address:
      ip:
      port: 9999
      log-path: logs/xxl-job/jobhandler
      log-retention-days: 30
      excluded-package:
```

## 使用示例

```java
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class DemoJobHandler {

    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        XxlJobHelper.log("demo job start");
        // 这里编写任务逻辑
        XxlJobHelper.handleSuccess("执行成功");
    }
}
```

## 配置项说明

| 配置项 | 说明 |
|------|------|
| `wk.job.enabled` | 是否启用 XXL-JOB 执行器自动配置 |
| `wk.job.admin-addresses` | 调度中心地址，多个地址使用逗号分隔 |
| `wk.job.access-token` | 调度中心通讯令牌 |
| `wk.job.timeout` | 调度请求超时时间，单位秒 |
| `wk.job.executor.appname` | 执行器名称，需要和 XXL-JOB 管理端保持一致 |
| `wk.job.executor.address` | 执行器注册地址，通常留空自动注册 |
| `wk.job.executor.ip` | 执行器 IP，留空自动获取 |
| `wk.job.executor.port` | 执行器端口 |
| `wk.job.executor.log-path` | 执行器日志目录 |
| `wk.job.executor.log-retention-days` | 执行器日志保留天数 |
| `wk.job.executor.excluded-package` | XXL-JOB 任务扫描排除包路径 |

## 注意事项

1. 启用后必须配置 `wk.job.admin-addresses` 和 `wk.job.executor.appname`，否则启动时会直接报错。
2. `wk-starter-job` 只负责执行器接入，调度中心 `xxl-job-admin` 需要单独部署。
3. `@XxlJob` 标注的方法所在类必须被 Spring 扫描到，通常建议放在业务模块的 `@Component` Bean 中。
