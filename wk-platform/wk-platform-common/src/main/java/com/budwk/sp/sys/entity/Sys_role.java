package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.List;

/**
 * 系统角色表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_SYS_ROLE_CODE", fields = {"code"}, unique = true)})
public class Sys_role extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -3044639976455798647L;
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
    @Comment("角色名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    private String name;

    @Column
    @Comment("角色代码")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String code;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;

    @Column
    @Comment("单位ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String unitId;

    @Column
    @Comment("分组ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String groupId;

    @Column
    @Comment("角色备注")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String note;

    @One(field = "unitId")
    public Sys_unit unit;

    @One(field = "createdBy")
    public Sys_user createdByUser;

    @One(field = "groupId")
    protected Sys_group group;

    @ManyMany(from = "roleId", relation = "sys_role_menu", to = "menuId")
    protected List<Sys_menu> menus;

    @ManyMany(from = "roleId", relation = "sys_role_app", to = "appId")
    protected List<Sys_app> apps;

    @ManyMany(from = "roleId", relation = "sys_role_user", to = "userId")
    private List<Sys_user> users;

}
