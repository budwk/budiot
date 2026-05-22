package com.budwk.sp.starter.log.providers;

import com.budwk.sp.starter.log.model.SLogRecord;

/**
 * 系统日志服务
 *
 * @author wizzer@qq.com
 */
public interface ISysLogProvider {

    void save(SLogRecord record);
}
