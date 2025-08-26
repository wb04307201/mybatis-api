package cn.wubo.mybatis.api.util;

import java.util.List;
import java.util.stream.Collectors;

import static cn.wubo.mybatis.api.Constant.QUOTATION;

public class BuilderUtils {

    private BuilderUtils() {
    }

    public static String removeGroovyFlag(String str) {
        return str.replace("(G)", "");
    }

    public static String getValueStr(Object value) {
        if (value instanceof String) {
            return QUOTATION + value + QUOTATION;
        } else if (value instanceof List list) {
            return list.stream().map(BuilderUtils::getValueStr).collect(Collectors.joining(",")).toString();
        } else {
            return String.valueOf(value);
        }
    }

}
