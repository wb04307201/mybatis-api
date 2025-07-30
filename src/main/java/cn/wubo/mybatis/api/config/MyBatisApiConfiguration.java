package cn.wubo.mybatis.api.config;

import cn.wubo.mybatis.api.core.mapper.MyBatisApiMapper;
import cn.wubo.mybatis.api.core.service.MyBatisApiService;
import cn.wubo.mybatis.api.core.service.id.IDService;
import cn.wubo.mybatis.api.core.service.id.impl.DefaultIDServiceImpl;
import cn.wubo.mybatis.api.core.service.mapping.IMappingService;
import cn.wubo.mybatis.api.core.service.mapping.impl.DefaultMappingServiceImpl;
import cn.wubo.mybatis.api.core.service.result.IResultService;
import cn.wubo.mybatis.api.core.service.result.impl.DefaultResultServiceImpl;
import cn.wubo.mybatis.api.exception.MyBatisApiException;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.lang.reflect.InvocationTargetException;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
@EnableConfigurationProperties({MyBatisApiProperties.class})
@MapperScan(basePackages = "cn.wubo.mybatis.api.core.mapper")
public class MyBatisApiConfiguration {

    MyBatisApiProperties myBatisApiProperties;

    public MyBatisApiConfiguration(MyBatisApiProperties myBatisApiProperties) {
        this.myBatisApiProperties = myBatisApiProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public IDService<?> idService() {
        return new DefaultIDServiceImpl();
    }


    @Bean
    @ConditionalOnMissingBean
    public IMappingService mappingService() {
        return new DefaultMappingServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public IResultService<?> resultService() {
        return new DefaultResultServiceImpl();
    }


        /**
     * 创建MyBatisApiService实例的Bean
     *
     * @param idService ID服务实例，用于生成唯一标识符
     * @param mappingService 映射服务实例，用于数据映射转换
     * @param resultService 结果服务实例，用于处理返回结果
     * @return 配置好的MyBatisApiService实例
     */
    @Bean
    public MyBatisApiService myBatisApiService(IDService<?> idService, IMappingService mappingService, IResultService<?> resultService,MyBatisApiMapper mapper) {
        return new MyBatisApiService(myBatisApiProperties, idService, mappingService, resultService, mapper);
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
        if (!Boolean.TRUE.equals(myBatisApiProperties.getEnableRouter())) {
            return builder.build();
        }

        String basePath = myBatisApiProperties.getBasePath();

        // 判断basePath是否以"/"开头，并且不以"/"结尾
        if (!basePath.startsWith("/") || basePath.endsWith("/")) {
            throw new MyBatisApiException("basePath must start with '/' and not end with '/'");
        }

        final String routePath = basePath + "/{method}/{tableName}";

        // 处理POST请求
        builder.POST(routePath, accept(MediaType.APPLICATION_JSON), request -> {
            String method = request.pathVariable("method");
            String tableName = request.pathVariable("tableName");

            try {
                // 调用MyBatisApiService的parse方法
                return ServerResponse.ok()
                        .body(myBatisApiService.parse(method, tableName, request.body(String.class)));
            } catch (Exception e) {
                // 异常处理：统一返回400错误
                return ServerResponse.badRequest()
                        .body("Internal Server Error: " + e.getMessage());
            }
        });

        // 构建并返回路由函数
        return builder.build();
    }
}
