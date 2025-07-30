package cn.wubo.mybatis.api.util;

import java.util.Objects;
import java.util.regex.Pattern;

public class SqlInjectionUtils {

    private static final Pattern SQL_SYNTAX_PATTERN = Pattern.compile("(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)\\s+.*(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|(select\\s*\\*\\s*from\\s+)|(and|or)\\s+.*(like|=|>|<|in|between|is|not|exists)", 2);
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("'.*(or|union|--|#|/\\*|;)", 2);

    private SqlInjectionUtils() {
    }

    /**
 * 检查给定的字符串是否包含SQL注释或语法
 *
 * @param value 待检查的字符串
 * @return 如果字符串包含SQL注释或语法则返回true，否则返回false
 */
public static boolean check(String value) {
    Objects.requireNonNull(value);
    if (value.isEmpty()) {
        return false;
    }
    if (SQL_COMMENT_PATTERN.matcher(value).find()) {
        return true;
    }
    return SQL_SYNTAX_PATTERN.matcher(value).find();
}


}
