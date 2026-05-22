package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 系统日志表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_log_${month}")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Sys_log extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -8207922146920142428L;

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
    @Comment("应用ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String appId;

    @Column
    @Comment("用户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String userId;

    @Column
    @Comment("日志类型")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String type;

    @Column
    @Comment("功能模块")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String tag;

    @Column
    @Comment("日志内容")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String msg;

    @Column
    @Comment("登录用户名")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String loginname;

    @Column
    @Comment("用户姓名")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String username;

    @Column
    @Comment("请求IP")
    @ColDefine(type = ColType.VARCHAR, width = 64)
    private String ip;

    @Column
    @Comment("请求路径")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String url;

    @Column
    @Comment("执行方法")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String method;

    @Column
    @Comment("操作系统")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String os;

    @Column
    @Comment("浏览器")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String browser;

    @Column
    @Comment("请求参数")
    @ColDefine(type = ColType.TEXT)
    private String params;

    @Column
    @Comment("执行结果")
    @ColDefine(type = ColType.TEXT)
    private String result;

    @Column
    @Comment("异常信息")
    @ColDefine(type = ColType.TEXT)
    private String exception;

    @Column
    @Comment("执行耗时(ms)")
    private Long executeTime;
}
