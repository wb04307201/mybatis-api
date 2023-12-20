package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.MyBatisApiService;
import cn.wubo.mybatis.api.core.id.IDService;
import cn.wubo.mybatis.api.core.id.impl.UUIDServiceImpl;
import cn.wubo.mybatis.api.core.mapping.IMappingService;
import cn.wubo.mybatis.api.core.mapping.impl.LowerCaseMappingServiceImpl;
import cn.wubo.mybatis.api.core.result.IResultService;
import cn.wubo.mybatis.api.core.result.impl.NoneResultServiceImpl;
import cn.wubo.mybatis.api.exception.MyBatisApiException;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
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

    /**
     * 创建一个MyBatisApiService对象
     *
     * @param idServices      ID服务的列表
     * @param mappingServices 映射服务的列表
     * @param iResultServices 结果服务的列表
     * @return 创建的MyBatisApiService对象
     */
    @Bean
    public MyBatisApiService myBatisApiService(List<IDService<?>> idServices, List<IMappingService> mappingServices, List<IResultService<?>> iResultServices) {
        // 通过名称找到对应的IDService对象
        IDService<?> idService = idServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getIdClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IDService！", myBatisApiProperties.getIdClass())));
        // 通过名称找到对应的IMappingService对象
        IMappingService mappingService = mappingServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getMappingClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IMappingService！", myBatisApiProperties.getMappingClass())));
        // 通过名称找到对应的IResultService对象
        IResultService<?> resultService = iResultServices.stream().filter(is -> is.getClass().getName().equals(myBatisApiProperties.getResultClass())).findAny().orElseThrow(() -> new MyBatisApiException(String.format("未找到%s对应的bean，无法加载IResultService！", myBatisApiProperties.getResultClass())));
        // 创建MyBatisApiService对象并返回
        return new MyBatisApiService(myBatisApiProperties, idService, mappingService, resultService);
    }


    /**
     * 注册一个Bean，用于路由MyBatisApiService的请求
     *
     * @param myBatisApiService MyBatisApiService对象
     * @return 路由函数
     */
    @Bean
    public RouterFunction<ServerResponse> myBatisApiRouter(MyBatisApiService myBatisApiService) {
        RouterFunctions.Builder builder = route();

        // 判断是否启用路由
        if (Boolean.TRUE.equals(myBatisApiProperties.getEnableRouter())) {

            // 判断basePath是否以"/"开头，并且不以"/"结尾
            if (!myBatisApiProperties.getBasePath().startsWith("/") || myBatisApiProperties.getBasePath().endsWith("/"))
                throw new MyBatisApiException("basePath must start with '/' and not end with '/'");

            // 处理POST请求
            builder.POST(myBatisApiProperties.getBasePath() + "/{method}/{tableName}", accept(MediaType.APPLICATION_JSON), request -> {
                String method = request.pathVariable("method");
                String tableName = request.pathVariable("tableName");

                // 调用MyBatisApiService的parse方法
                return ServerResponse.status(HttpStatus.OK).body(myBatisApiService.parse(method, tableName, request.body(String.class)));
            });
        }

        // 构建并返回路由函数
        return builder.build();
    }

}
