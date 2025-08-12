package cn.wubo.mybatis.api;

import cn.wubo.mybatis.api.mapper.MyBatisApiMapper;
import cn.wubo.mybatis.api.service.MyBatisApiService;
import cn.wubo.mybatis.api.service.id.IDService;
import cn.wubo.mybatis.api.service.id.impl.DefaultIDServiceImpl;
import cn.wubo.mybatis.api.service.mapping.IMappingService;
import cn.wubo.mybatis.api.service.mapping.impl.DefaultMappingServiceImpl;
import cn.wubo.mybatis.api.service.result.IResultService;
import cn.wubo.mybatis.api.service.result.impl.DefaultResultServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@AutoConfiguration
@EnableConfigurationProperties({MyBatisApiProperties.class})
public class MyBatisApiConfiguration {

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

    @Bean
    public MapperFactoryBean<MyBatisApiMapper> myBatisApiMapperMapperFactoryBean(SqlSessionFactory sqlSessionFactory) throws Exception {
        MapperFactoryBean<MyBatisApiMapper> factoryBean = new MapperFactoryBean<>(MyBatisApiMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }

    /**
     * 创建MyBatisApiService实例的Bean
     *
     * @param idService      ID服务实例，用于生成唯一标识符
     * @param mappingService 映射服务实例，用于数据映射转换
     * @param resultService  结果服务实例，用于处理返回结果
     * @return 配置好的MyBatisApiService实例
     */
    @Bean
    public MyBatisApiService myBatisApiService(MyBatisApiProperties properties, IDService<?> idService, IMappingService mappingService, IResultService<?> resultService, MyBatisApiMapper mapper) {
        return new MyBatisApiService(properties.getId(), idService, mappingService, resultService, mapper);
    }


    /**
     * 注册一个Bean，用于路由MyBatisApiService的请求
     *
     * @param myBatisApiService MyBatisApiService对象
     * @return 路由函数
     */
    @Bean
    public RouterFunction<ServerResponse> myBatisApiRouter(MyBatisApiProperties properties, MyBatisApiService myBatisApiService) {
        RouterFunctions.Builder builder = route();

        // 判断是否启用路由
        if (!Boolean.TRUE.equals(properties.getEnableRouter())) {
            return builder.build();
        }

        String basePath = properties.getBasePath();

        // 判断basePath是否以"/"开头，并且不以"/"结尾
        if (!basePath.startsWith("/") || basePath.endsWith("/")) {
            throw new IllegalArgumentException("basePath must start with '/' and not end with '/'");
        }

        final String routePath = basePath + "/{method}/{tableName}";

        // 处理POST请求
        builder.POST(routePath, accept(MediaType.APPLICATION_JSON), request -> {
            String method = request.pathVariable("method");
            String tableName = request.pathVariable("tableName");

            try {
                // 调用MyBatisApiService的parse方法
                return ServerResponse.ok().body(myBatisApiService.parse(method, tableName, request.body(String.class)));
            } catch (Exception e) {
                // 异常处理：统一返回400错误
                return ServerResponse.badRequest().body("Internal Server Error: " + e.getMessage());
            }
        });

        // 构建并返回路由函数
        return builder.build();
    }
}
