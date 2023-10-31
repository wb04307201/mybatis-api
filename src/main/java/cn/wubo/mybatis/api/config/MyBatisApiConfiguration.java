package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.result.IResultService;
import cn.wubo.mybatis.api.core.result.impl.NoneResultServiceImpl;
import cn.wubo.mybatis.api.exception.MyBatisApiException;
import cn.wubo.mybatis.api.core.MyBatisApiService;
import cn.wubo.mybatis.api.core.id.IDService;
import cn.wubo.mybatis.api.core.id.impl.UUIDServiceImpl;
import cn.wubo.mybatis.api.core.mapping.IMappingService;
import cn.wubo.mybatis.api.core.mapping.impl.LowerCaseMappingServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    public IDService<?> uuidService() {
        return new UUIDServiceImpl();
    }

    @Bean
    public IMappingService lowerCaseMappingServiceImpl() {
        return new LowerCaseMappingServiceImpl();
    }

    @Bean
    public IResultService<?> noneResultServiceImpl() {
        return new NoneResultServiceImpl();
    }

    @Bean
    public MyBatisApiService myBatisApiService(List<IDService<?>> idServices, List<IMappingService> mappingServices,List<IResultService<?>> iResultServices) {
        IDService<?> idService = idServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getIdClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IDService！", myBatisApiProperties.getIdClass())));
        IMappingService mappingService = mappingServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getMappingClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IMappingService！", myBatisApiProperties.getMappingClass())));
        IResultService<?> resultService = iResultServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getResultClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IResultService！", myBatisApiProperties.getResultClass())));
        return new MyBatisApiService(myBatisApiProperties, idService, mappingService,resultService);
    }

    @Bean
    @ConditionalOnExpression("'true'.equals('${mybatis.api.enableRouter}')")
    public RouterFunction<ServerResponse> myBatisApiRouter(MyBatisApiService myBatisApiService) {
        if (!myBatisApiProperties.getBasePath().startsWith("/") || myBatisApiProperties.getBasePath().endsWith("/"))
            throw new MyBatisApiException("basePath must start with '/' and not end with '/'");

        return route().POST(myBatisApiProperties.getBasePath() + "/{method}/{tableName}", accept(MediaType.APPLICATION_JSON), request -> {
            String method = request.pathVariable("method");
            String tableName = request.pathVariable("tableName");
            return ServerResponse.status(HttpStatus.OK).body(myBatisApiService.parse(method, tableName, request.body(String.class)));
        }).build();
    }

}
