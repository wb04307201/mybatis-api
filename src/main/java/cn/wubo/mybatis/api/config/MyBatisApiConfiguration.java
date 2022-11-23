package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.MyBatisApiService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@MapperScan({"cn.wubo.mybatis.json.api"})
@Configuration
public class MyBatisApiConfiguration {

    @Bean
    public MyBatisApiService robotService() {
        return new MyBatisApiService();
    }

}
