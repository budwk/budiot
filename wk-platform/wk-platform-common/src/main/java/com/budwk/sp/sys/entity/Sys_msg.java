package com.budwk.sp.sys.entity;

import com.budwk.sp.starter.database.entity.BaseEntity;
import com.budwk.sp.sys.enums.SysMsgScope;
import com.budwk.sp.sys.enums.SysMsgType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.*;
import org.nutz.dao.interceptor.annotation.PrevInsert;

import java.io.Serializable;
import java.util.List;

/**
 * 站内消息表
 *
 * @author wizzer@qq.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_msg")
@TableMeta("{'mysql-charset':'utf8mb4'}")
public class Sys_msg extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 6416003551694659705L;
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
    @Comment("消息类型")
    @ColDefine(type = ColType.VARCHAR, width = 10)
    private SysMsgType type;

    @Column
    @Comment("发送范围")
    @ColDefine(type = ColType.VARCHAR, width = 10)
    private SysMsgScope scope;

    @Column
    @Comment("消息标题")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String title;

    @Column
    @Comment("消息内容(500字节以内)")
    @ColDefine(type = ColType.VARCHAR,width = 500)
    private String note;

    @Column
    @Comment("跳转链接")
    @ColDefine(type = ColType.VARCHAR, width = 255)
    private String url;

    @Column
    @Comment("发送时间")
    private Long sendAt;

    @Many(field = "msgId")
    private List<Sys_msg_user> userList;

    @One(field = "createdBy")
    public Sys_user createdByUser;

}
