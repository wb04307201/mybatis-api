package cn.wubo.mybatis.api.builder;

import org.apache.ibatis.jdbc.SQL;

import java.util.Map;

public class Builder extends AbstractBuilder {

    /**
     * 选择查询
     *
     * @param tableName 表名
     * @param params    查询参数
     * @return SQL语句
     */
    public String select(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().FROM(tableName);
        // 解析查询列
        parseSelectColumns(sql, params);
        // 解析连接查询
        parseJoin(sql, params);
        // 解析WHERE条件
        parseWhere(sql,params);
        // 解析GROUP BY子句
        parseGroup(sql, params);
        // 解析ORDER BY子句
        parseOrder(sql, params);
        // 解析分页条件
        parsePage(sql, params);
        return sql.toString();
    }



    /**
     * 插入数据到指定表中
     *
     * @param tableName 表名
     * @param params    插入的数据参数，键为字段名，值为字段值
     * @return 返回插入的SQL语句
     */
    public String insert(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().INSERT_INTO(tableName);
        parseValues(sql, params);
        return sql.toString();
    }



    /**
     * 更新指定表中的数据
     *
     * @param tableName 表名
     * @param params    更新的字段和值
     * @return 更新后的SQL语句
     */
    public String update(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().UPDATE(tableName);
        parseSet(sql, params);
        parseWhere(sql, params);
        return sql.toString();
    }



    /**
     * 删除操作，根据给定的表名和参数构建并返回SQL字符串
     *
     * @param tableName 要删除数据的表名
     * @param params    删除参数映射，包含WHERE条件和分页信息
     * @return 构建的SQL字符串
     */
    public String delete(String tableName, Map<String, Object> params) {
        // 构造DELETE SQL语句
        SQL sql = new SQL().DELETE_FROM(tableName);
        // 解析WHERE条件参数
        parseWhere(sql, params);
        return sql.toString();
    }


}
