package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.DB;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 系统员工职务表
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_post")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Sys_post extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -402450821083980149L;
    @Column
    @Name
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(uu32 = true)
    private String id;

    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;

    @Column
    @Comment("职务名称")
    @ColDefine(type = ColType.VARCHAR, width = 50)
    private String name;

    @Column
    @Comment("职务编号")
    @ColDefine(type = ColType.VARCHAR, width = 25)
    private String code;

    @Column
    @Comment("排序字段")
    @Prev({
            @SQL(db = DB.MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM sys_post"),
            @SQL(db = DB.DM_MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM sys_post"),
            @SQL(db = DB.PSQL, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_post"),
            @SQL(db = DB.ORACLE, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_post"),
            @SQL(db = DB.YASHAN, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_post"),
            @SQL(db = DB.DM, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_post"),
            @SQL(db = DB.KINGBASE, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_post")
    })
    private Integer location;
}
