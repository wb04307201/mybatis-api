package cn.wubo.mybatis.api.core;

import cn.wubo.mybatis.api.exception.MyBatisApiException;
import cn.wubo.mybatis.api.util.SqlInjectionUtils;
import groovy.lang.GroovyShell;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.wubo.mybatis.api.core.Constant.CONDITION;

public class Builder {

    /**
     * 选择查询
     *
     * @param tableName 表名
     * @param params    查询参数
     * @return SQL语句
     */
    public String select(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().FROM(tableName);
        // 解析关联查询
        findAny(params, Constant.JOIN).ifPresent(join -> parseJoin(sql, join.getValue()));
        // 解析列查询
        Optional<Map.Entry<String, Object>> select = findAny(params, Constant.COLUMN);
        AtomicReference<Boolean> distinct = new AtomicReference<>();
        // 解析distinct
        findAny(params, Constant.DISTINCT).ifPresent(dis -> distinct.set(dis.getValue() != null && Boolean.TRUE.equals(dis.getValue())));
        // 根据列和distinct设置查询语句
        if (select.isPresent() && Boolean.TRUE.equals(distinct.get()))
            sql.SELECT_DISTINCT(String.valueOf(select.get().getValue()).split(","));
        else if (select.isPresent()) sql.SELECT(String.valueOf(select.get().getValue()).split(","));
        else if (Boolean.TRUE.equals(distinct.get())) sql.SELECT_DISTINCT("*");
        else sql.SELECT("*");
        // 解析where条件
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
        // 解析分页
        findAny(params, Constant.PAGE).ifPresent(page -> parsePage(sql, page.getValue()));
        // 解析group by
        findAny(params, Constant.GROUP).ifPresent(group -> parseGroup(sql, group.getValue()));
        // 解析order by
        findAny(params, Constant.ORDER).ifPresent(group -> parseOrder(sql, group.getValue()));
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
        // 过滤掉以Constant.AT开头的键，并添加VALUES子句
        // @formatter:off
        params.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(Constant.AT))
                .forEach(entry -> sql.VALUES(getKeyStr(entry.getKey()), getValueStr(entry.getKey(), entry.getValue())));
        // @formatter:on
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
        params.entrySet().stream().filter(entry -> !entry.getKey().startsWith(Constant.AT)).forEach(entry -> sql.SET(getKeyStr(entry.getKey()) + " = " + getValueStr(entry.getKey(), entry.getValue(), Boolean.TRUE, Boolean.FALSE)));
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
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
        SQL sql = new SQL().DELETE_FROM(tableName);
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
        findAny(params, Constant.PAGE).ifPresent(page -> parsePage(sql, page.getValue()));
        return sql.toString();
    }


    /**
     * 解析连接操作。
     *
     * @param sql  SQL对象
     * @param join 连接信息
     *             - join: 连接类型
     *             - v: 连接的表
     */
    private void parseJoin(SQL sql, Object join) {
        Map<String, String> j = (Map<String, String>) join;
        j.forEach((k, v) -> {
            if (k.equalsIgnoreCase(MatchJoins.JOIN.name())) sql.JOIN(v);
            else if (k.equalsIgnoreCase(MatchJoins.INNER_JOIN.name())) sql.INNER_JOIN(v);
            else if (k.equalsIgnoreCase(MatchJoins.LEFT_OUTER_JOIN.name())) sql.LEFT_OUTER_JOIN(v);
            else if (k.equalsIgnoreCase(MatchJoins.RIGHT_OUTER_JOIN.name())) sql.RIGHT_OUTER_JOIN(v);
            else if (k.equalsIgnoreCase(MatchJoins.OUTER_JOIN.name())) sql.OUTER_JOIN(v);
        });
    }


    /**
     * 解析where条件
     *
     * @param sql   SQL对象
     * @param where where条件集合
     */
    private void parseWhere(SQL sql, Object where) {
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) where;
        conditions.forEach(condition -> {
            String keyStr = String.valueOf(condition.get("key"));
            Object valueObj = condition.get("value");
            String conditionStr = condition.containsKey(CONDITION) && !ObjectUtils.isEmpty(condition.get(CONDITION)) ? String.valueOf(condition.get(CONDITION)) : "eq";

            if (ObjectUtils.isEmpty(keyStr)) {
                throw new MyBatisApiException("接收不到完整的@where键[" + keyStr + "]");
            }

            if (conditionStr.equalsIgnoreCase(MatchParams.EQ.name()) || conditionStr.equalsIgnoreCase(MatchParams.UEQ.name())) {
                sql.WHERE(getKeyStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value() + getValueStr(keyStr, valueObj));
            } else if (conditionStr.equalsIgnoreCase(MatchParams.LIKE.name()) || conditionStr.equalsIgnoreCase(MatchParams.ULIKE.name())) {
                sql.WHERE(upperValueStr(getKeyStr(keyStr)) + MatchParams.search(conditionStr.toUpperCase()).value() + "'%" + getValueStr(keyStr, valueObj, Boolean.FALSE, Boolean.TRUE) + "%'");
            } else if (conditionStr.equalsIgnoreCase(MatchParams.LLIKE.name())) {
                sql.WHERE(upperValueStr(getKeyStr(keyStr)) + MatchParams.LLIKE.value() + "'%" + getValueStr(keyStr, valueObj, Boolean.FALSE, Boolean.TRUE) + "'");
            } else if (conditionStr.equalsIgnoreCase(MatchParams.RLIKE.name())) {
                sql.WHERE(upperValueStr(getKeyStr(keyStr)) + MatchParams.RLIKE.value() + "'" + getValueStr(keyStr, valueObj, Boolean.FALSE, Boolean.TRUE) + "%'");
            } else if (conditionStr.equalsIgnoreCase(MatchParams.GT.name()) || conditionStr.equalsIgnoreCase(MatchParams.LT.name()) || conditionStr.equalsIgnoreCase(MatchParams.GTEQ.name()) || conditionStr.equalsIgnoreCase(MatchParams.LTEQ.name())) {
                sql.WHERE(getKeyStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value() + getValueStr(keyStr, valueObj));
            } else if (conditionStr.equalsIgnoreCase(MatchParams.BETWEEN.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTBETWEEN.name())) {
                if (valueObj instanceof List && ((List<Object>) valueObj).size() == 2) {
                    List<Object> valueObjs = (List<Object>) valueObj;
                    sql.WHERE(getKeyStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value() + "(" + getValueStr(keyStr, valueObjs.get(0)) + "," + getValueStr(keyStr, valueObjs.get(1)) + ")");
                } else {
                    throw new MyBatisApiException("接收不到完整的条件 between 值无效");
                }
            } else if (conditionStr.equalsIgnoreCase(MatchParams.IN.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTIN.name())) {
                if (valueObj instanceof List) {
                    List<Object> valueObjs = (List<Object>) valueObj;
                    if (valueObjs.isEmpty()) {
                        sql.WHERE("1 = 2");
                    } else {
                        sql.WHERE(getKeyStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value() + "(" + valueObjs.stream().map(vo -> getValueStr(keyStr, vo)).collect(Collectors.joining(",")) + ")");
                    }
                } else {
                    throw new MyBatisApiException("接收不到完整的条件 in 值无效");
                }
            } else if (conditionStr.equalsIgnoreCase(MatchParams.NULL.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTNULL.name())) {
                sql.WHERE(getKeyStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value());
            }
        });
    }

    /**
     * 解析分页参数，设置SQL的偏移量和限制结果数量
     *
     * @param sql  SQL对象
     * @param page 分页参数
     */
    private void parsePage(SQL sql, Object page) {
        // 将分页参数转换为Map对象
        Map<String, Object> p = (Map<String, Object>) page;

        // 获取分页大小，如果分页大小参数不存在或为空，则默认为10
        int pageSize = p.containsKey(Constant.PAGE_SIZE) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_SIZE)) ? (int) p.get(Constant.PAGE_SIZE) : 10;

        // 获取分页索引，如果分页索引参数不存在或为空，则默认为0
        int pageIndex = p.containsKey(Constant.PAGE_INDEX) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_INDEX)) ? (int) p.get(Constant.PAGE_INDEX) : 0;

        // 设置SQL的偏移量为分页索引乘以分页大小
        sql.OFFSET((long) pageSize * pageIndex).LIMIT(pageSize);
    }


    /**
     * 解析分组信息，设置SQL的分组字段
     *
     * @param sql   SQL对象
     * @param group 分组字段
     */
    private void parseGroup(SQL sql, Object group) {
        List<String> p = (List<String>) group;
        sql.GROUP_BY(p.toArray(new String[0]));
    }


    // 解析排序信息，设置SQL的排序字段
    private void parseOrder(SQL sql, Object order) {
        List<String> p = (List<String>) order;
        sql.ORDER_BY(p.toArray(new String[0]));
    }


    /**
     * 从指定的参数Map中找到第一个键与给定key相等的Entry，并返回一个Optional对象
     *
     * @param params 参数Map
     * @param key    要查找的键
     * @return 包含找到的Entry的Optional对象，如果未找到则返回空的Optional对象
     */
    private Optional<Map.Entry<String, Object>> findAny(Map<String, Object> params, String key) {
        return params.entrySet().stream().filter(entry -> entry.getKey().equals(key)).findAny();
    }


    // 根据给定的键和值对象获取对应的字符串值
    private String getValueStr(String key, Object valueObj) {
        // 调用getValueStr方法，传入键、值对象、Boolean.FALSE和Boolean.FALSE作为参数，获取字符串值
        return getValueStr(key, valueObj, Boolean.FALSE, Boolean.FALSE);
    }


    /**
     * 根据给定的key和valueObj获取对应的字符串值
     *
     * @param key      键
     * @param valueObj 值对象
     * @param isUpdate 是否是更新操作
     * @param isLike   是否忽略大小写
     * @return 对应的字符串值
     */
    private String getValueStr(String key, Object valueObj, Boolean isUpdate, Boolean isLike) {
        Object result = valueObj;
        if (key.endsWith("(G)")) {
            GroovyShell gs = new GroovyShell();
            result = gs.evaluate(String.valueOf(valueObj));
        }
        String valueStr;
        if (Boolean.TRUE.equals(isLike)) valueStr = String.valueOf(result).toUpperCase();
        else if (result instanceof String && SqlInjectionUtils.check((String) result))
            throw new MyBatisApiException("参数是否存在 SQL 注入!");
        else if (result instanceof String) valueStr = Constant.QUOTATION + result + Constant.QUOTATION;
        else valueStr = String.valueOf(result);

        if (Boolean.TRUE.equals(isUpdate) && "'=null'".equals(valueStr)) valueStr = "null";
        return valueStr;
    }


    // 根据给定的键获取对应的字符串键
    private String getKeyStr(String key) {
        // 使用正则表达式将 "(G)" 从键中移除
        return key.replace("(G)", "");
    }


    /**
     * 对给定的字符串进行大写处理并返回
     *
     * @param keyStr 待处理的字符串
     * @return 大写处理后的字符串
     */
    private String upperValueStr(String keyStr) {
        return "upper(" + keyStr + ")";
    }

}
