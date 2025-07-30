package cn.wubo.mybatis.api.core.service.result.impl;

import cn.wubo.mybatis.api.core.service.result.IResultService;

public class DefaultResultServiceImpl implements IResultService<Object> {
    @Override
    public Object generalResult(Object obj) {
        return obj;
    }
}
