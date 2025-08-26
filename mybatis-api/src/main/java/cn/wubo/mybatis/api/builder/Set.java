package cn.wubo.mybatis.api.builder;

import cn.wubo.mybatis.api.util.SqlInjectionUtils;
import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Set {

    private String key;
    private Object value;

    public static Set create(String key, Object value) {
        Object valueObj = value;
        if (key.endsWith("(G)")) {
            GroovyShell gs = new GroovyShell();
            valueObj = gs.evaluate(String.valueOf(value));
        }

        SqlInjectionUtils.check(valueObj);
        return new Set(key.replace("(G)", ""), valueObj);
    }
}
