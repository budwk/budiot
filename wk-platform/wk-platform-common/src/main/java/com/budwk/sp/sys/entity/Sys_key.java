package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

/**
 * 系统密钥表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_key")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({@Index(name = "INDEX_SYS_KEY", fields = {"appid"}, unique = true)})
public class Sys_key extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 5153785005013490045L;

    @Column
    @Name
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(uu32 = true)
    private String id;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String appid;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String appkey;

    @Column
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String name;

    @Column
    @Comment("是否禁用")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean disabled;
}
