package cn.wubo.mybatis.api.service.id.impl;

import cn.wubo.mybatis.api.service.id.IDService;

import java.util.UUID;

public class DefaultIDServiceImpl implements IDService<String> {
    @Override
    public String generalID() {
        return UUID.randomUUID().toString();
    }
}
