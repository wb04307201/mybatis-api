package cn.wubo.mybatis.api.core.id.impl;

import cn.wubo.mybatis.api.core.id.IDService;

import java.util.UUID;

public class UUIDServiceImpl implements IDService<String> {
    @Override
    public String generalID() {
        return UUID.randomUUID().toString();
    }
}
