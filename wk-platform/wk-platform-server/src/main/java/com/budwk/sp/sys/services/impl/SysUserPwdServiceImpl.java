package com.budwk.sp.sys.services.impl;

import com.budwk.sp.sys.entity.Sys_user_pwd;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.services.SysUserPwdService;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.springframework.stereotype.Service;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysUserPwdServiceImpl extends BaseServiceImpl<Sys_user_pwd> implements SysUserPwdService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysUserPwdServiceImpl(Dao dao) {
        super(dao);
    }

}
