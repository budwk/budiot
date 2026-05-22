package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.sys.entity.Sys_msg;
import com.budwk.sp.sys.entity.Sys_msg_user;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.enums.SysMsgScope;
import com.budwk.sp.sys.providers.ISysMsgProvider;
import com.budwk.sp.sys.services.SysMsgService;
import com.budwk.sp.sys.services.SysMsgUserService;
import com.budwk.sp.sys.services.SysUserService;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.pager.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysMsgServiceImpl extends BaseServiceImpl<Sys_msg> implements SysMsgService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysMsgServiceImpl(Dao dao) {
        super(dao);
    }

    @Autowired
    private SysMsgUserService sysMsgUserService;

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    private ObjectProvider<ISysMsgProvider> sysMsgProvider;

    @Override
    public int getUnreadNum(String userId) {
        int size = sysMsgUserService.count(Cnd.where("delFlag", "=", false).and("userId", "=", userId)
                .and("status", "=", 0));
        return size;
    }

    @Override
    public List<Sys_msg_user> getUnreadList(String userId, int pageNumber, int pageSize) {
        return sysMsgUserService.query(Cnd.where("delFlag", "=", false).and("userId", "=", userId)
                .and("status", "=", 0)
                .desc("createdAt"), "msg", Cnd.orderBy().desc("sendAt"), new Pager().setPageNumber(pageNumber).setPageSize(pageSize));

    }

    @Override
    public void deleteMsg(String id) {
        this.vDelete(id);
        sysMsgUserService.vDelete(Cnd.where("msgId", "=", id));
    }

    @Override
    public void saveMsg(Sys_msg msg, String[] ids) {
        if (!StringUtils.hasText(msg.getTenantId())) {
            msg.setTenantId(com.budwk.sp.starter.common.constant.GlobalConstant.TENANT_ID_DEFAULT);
        }
        Sys_msg dbMsg = this.insert(msg);
        if (dbMsg != null) {
            ISysMsgProvider provider = sysMsgProvider.getIfAvailable();
            if (SysMsgScope.SCOPE.equals(dbMsg.getScope()) && ids != null) {
                for (String userId : ids) {
                    if (!StringUtils.hasText(userId)) {
                        continue;
                    }
                    Sys_user user = sysUserService.fetch(userId);
                    if (user == null) {
                        continue;
                    }
                    Sys_msg_user sys_msg_user = new Sys_msg_user();
                    sys_msg_user.setTenantId(StringUtils.hasText(user.getTenantId()) ? user.getTenantId() : dbMsg.getTenantId());
                    sys_msg_user.setMsgId(dbMsg.getId());
                    sys_msg_user.setStatus(0);
                    sys_msg_user.setCreatedBy(dbMsg.getCreatedBy());
                    sys_msg_user.setUserId(user.getId());
                    sys_msg_user.setLoginname(user.getLoginname());
                    sys_msg_user.setUsername(user.getUsername());
                    sysMsgUserService.insert(sys_msg_user);
                    if (provider != null) {
                        provider.getMsg(userId, true);
                    }
                }
            }
            if (SysMsgScope.ALL.equals(dbMsg.getScope())) {
                Cnd cnd = Cnd.where("disabled", "=", false).and("delFlag", "=", false);
                int total = sysUserService.count(cnd);
                int size = 500;
                Pagination pagination = new Pagination();
                pagination.setTotalCount(total);
                pagination.setPageSize(size);
                for (int i = 1; i <= pagination.getTotalPage(); i++) {
                    Pagination pagination2 = sysUserService.listPage(i, size, cnd);
                    for (Sys_user user : pagination2.getList(Sys_user.class)) {
                        Sys_msg_user sys_msg_user = new Sys_msg_user();
                        sys_msg_user.setTenantId(StringUtils.hasText(user.getTenantId()) ? user.getTenantId() : dbMsg.getTenantId());
                        sys_msg_user.setMsgId(dbMsg.getId());
                        sys_msg_user.setStatus(0);
                        sys_msg_user.setCreatedBy(dbMsg.getCreatedBy());
                        sys_msg_user.setUserId(user.getId());
                        sys_msg_user.setLoginname(user.getLoginname());
                        sys_msg_user.setUsername(user.getUsername());
                        sysMsgUserService.insert(sys_msg_user);
                        if (provider != null) {
                            provider.getMsg(user.getId(), true);
                        }
                    }
                }
            }
        }
    }

}
