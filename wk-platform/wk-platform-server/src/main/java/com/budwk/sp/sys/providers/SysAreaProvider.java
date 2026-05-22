package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.entity.Sys_area;
import com.budwk.sp.sys.services.SysAreaService;
import org.apache.dubbo.config.annotation.DubboService;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
@DubboService(interfaceClass = ISysAreaProvider.class)
@Service
public class SysAreaProvider implements ISysAreaProvider {
    
    @Autowired
    private SysAreaService sysAreaService;

    /**
     * 通过code获取子级(code为空返回第一级)
     *
     * @param code 标识
     * @return
     */
    public List<Sys_area> getSubListByCode(String code) {
        return sysAreaService.getSubListByCode(code);
    }

    /**
     * 通过code获取子级(code为空返回第一级)
     *
     * @param filedName 字段名
     * @param code 标识
     * @return
     */
    public List<Sys_area> getSubListByCode(String filedName, String code) {
        return sysAreaService.getSubListByCode(filedName, code);
    }


    /**
     * 通过code获取子级(code为空返回第一级)
     *
     * @param code 标识
     * @return
     */
    public NutMap getSubMapByCode(String code) {
        return sysAreaService.getSubMapByCode(code);
    }
}
