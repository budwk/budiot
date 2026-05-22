package com.budwk.sp.starter.database.idgen;

import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.nutz.el.opt.custom.CustomMake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "wk.database.snowflake", havingValue = "true")
public class SnowflakeIdServiceImpl implements IdService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String instanceUuid = UUID.randomUUID().toString();
    private int assignedWorkerId = -1;
    private static final String KEY_PREFIX = "wk:idgen:worker:";

    // Lua 脚本保持不变
    private static final String LUA_ASSIGN =
            "if redis.call('get', KEYS[1]) == ARGV[1] or redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
                    "  redis.call('expire', KEYS[1], ARGV[2]) " +
                    "  return 1 " +
                    "else " +
                    "  return 0 " +
                    "end";

    @PostConstruct
    public void init() {
        short bitLength = 6;
        int maxId = (1 << bitLength) - 1;

        // 循环抢占 WorkerId
        for (int i = 0; i <= maxId; i++) {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_ASSIGN, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(KEY_PREFIX + i), instanceUuid, "300");
            if (Long.valueOf(1).equals(result)) {
                assignedWorkerId = i;
                break;
            }
        }

        if (assignedWorkerId == -1) throw new RuntimeException("Snowflake WorkerId 分配失败");

        // 初始化 Yitter
        IdGeneratorOptions options = new IdGeneratorOptions((short) assignedWorkerId);
        options.WorkerIdBitLength = (byte) bitLength;
        YitIdHelper.setIdGenerator(options);

        // 注册到 Nutz 的自定义快速设置中，以便在 POJO 上使用 @Prev(els=@EL("snowflake()"))
        CustomMake.me().register("snowflake", this);
    }

    @Override
    public String nextId() {
        return String.valueOf(YitIdHelper.nextId());
    }

    @Scheduled(fixedRate = 60000)
    public void heartbeat() {
        redisTemplate.expire(KEY_PREFIX + assignedWorkerId, Duration.ofMinutes(5));
    }

    @PreDestroy
    public void release() {
        redisTemplate.delete(KEY_PREFIX + assignedWorkerId);
    }

    @Override
    public Object run(List<Object> list) {
        return nextId();
    }

    @Override
    public String fetchSelf() {
        return "snowflake";
    }
}