package cn.wubo.mybatis.api.builder;

import cn.wubo.mybatis.api.Constant;
import cn.wubo.mybatis.api.enums.JoinsEnum;
import cn.wubo.mybatis.api.util.BuilderUtils;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static cn.wubo.mybatis.api.Constant.*;

public abstract class AbstractBuilder implements IBuilder {

    /**
     * 解析并处理SELECT语句中的列信息
     *
     * @param sql    SQL对象，用于构建SELECT语句
     * @param params 参数映射，包含查询相关的各种参数配置
     */
    protected void parseSelectColumns(SQL sql, Map<String, Object> params) {
        // @formatter:off
        // 解析需要查询的列名，如果未指定则默认查询所有列
        String[] columns = findAny.apply(params, COLUMN)
                .map(entry ->
                        entry.getValue() != null ? entry.getValue().toString() : "*"
                )
                .orElse("*").split(",");

        // 判断是否需要去重查询，如果需要则使用SELECT_DISTINCT，否则使用普通SELECT
        if (findAny.apply(params, DISTINCT)
                .map(dis -> dis.getValue() != null && Boolean.TRUE.equals(dis.getValue()))
                .orElse(Boolean.FALSE)
        ) sql.SELECT_DISTINCT(columns);
        else sql.SELECT(columns);
        // @formatter:on
    }

    /**
     * 解析并处理SQL连接语句
     *
     * @param sql    SQL对象，用于构建SQL语句
     * @param params 参数映射，包含SQL构建所需的各种参数
     */
    protected void parseJoin(SQL sql, Map<String, Object> params) {
        // 查找并处理JOIN相关的参数配置
        findAny.apply(params, JOIN).ifPresent(join -> {
            // 检查join参数值是否为Map类型，如果是则进行连接语句解析
            if (join.getValue() instanceof Map) {
                Map<String, String> j = (Map<String, String>) join.getValue();
                // 遍历连接配置，根据不同的连接类型调用对应的SQL方法
                j.forEach((k, v) -> {
                    if (k.equalsIgnoreCase(JoinsEnum.JOIN.name())) sql.JOIN(v);
                    else if (k.equalsIgnoreCase(JoinsEnum.INNER_JOIN.name())) sql.INNER_JOIN(v);
                    else if (k.equalsIgnoreCase(JoinsEnum.LEFT_OUTER_JOIN.name())) sql.LEFT_OUTER_JOIN(v);
                    else if (k.equalsIgnoreCase(JoinsEnum.RIGHT_OUTER_JOIN.name())) sql.RIGHT_OUTER_JOIN(v);
                    else if (k.equalsIgnoreCase(JoinsEnum.OUTER_JOIN.name())) sql.OUTER_JOIN(v);
                });
            }
        });
    }

    /**
     * 解析并处理SQL查询中的WHERE条件
     *
     * @param sql    SQL对象，用于构建和存储SQL查询语句
     * @param params 参数映射，包含查询条件和其他相关参数
     */
    protected void parseWhere(SQL sql, Map<String, Object> params) {
        findAny.apply(params, WHERE).ifPresent(where -> {
            // 检查 WHERE 条件是否为 List 类型，如果不是则抛出异常
            if (!(where.getValue() instanceof List)) {
                throw new IllegalArgumentException("Invalid @where condition type");
            }
            // 将 WHERE 条件转换为 List<Map<String, Object>> 类型并遍历处理每个条件
            List<Map<String, Object>> wherelist = (List<Map<String, Object>>) where.getValue();
            for (Map<String, Object> condition : wherelist) {
                sql.WHERE(condition.containsKey(CONDITION) ? Condition.create((String) condition.get(KEY), (String) condition.get(CONDITION), condition.get(VALUE)).toSql() : Condition.create((String) condition.get(KEY), condition.get(VALUE)).toSql());
            }
        });
    }


    /**
     * 解析并处理SQL中的GROUP BY子句
     *
     * @param sql    SQL对象，用于构建GROUP BY子句
     * @param params 参数映射，包含GROUP相关的参数配置
     */
    protected void parseGroup(SQL sql, Map<String, Object> params) {
        findAny.apply(params, GROUP).ifPresent(group -> {
            // 检查group参数是否为List类型
            if (group.getValue() instanceof List) {
                List<?> groupList = (List<?>) group.getValue();
                if (!groupList.isEmpty()) {
                    // 过滤并转换group列表中的有效元素为字符串数组
                    // @formatter:off
                    String[] groupArray = groupList.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    // @formatter:on
                    // 如果存在有效的group字段，则添加到SQL的GROUP BY子句中
                    if (groupArray.length > 0) {
                        sql.GROUP_BY(groupArray);
                    }
                }
            } else {
                throw new IllegalArgumentException("Group parameter is not a List: " + group);
            }
        });
    }


    /**
     * 解析并处理SQL排序参数
     *
     * @param sql    SQL对象，用于构建排序语句
     * @param params 参数映射，包含排序相关的参数信息
     */
    protected void parseOrder(SQL sql, Map<String, Object> params) {
        findAny.apply(params, Constant.ORDER).ifPresent(order -> {
            if (order.getValue() instanceof List) {
                List<?> orderList = (List<?>) order.getValue();
                if (!orderList.isEmpty()) {
                    // 过滤并转换排序参数列表，去除空值和空字符串，转换为字符串数组
                    // @formatter:off
                    String[] orderArray = orderList.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    // @formatter:on
                    if (orderArray.length > 0) {
                        sql.ORDER_BY(orderArray);
                    }
                }
            } else {
                throw new IllegalArgumentException("Order parameter is not a List: " + order);
            }
        });
    }


    /**
     * 解析分页参数并应用到SQL查询中
     *
     * @param sql    SQL对象，用于设置分页的OFFSET和LIMIT子句
     * @param params 参数映射，包含分页相关参数
     */
    protected void parsePage(SQL sql, Map<String, Object> params) {
        findAny.apply(params, PAGE).ifPresent(page -> {
            // 检查分页参数是否为Map类型
            if (page.getValue() instanceof Map) {
                Map<String, Object> p = (Map<String, Object>) page.getValue();
                int pageSize = 10;
                int pageIndex = 0;

                // 解析分页大小参数
                if (p.containsKey(PAGE_SIZE) && p.get(PAGE_SIZE) instanceof Integer) {
                    pageSize = (int) p.get(PAGE_SIZE);
                }
                // 解析页码参数
                if (p.containsKey(PAGE_INDEX) && p.get(PAGE_INDEX) instanceof Integer) {
                    pageIndex = (int) p.get(PAGE_INDEX);
                }

                // 边界检查
                if (pageSize > 0 && pageIndex >= 0) {
                    long offset = (long) pageSize * pageIndex;
                    // 检查是否溢出
                    if (offset >= 0) {
                        sql.OFFSET(offset).LIMIT(pageSize);
                    }
                }
            }
        });
    }


        /**
     * 解析并处理SQL参数值
     *
     * @param sql    SQL对象，用于构建VALUES子句
     * @param params 参数映射，包含键值对形式的参数数据
     */
    protected void parseValues(SQL sql, Map<String, Object> params) {
        // 遍历参数映射，过滤掉以AT符号开头且值不为null的参数，构建SQL的VALUES子句
        for(Map.Entry<String, Object> entry:params.entrySet()){
            if(!entry.getKey().startsWith(AT) && entry.getValue() != null){
                Set set = Set.create(entry.getKey(), entry.getValue());
                sql.VALUES(BuilderUtils.removeGroovyFlag(set.getKey()), BuilderUtils.getValueStr(set.getValue()));
            }
        }
    }


        /**
     * 解析并设置SQL更新语句中的SET子句
     *
     * @param sql    SQL对象，用于构建SQL语句
     * @param params 参数映射，包含需要设置的字段名和对应的值
     */
    protected void parseSet(SQL sql, Map<String, Object> params) {
        // 遍历参数映射，为每个非特殊标记的字段生成SET子句
        for(Map.Entry<String, Object> entry:params.entrySet()){
            if(!entry.getKey().startsWith(AT)){
                Set set = Set.create(entry.getKey(), entry.getValue());
                sql.SET(BuilderUtils.removeGroovyFlag(set.getKey()) + " = " + BuilderUtils.getValueStr(set.getValue()));
            }
        }
    }



    private final BiFunction<Map<String, Object>, String, Optional<Map.Entry<String, Object>>> findAny = (params, key) -> params.entrySet().stream().filter(entry -> entry.getKey().equals(key)).findAny();


}
