package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.DB;
import org.nutz.dao.entity.annotation.*;

import java.io.Serializable;

/**
 * 系统应用表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_app")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Sys_app extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 7641962702833856043L;

    @Name
    @Column
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String id;

    @Column
    @Comment("应用名称")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String name;

    @Column
    @Comment("应用路径")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String path;

    @Column
    @Comment("应用图标")
    @ColDefine(type = ColType.TEXT)
    private String icon;

    @Column
    @Comment("是否隐藏")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean hidden;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;

    @Column
    @Comment("排序字段")
    @Prev({
            @SQL(db = DB.MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM sys_app"),
            @SQL(db = DB.DM_MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM sys_app"),
            @SQL(db = DB.PSQL, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_app"),
            @SQL(db = DB.ORACLE, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_app"),
            @SQL(db = DB.YASHAN, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_app"),
            @SQL(db = DB.DM, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_app"),
            @SQL(db = DB.KINGBASE, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_app")
    })
    private Integer location;
}
