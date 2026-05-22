package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.desensitize.Desensitize;
import com.budwk.sp.starter.database.desensitize.DesensitizeStrategy;
import com.budwk.sp.starter.database.entity.BaseEntity;
import com.budwk.sp.starter.excel.annotation.Excel;
import com.budwk.sp.starter.excel.annotation.Excels;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.List;

/**
 * 系统用户表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_SYS_USER_LOGINNAMAE", fields = {"loginname","tenantId"}, unique = true)})
public class Sys_user extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -4700089133783566804L;
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
    @Comment("用户编号")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Excel(name = "用户编号", cellType = Excel.ColumnType.STRING, prompt = "用户编号")
    private String serialNo;

    @Column
    @Comment("登录用户名")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    @Excel(name = "用户登录名")
    private String loginname;

    /**
     * transient 修饰符可让此字段不在对象里显示
     */
    @Column
    @Comment("密码")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    @JsonIgnore
    private String password;

    @Column
    @Comment("密码盐")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    @JsonIgnore
    private String salt;

    @Column
    @Comment("用户姓名")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    @Excel(name = "用户姓名")
    private String username;

    @Column
    @Comment("性别")
    @ColDefine(type = ColType.INT, width = 1)
    @Excel(name = "性别", dict = "1=男,2=女,0=未知")
    private Integer sex;

    @Column
    @Comment("头像")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String avatar;

    @Column
    @Comment("用户状态")
    @ColDefine(type = ColType.BOOLEAN)
    @Excel(name = "用户状态", dict = "true=禁用,false=启用")
    private boolean disabled;

    @Column
    @Comment("电子邮箱")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @Desensitize(strategy = DesensitizeStrategy.EMAIL)
    @Excel(name = "电子邮箱")
    private String email;

    @Column
    @Comment("手机号码")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @Desensitize(strategy = DesensitizeStrategy.MOBILE)
    @Excel(name = "手机号码")
    private String mobile;

    @Column
    @Comment("登录时间")
    @Excel(name = "最后登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Long loginAt;

    @Column
    @Comment("登录IP")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    @Excel(name = "最后登录IP", type = Excel.Type.EXPORT)
    private String loginIp;

    @Column
    @Comment("登录次数")
    private Integer loginCount;

    @Column
    @Comment("需要修改密码")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean needChangePwd;

    @Column
    @Comment("是否禁止登录")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabledLogin;

    @Column
    @Comment("禁止登录时间")
    private Long disabledLoginAt;

    @Column
    @Comment("密码重置时间")
    private Long pwdResetAt;

    @Column
    @Comment("布局样式")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String themeConfig;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String companyId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String unitId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String unitPath;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String postId;

    @Excel(name = "单位名称", type = Excel.Type.IMPORT)
    private String importUnitName;

    @One(field = "unitId")
    @Excels({
            @Excel(name = "所属单位", targetAttr = "name", type = Excel.Type.EXPORT)
    })
    private Sys_unit unit;

    @One(field = "postId")
    @Excels({
            @Excel(name = "单位职务", targetAttr = "name", type = Excel.Type.EXPORT)
    })
    private Sys_post post;

    @One(field = "createdBy")
    public Sys_user createdByUser;

    @One(field = "updatedBy")
    public Sys_user updatedByUser;

    @ManyMany(from = "userId", relation = "sys_role_user", to = "roleId")
    private List<Sys_role> roles;

}
