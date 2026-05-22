package com.budwk.sp.starter.database.idgen;

import org.nutz.el.opt.RunMethod;

public interface IdService extends RunMethod {
    String nextId(); // 考虑到 UUID 是字符串，统一返回 String
}