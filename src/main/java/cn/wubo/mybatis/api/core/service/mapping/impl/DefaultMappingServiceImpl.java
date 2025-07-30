package cn.wubo.mybatis.api.core.service.mapping.impl;

import cn.wubo.mybatis.api.core.service.mapping.IMappingService;

public class DefaultMappingServiceImpl implements IMappingService {

    @Override
    public String parseKey(String field) {
        return field.toLowerCase();
    }
}
