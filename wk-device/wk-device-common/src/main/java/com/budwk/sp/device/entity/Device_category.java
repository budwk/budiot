package com.budwk.sp.device.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.DB;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("device_category")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_DEVICE_CATEGORY_PATH", fields = {"tenantId", "path"}, unique = true),
        @Index(name = "INDEX_DEVICE_CATEGORY_CODE", fields = {"tenantId", "code"}, unique = true)
})
public class Device_category extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column
    @Name
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    private String id;
    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;
    @Column
    @Comment("父级ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String parentId;
    @Column
    @Comment("树路径")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String path;
    @Column
    @Comment("分类名称")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String name;
    @Column
    @Comment("分类编码")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String code;
    @Column
    @Comment("启用状态")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
    @Column
    @Comment("排序字段")
    @Prev({
            @SQL(db = DB.MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM device_category"),
            @SQL(db = DB.DM_MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM device_category"),
            @SQL(db = DB.PSQL, value = "SELECT COALESCE(MAX(location),0)+1 FROM device_category"),
            @SQL(db = DB.ORACLE, value = "SELECT COALESCE(MAX(location),0)+1 FROM device_category"),
            @SQL(db = DB.YASHAN, value = "SELECT COALESCE(MAX(location),0)+1 FROM device_category"),
            @SQL(db = DB.DM, value = "SELECT COALESCE(MAX(location),0)+1 FROM device_category"),
            @SQL(db = DB.KINGBASE, value = "SELECT COALESCE(MAX(location),0)+1 FROM device_category")
    })
    private Integer location;
    @Column
    @Comment("有子节点")
    private boolean hasChildren;
}
