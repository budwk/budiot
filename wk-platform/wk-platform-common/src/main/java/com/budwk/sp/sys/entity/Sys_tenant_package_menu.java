package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_tenant_package_menu")
@TableMeta("{'mysql-charset':'utf8mb4'}")
@TableIndexes({
        @Index(name = "INDEX_SYS_TENANT_PACKAGE_MENU", fields = {"packageId", "appId", "menuId"}, unique = true)
})
public class Sys_tenant_package_menu extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Name
    @Column
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(uu32 = true)
    private String id;

    @Column
    @Comment("套餐ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String packageId;

    @Column
    @Comment("应用ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String appId;

    @Column
    @Comment("菜单ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String menuId;
}
