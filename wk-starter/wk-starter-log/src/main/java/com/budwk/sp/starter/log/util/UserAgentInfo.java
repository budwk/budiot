package com.budwk.sp.starter.log.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * User-Agent 解析结果
 *
 * @author wizzer@qq.com
 */
@Data
@AllArgsConstructor
public class UserAgentInfo {

    private String browser;
    private String os;
}
