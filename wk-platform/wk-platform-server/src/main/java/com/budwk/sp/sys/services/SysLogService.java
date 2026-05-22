package com.budwk.sp.sys.services;

import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseService;
import com.budwk.sp.starter.log.model.SLogRecord;
import com.budwk.sp.sys.entity.Sys_log;

/**
 * 系统日志服务
 *
 * @author wizzer@qq.com
 */
public interface SysLogService extends BaseService<Sys_log> {

    Pagination getLogPage(String appId, String type, String status, String loginname, String username,
                          String tag, String msg, Long beginTime, Long endTime,
                          int pageNo, int pageSize, String pageOrderName, String pageOrderBy);

    Pagination getUserLogPage(String userId, int pageNo, int pageSize, String pageOrderName, String pageOrderBy);

    boolean deleteLog(String id, Long createdAt);

    int clearLogs(String appId, String type, String status, String loginname, String username,
                  String tag, String msg, Long beginTime, Long endTime);

    void save(SLogRecord record);
}
