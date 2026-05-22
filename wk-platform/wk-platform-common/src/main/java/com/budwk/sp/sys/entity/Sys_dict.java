package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.DB;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 数据字典
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dict")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_SYS_DICT_PATH", fields = {"path"}, unique = true),
        @Index(name = "INDEX_SYS_DICT_CODE", fields = {"code"}, unique = true)})
public class Sys_dict extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1210793382290366000L;
    @Column
    @Name
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(uu32 = true)
    private String id;

    @Column
    @Comment("父级ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String parentId;

    @Column
    @Comment("树路径")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String path;

    @Column
    @Comment("字典名称")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String name;

    @Column
    @Comment("字典编码")
    @ColDefine(type = ColType.VARCHAR, width = 20)
    private String code;

    @Column
    @Comment("启用状态")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;

    @Column
    @Comment("排序字段")
    @Prev({
            @SQL(db = DB.MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM sys_dict"),
            @SQL(db = DB.DM_MYSQL, value = "SELECT IFNULL(MAX(location),0)+1 FROM sys_dict"),
            @SQL(db = DB.PSQL, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_dict"),
            @SQL(db = DB.ORACLE, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_dict"),
            @SQL(db = DB.YASHAN, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_dict"),
            @SQL(db = DB.DM, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_dict"),
            @SQL(db = DB.KINGBASE, value = "SELECT COALESCE(MAX(location),0)+1 FROM sys_dict")
    })
    private Integer location;

    @Column
    @Comment("有子节点")
    private boolean hasChildren;

}
