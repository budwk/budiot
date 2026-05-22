package com.budwk.sp.sys.services;

import com.budwk.sp.sys.entity.Sys_key;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseService;

/**
 * @author wizzer@qq.com
 */
public interface SysKeyService extends BaseService<Sys_key> {

    /**
     * 创建密钥
     *
     * @param name   应用名称
     * @param userId appid
     * @throws Exception
     */
    void createAppkey(String name, String userId) throws BaseException;

    /**
     * 删除密钥
     *
     * @param appid appid
     * @throws Exception
     */
    void deleteAppkey(String appid) throws BaseException;

    /**
     * 启用禁用
     *
     * @param appid    appid
     * @param disabled true为禁用
     * @throws Exception
     */
    void updateAppkey(String appid, boolean disabled, String userId) throws BaseException;

    /**
     * 获取appkey
     *
     * @param appid appid
     * @return
     */
    String getAppkey(String appid);
    /**
     * 清空缓存
     */
    void cacheClear();
}
