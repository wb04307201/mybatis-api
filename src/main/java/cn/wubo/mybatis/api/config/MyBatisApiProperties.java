package cn.wubo.mybatis.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mybatis.api")
public class MyBatisApiProperties {
    private String basePath = "/api";
    private String id = "id";
    private String idClass = "cn.wubo.mybatis.api.core.id.impl.UUIDServiceImpl";
    private String mappingClass = "cn.wubo.mybatis.api.core.mapping.impl.LowerCaseMappingServiceImpl";
    private String resultClass = "cn.wubo.mybatis.api.core.result.impl.NoneResultServiceImpl";
    private Boolean enableRouter = Boolean.TRUE;
}
