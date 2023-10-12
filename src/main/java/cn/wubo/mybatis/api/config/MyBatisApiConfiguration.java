package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.MyBatisApiException;
import cn.wubo.mybatis.api.core.MyBatisApiService;
import cn.wubo.mybatis.api.core.id.IDService;
import cn.wubo.mybatis.api.core.id.impl.UUIDServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
@EnableConfigurationProperties({MyBatisApiProperties.class})
@MapperScan(basePackages = "cn.wubo.mybatis.api.core")
public class MyBatisApiConfiguration {

    MyBatisApiProperties myBatisApiProperties;

    public MyBatisApiConfiguration(MyBatisApiProperties myBatisApiProperties) {
        this.myBatisApiProperties = myBatisApiProperties;
    }

    @Bean
    public UUIDServiceImpl uuidService() {
        return new UUIDServiceImpl();
    }

    @Bean
    public MyBatisApiService myBatisApiService(List<IDService<?>> idServices) {
        IDService<?> idService = idServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getIdClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IDService！", myBatisApiProperties.getIdClass())));
        return new MyBatisApiService(myBatisApiProperties,idService);
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
