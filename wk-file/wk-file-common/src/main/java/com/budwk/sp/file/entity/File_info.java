package com.budwk.sp.file.entity;

import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.starter.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.ColDefine;
import org.nutz.dao.entity.annotation.ColType;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.entity.annotation.EL;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;
import org.nutz.dao.entity.annotation.TableMeta;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("file_info_${tenantKey}")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class File_info extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Name
    @Column
    @Comment("ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    @PrevInsert(els = {@EL("snowflake()")})
    private String id;

    @Column
    @Comment("租户ID")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String tenantId;

    @Column
    @Comment("原始文件名")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String fileName;

    @Column
    @Comment("文件后缀")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String suffix;

    @Column
    @Comment("MIME类型")
    @ColDefine(type = ColType.VARCHAR, width = 128)
    private String contentType;

    @Column
    @Comment("文件大小")
    private long size;

    @Column
    @Comment("分类")
    @ColDefine(type = ColType.VARCHAR, width = 32)
    private String category;

    @Column
    @Comment("存储类型")
    @ColDefine(type = ColType.VARCHAR, width = 16)
    private FileStorageType storageType;

    @Column
    @Comment("存储键")
    @ColDefine(type = ColType.VARCHAR, width = 500)
    private String storageKey;

    @Column
    @Comment("公开访问路径")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String accessPath;

    @Column
    @Comment("是否公开预览")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean publicFlag;

    @Column
    @Comment("是否图片")
    @ColDefine(type = ColType.BOOLEAN)
    private boolean imageFlag;

    @Column
    @Comment("上传人登录名")
    @ColDefine(type = ColType.VARCHAR, width = 120)
    private String createdByLoginname;

    @Column
    @Comment("上传人姓名")
    @ColDefine(type = ColType.VARCHAR, width = 100)
    private String createdByUsername;
}
