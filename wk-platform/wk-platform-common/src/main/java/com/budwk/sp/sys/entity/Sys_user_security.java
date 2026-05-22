package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;

import java.io.Serializable;

/**
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_security")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Sys_user_security extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -3044639976455791237L;
    @Column
    @Name
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String id;

    @Column
    @Comment("是否启用")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean hasEnabled;

    @Column
    @Comment("密码最小长度")
    @ColDefine(type = ColType.INT)
    private Integer pwdLengthMin;

    @Column
    @Comment("密码最大长度")
    @ColDefine(type = ColType.INT)
    private Integer pwdLengthMax;

    @Column
    @Comment("密码字符要求")
    @ColDefine(type = ColType.INT, width = 1)
    private Integer pwdCharMust;

    @Column
    @Comment("不能包含的用户信息")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String pwdCharNot;

    @Column
    @Comment("密码重复性检查")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean pwdRepeatCheck;

    @Column
    @Comment("重复性检查记录数")
    @ColDefine(type = ColType.INT)
    private Integer pwdRepeatNum;

    @Column
    @Comment("超重试次数锁定账号")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean pwdRetryLock;

    @Column
    @Comment("密码错误最大重试次数")
    @ColDefine(type = ColType.INT)
    private Integer pwdRetryNum;

    @Column
    @Comment("超重试次数处理方式")
    @ColDefine(type = ColType.INT)
    private Integer pwdRetryAction;

    @Column
    @Comment("密码错误禁止登录时长")
    @ColDefine(type = ColType.INT)
    private Integer pwdRetryTime;

    @Column
    @Comment("指定密码过期时间")
    @ColDefine(type = ColType.INT)
    private Integer pwdTimeoutDay;

    @Column
    @Comment("重置密码后需修改密码")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean pwdResetChange;

    @Column
    @Comment("用户名/手机号输错锁定")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean nameRetryLock;

    @Column
    @Comment("用户名/手机号输错次数")
    @ColDefine(type = ColType.INT)
    private Integer nameRetryNum;

    @Column
    @Comment("输错IP锁定时间(s)")
    @ColDefine(type = ColType.INT)
    private Integer nameTimeout;

    @Column
    @Comment("用户单一登录")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean userSessionOnlyOne;

    @Column
    @Comment("是否启用登录验证码")
    @ColDefine(type = ColType.BOOLEAN)
    private Boolean captchaHasEnabled;

    @Column
    @Comment("验证码类型")
    @ColDefine(type = ColType.INT, width = 1)
    private Integer captchaType;
}
