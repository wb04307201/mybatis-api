package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.MyBatisApiService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
@EnableConfigurationProperties({MyBatisApiProperties.class})
public class MyBatisApiConfiguration {

    MyBatisApiProperties myBatisApiProperties;

    public MyBatisApiConfiguration(MyBatisApiProperties myBatisApiProperties) {
        this.myBatisApiProperties = myBatisApiProperties;
    }

    @Bean
    public MyBatisApiService myBatisApiService() {
        return new MyBatisApiService();
    }

    @Bean
    public RouterFunction<ServerResponse> myBatisApiRouter(MyBatisApiService myBatisApiService) {
        return route().POST("/" + myBatisApiProperties.getBasePath() + "/{method}/{tableName}", accept(MediaType.APPLICATION_JSON), request -> {
            String method = request.pathVariable("method");
            String tableName = request.pathVariable("tableName");
            return ServerResponse.status(HttpStatus.OK).body(myBatisApiService.parse(method, tableName, request.body(String.class)));
        }).build();
    }

}
