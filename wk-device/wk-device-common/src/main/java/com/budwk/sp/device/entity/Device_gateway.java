package com.budwk.sp.device.entity;

import com.budwk.sp.device.enums.DeviceGatewayMode;
import com.budwk.sp.device.enums.DeviceGatewayStatus;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_gateway")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_GATEWAY_NAME", fields = {"tenantId", "name"}, unique = false),
        @Index(name = "INDEX_DEVICE_GATEWAY_PROTOCOL", fields = {"tenantId", "networkProtocol"}, unique = false),
        @Index(name = "INDEX_DEVICE_GATEWAY_RUNTIME", fields = {"delFlag", "disabled", "runtimeStatus"}, unique = false)
})
public class Device_gateway extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Name
    @Column
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    private String id;

    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;

    @Column
    @Comment("网关名称")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String name;

    @Column
    @Comment("网络协议")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceNetworkProtocol networkProtocol;

    @Column
    @Comment("接入模式")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private DeviceGatewayMode gatewayMode;

    @Column
    @Comment("设备协议ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String protocolId;

    @Column
    @Comment("监听主机")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String host;

    @Column
    @Comment("监听端口")
    private Integer port;

    @Column
    @Comment("访问路径")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String path;

    @Column
    @Comment("远程主机")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String remoteHost;

    @Column
    @Comment("远程端口")
    private Integer remotePort;

    @Column
    @Comment("客户端ID")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String clientId;

    @Column
    @Comment("用户名")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String username;

    @Column
    @Comment("密码")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String password;

    @Column
    @Comment("订阅主题")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String subscribeTopic;

    @Column
    @Comment("发布主题前缀")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String publishTopic;

    @Column
    @Comment("是否允许匿名")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean allowAnonymous;

    @Column
    @Comment("运行状态")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private DeviceGatewayStatus runtimeStatus;

    @Column
    @Comment("是否自动启动")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean autoStart;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;

    @Column
    @Comment("最后启动时间")
    private Long lastStartedAt;

    @Column
    @Comment("最后错误信息")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String lastError;

    @Column
    @Comment("说明")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String description;

    @One(field = "protocolId")
    private Device_protocol protocol;

    private Long lastSeenAt;
}
