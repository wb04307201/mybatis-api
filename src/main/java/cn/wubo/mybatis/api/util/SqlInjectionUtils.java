package cn.wubo.mybatis.api.util;

import java.util.Objects;
import java.util.regex.Pattern;

public class SqlInjectionUtils {

    private static final Pattern SQL_SYNTAX_PATTERN = Pattern.compile("(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)\\s+.*(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|(select\\s*\\*\\s*from\\s+)|(and|or)\\s+.*(like|=|>|<|in|between|is|not|exists)", 2);
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("'.*(or|union|--|#|/\\*|;)", 2);

    private SqlInjectionUtils() {
    }

    public static boolean check(String value) {
        Objects.requireNonNull(value);
        return SQL_COMMENT_PATTERN.matcher(value).find() || SQL_SYNTAX_PATTERN.matcher(value).find();
    }
}
