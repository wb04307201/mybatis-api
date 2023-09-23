package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.MyBatisApiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisApiConfiguration {

    @Bean
    public MyBatisApiService myBatisApiService() {
        return new MyBatisApiService();
    }

}
