package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 用户第三方账号绑定表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_oauth")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_SYS_USER_OAUTH_PROVIDER_OPENID", fields = {"provider", "openId"}, unique = true),
        @Index(name = "INDEX_SYS_USER_OAUTH_USER_PROVIDER", fields = {"userId", "provider"}, unique = true)
})
public class Sys_user_oauth extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column
    @Name
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(uu32 = true)
    private String id;

    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;

    @Column
    @Comment("用户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String userId;

    @Column
    @Comment("平台标识")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String provider;

    @Column
    @Comment("第三方唯一标识")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String openId;

    @Column
    @Comment("UnionId")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String unionId;

    @Column
    @Comment("第三方用户名")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String username;

    @Column
    @Comment("第三方昵称")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String nickname;

    @Column
    @Comment("头像")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String avatar;

    @Column
    @Comment("access_token")
    @ColDefine(type = ColType.VARCHAR, width = 500)
    private String accessToken;

    @Column
    @Comment("refresh_token")
    @ColDefine(type = ColType.VARCHAR, width = 500)
    private String refreshToken;

    @Column
    @Comment("过期时间")
    private Long expireAt;

    @Column
    @Comment("原始数据")
    @ColDefine(type = ColType.TEXT)
    private String rawInfo;

    @One(field = "userId")
    private Sys_user user;
}
