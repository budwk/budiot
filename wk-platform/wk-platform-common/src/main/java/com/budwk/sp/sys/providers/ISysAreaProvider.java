package com.budwk.sp.sys.providers;

import com.budwk.sp.sys.entity.Sys_area;
import org.nutz.lang.util.NutMap;

import java.util.List;

public interface ISysAreaProvider {
    
    /**
     * 通过code获取子级
     *
     * @param code 标识
     * @return
     */
    List<Sys_area> getSubListByCode(String code);

    /**
     * 通过code获取子级(code为空返回第一级)
     *
     * @param filedName 字段名
     * @param code 标识
     * @return
     */
    List<Sys_area> getSubListByCode(String filedName, String code);
    /**
     * 通过code获取子级
     *
     * @param code 标识
     * @return
     */
    NutMap getSubMapByCode(String code);
}
