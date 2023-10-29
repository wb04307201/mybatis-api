package cn.wubo.mybatis.api.core.result.impl;

import cn.wubo.mybatis.api.core.result.IResultService;

public class NoneResultServiceImpl implements IResultService<Object> {
    @Override
    public Object generalResult(Object obj) {
        return obj;
    }
}
