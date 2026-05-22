package com.budwk.sp.sys.controllers.pub;

import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.sys.services.SysAreaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * @author wizzer.cn
 */
@Service
@RequestMapping("/pub/area")
@Slf4j
public class PubAreaController {
    @Autowired
    private SysAreaService sysAreaService;

    @RequestMapping("/list")
    public Result<?> getList(@Param("code") String code, HttpServletRequest req) {
        return Result.data(sysAreaService.getSubListByCode(code));
    }

    @RequestMapping("/map")
    public Result<?> getMap(@Param("code") String code, HttpServletRequest req) {
        return Result.data(sysAreaService.getSubMapByCode(code));
    }
}
