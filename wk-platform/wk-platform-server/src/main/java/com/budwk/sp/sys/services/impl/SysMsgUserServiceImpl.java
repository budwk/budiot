package com.budwk.sp.sys.services.impl;

import com.budwk.sp.sys.entity.Sys_msg_user;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.services.SysMsgUserService;
import org.nutz.dao.Dao;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysMsgUserServiceImpl extends BaseServiceImpl<Sys_msg_user> implements SysMsgUserService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysMsgUserServiceImpl(Dao dao) {
        super(dao);
    }
}
