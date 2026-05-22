package com.budwk.sp.sys.providers;

import com.budwk.sp.starter.log.model.SLogRecord;
import com.budwk.sp.starter.log.providers.ISysLogProvider;
import com.budwk.sp.sys.services.SysLogService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 系统日志 Provider
 *
 * @author wizzer@qq.com
 */
@Primary
@Service
@DubboService(interfaceClass = ISysLogProvider.class)
public class SysLogProvider implements ISysLogProvider {

    @Autowired
    private SysLogService sysLogService;

    @Async
    public void save(SLogRecord record) {
        sysLogService.save(record);
    }
}
