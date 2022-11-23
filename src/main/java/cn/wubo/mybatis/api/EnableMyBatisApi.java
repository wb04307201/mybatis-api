package cn.wubo.mybatis.api;

import cn.wubo.mybatis.api.config.MyBatisApiConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({MyBatisApiConfiguration.class})
public @interface EnableMyBatisApi {
}
