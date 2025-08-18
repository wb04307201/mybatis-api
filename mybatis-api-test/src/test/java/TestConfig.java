import cn.wubo.entity.sql.CamelCaseMappingServiceImpl;
import cn.wubo.entity.sql.ResponseResultServiceImpl;
import cn.wubo.entity.sql.SnowflakeIdServiceImpl;
import cn.wubo.mybatis.api.service.id.IDService;
import cn.wubo.mybatis.api.service.mapping.IMappingService;
import cn.wubo.mybatis.api.service.result.IResultService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    @Bean
    public IMappingService mappingService() {
        return new CamelCaseMappingServiceImpl();
    }

    @Bean
    public IDService idService() {
        return new SnowflakeIdServiceImpl();
    }

    @Bean
    public IResultService resultService() {
        return new ResponseResultServiceImpl();
    }

}


