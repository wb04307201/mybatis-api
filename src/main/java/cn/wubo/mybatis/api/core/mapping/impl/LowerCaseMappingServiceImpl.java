package cn.wubo.mybatis.api.core.mapping.impl;

import cn.wubo.mybatis.api.core.mapping.IMappingService;

public class LowerCaseMappingServiceImpl implements IMappingService {

    @Override
    public String parseKey(String field) {
        return field.toLowerCase();
    }
}
