package com.budwk.sp.sys.services;

import com.budwk.sp.sys.dto.SysPostDTO;
import com.budwk.sp.sys.entity.Sys_post;
import com.budwk.sp.starter.database.service.BaseService;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
public interface SysPostService extends BaseService<Sys_post> {
    void importData(String fileName, List<SysPostDTO> list, boolean over, String userId, String loginname);
}
