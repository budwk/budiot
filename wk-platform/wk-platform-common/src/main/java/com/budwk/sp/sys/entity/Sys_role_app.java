package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 系统角色应用关联表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_app")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Sys_role_app extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 2202555838954679931L;
    @Column
    @Name
    @PrevInsert(uu32 = true)
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String id;

    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String roleId;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String appId;
}
