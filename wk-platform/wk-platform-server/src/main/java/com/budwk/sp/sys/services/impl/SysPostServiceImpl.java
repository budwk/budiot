package com.budwk.sp.sys.services.impl;

import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.sys.dto.SysPostDTO;
import com.budwk.sp.sys.entity.Sys_post;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.enums.SysMsgType;
import com.budwk.sp.sys.providers.ISysMsgProvider;
import com.budwk.sp.sys.services.SysPostService;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
@Service
public class SysPostServiceImpl extends BaseServiceImpl<Sys_post> implements SysPostService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SysPostServiceImpl(Dao dao) {
        super(dao);
    }

    @Autowired
    private ISysMsgProvider sysMsgProvider;

    public void importData(String fileName, List<SysPostDTO> list, boolean over, String userId, String loginname) {
        if (list == null || list.size() == 0) {
            throw new BaseException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder resultMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysPostDTO dto : list) {
            Sys_post obj = new Sys_post();
            obj.setCode(Strings.sNull(dto.getCode()).trim());
            obj.setName(Strings.sNull(dto.getName()).trim());
            obj.setCreatedBy(userId);
            obj.setUpdatedBy(userId);
            if (over) {
                int have = this.count(Cnd.where("code", "=", obj.getCode()));
                if (have > 0) {
                    this.update(Chain.make("name", obj.getName())
                            .add("updatedBy", userId)
                            .add("updatedAt", System.currentTimeMillis()), Cnd.where("code", "=", obj.getCode())
                    );
                    successNum++;
                } else {
                    try {
                        this.insert(obj);
                        successNum++;
                    } catch (Exception e) {
                        failureNum++;
                        String msg = "<br/>" + failureNum + "、职务名称：" + obj.getName() + "<br/>";
                        failureMsg.append(msg + e.getMessage());
                    }
                }
            } else {
                try {
                    this.insert(obj);
                    successNum++;
                } catch (Exception e) {
                    failureNum++;
                    String msg = "<br/>" + failureNum + "、职务名称：" + obj.getName() + "<br/>";
                    failureMsg.append(msg + e.getMessage());
                }
            }
        }
        resultMsg.insert(0, "导入结果：共成功 " + successNum + " 条");
        if (failureNum > 0) {
            resultMsg.append("，失败 " + failureNum + " 条，失败数据如下：<br/>");
            resultMsg.append(failureMsg);
        }
        sysMsgProvider.sendMsg(userId, SysMsgType.USER, "职务导入 " + fileName + " 完成", "", resultMsg.toString(), userId);
    }
}
