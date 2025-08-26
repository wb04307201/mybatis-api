package cn.wubo.mybatis.api.util;

import java.util.Objects;
import java.util.regex.Pattern;

public class SqlInjectionUtils {

    private static final Pattern SQL_SYNTAX_PATTERN = Pattern.compile("(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)\\s+.*(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|(select\\s*\\*\\s*from\\s+)|(and|or)\\s+.*(like|=|>|<|in|between|is|not|exists)", 2);
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("'.*(or|union|--|#|/\\*|;)", 2);

    private SqlInjectionUtils() {
    }

        /**
     * 检查输入值是否为空，并对字符串类型的值进行SQL注入攻击检测
     * @param value 待检查的值，不能为null
     * @throws IllegalArgumentException 当检测到SQL注入攻击时抛出
     */
    public static void check(Object value) {
        // 检查参数是否为null
        Objects.requireNonNull(value);

        // 对字符串类型的值进行SQL注入攻击检测
        if (value instanceof String str) {
            // 使用正则表达式匹配SQL语法关键字，如果匹配成功则抛出异常
            if (SQL_SYNTAX_PATTERN.matcher(str).find() || SQL_COMMENT_PATTERN.matcher(str).find()){
                throw new IllegalArgumentException("SQL injection attack detected");
            }
        }
    }

}
