package cn.wubo.mybatis.api.builder;

import cn.wubo.mybatis.api.enums.OperaterEnum;
import cn.wubo.mybatis.api.util.BuilderUtils;
import cn.wubo.mybatis.api.util.SqlInjectionUtils;
import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

import static cn.wubo.mybatis.api.Constant.*;

@Data
@AllArgsConstructor
public class Condition {

    private String key;
    private OperaterEnum operater;
    private Object value;

    public static Condition create(String key, String operaterStr, Object value) {
        Object valueObj = value;
        if (key.endsWith("(G)")) {
            GroovyShell gs = new GroovyShell();
            valueObj = gs.evaluate(String.valueOf(value));
        }
        SqlInjectionUtils.check(valueObj);
        return new Condition(key.replace("(G)", ""), OperaterEnum.search(operaterStr), valueObj);
    }

    public static Condition create(String key, Object value) {
        return create(key, "eq", value);
    }

    public String toSql() {
        return switch (operater) {
            case EQ, UEQ:
                yield key + operater.value() + BuilderUtils.getValueStr(value);
            case LIKE, ULIKE:
                yield LEFT_UPPER + key + RIGHT_UPPER + operater.value() + LEFT_LIKE + String.valueOf(value).toUpperCase() + RIGHT_lIKE;
            case LLIKE:
                yield LEFT_UPPER + key + RIGHT_UPPER + operater.value() + LEFT_LIKE + String.valueOf(value).toUpperCase() + QUOTATION;
            case RLIKE:
                yield LEFT_UPPER + key + RIGHT_UPPER + operater.value() + QUOTATION + String.valueOf(value).toUpperCase() + RIGHT_lIKE;
            case GT, LT, GTEQ, LTEQ:
                yield key + operater.value() + BuilderUtils.getValueStr(value);
            case BETWEEN, NOTBETWEEN, IN, NOTIN:
                if (value instanceof List list) {
                    yield key + operater.value() + "(" + BuilderUtils.getValueStr(list) + ")";
                } else {
                    throw new IllegalArgumentException("Invalid condition,  value must be a List");
                }
            case NULL, NOTNULL:
                yield key + operater.value();
        };
    }
}
