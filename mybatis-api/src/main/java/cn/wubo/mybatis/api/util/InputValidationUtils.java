package cn.wubo.mybatis.api.util;

import java.util.regex.Pattern;

public class InputValidationUtils {

    // 表名和列名的合法字符模式（只允许字母、数字、下划线）
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private InputValidationUtils() {}

        /**
     * 验证表名的有效性
     *
     * @param tableName 待验证的表名，不能为null
     * @throws IllegalArgumentException 当表名无效时抛出此异常，包括以下情况：
     *                                  1. 表名为null或空字符串
     *                                  2. 表名不符合命名规则（必须以字母或下划线开头，只能包含字母、数字和下划线）
     *                                  3. 表名长度超过64个字符
     */
    public static void validateTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName + ". Table name must start with a letter or underscore and contain only letters, digits, and underscores.");
        }

        // 检查长度限制
        if (tableName.length() > 64) {
            throw new IllegalArgumentException("Table name is too long: " + tableName);
        }
    }

}
