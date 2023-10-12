package cn.wubo.mybatis.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mybatis.api")
public class MyBatisApiProperties {
    private String basePath = "api";
    private String id = "id";
    private String idClass = "cn.wubo.mybatis.api.core.id.impl.UUIDServiceImpl";
}
