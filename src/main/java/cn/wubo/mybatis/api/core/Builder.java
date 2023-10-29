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

    public String select(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().FROM(tableName);
        findAny(params, Constant.JOIN).ifPresent(join -> parseJoin(sql, join.getValue()));
        Optional<Map.Entry<String, Object>> select = findAny(params, Constant.COLUMN);
        AtomicReference<Boolean> distinct = new AtomicReference<>();
        findAny(params, Constant.DISTINCT).ifPresent(dis -> distinct.set(dis.getValue() != null && Boolean.TRUE.equals(dis.getValue())));
        if (select.isPresent() && Boolean.TRUE.equals(distinct.get()))
            sql.SELECT_DISTINCT(String.valueOf(select.get().getValue()).split(","));
        else if (select.isPresent()) sql.SELECT(String.valueOf(select.get().getValue()).split(","));
        else if (Boolean.TRUE.equals(distinct.get())) sql.SELECT_DISTINCT("*");
        else sql.SELECT("*");
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
        findAny(params, Constant.PAGE).ifPresent(page -> parsePage(sql, page.getValue()));
        findAny(params, Constant.GROUP).ifPresent(group -> parseGroup(sql, group.getValue()));
        findAny(params, Constant.ORDER).ifPresent(group -> parseOrder(sql, group.getValue()));
        return sql.toString();
    }

    public String insert(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().INSERT_INTO(tableName);
        params.entrySet().stream().filter(entry -> !entry.getKey().startsWith(Constant.AT)).forEach(entry -> sql.VALUES(getKeyStr(entry.getKey()), getValueStr(entry.getKey(), entry.getValue())));
        return sql.toString();
    }

    public String update(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().UPDATE(tableName);
        params.entrySet().stream().filter(entry -> !entry.getKey().startsWith(Constant.AT)).forEach(entry -> sql.SET(getKeyStr(entry.getKey()) + " = " + getValueStr(entry.getKey(), entry.getValue(), Boolean.TRUE, Boolean.FALSE)));
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
        return sql.toString();
    }

    public String delete(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().DELETE_FROM(tableName);
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
        findAny(params, Constant.PAGE).ifPresent(page -> parsePage(sql, page.getValue()));
        return sql.toString();
    }

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

    private void parseWhere(SQL sql, Object where) {
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) where;
        conditions.forEach(condition -> {
            String keyStr = String.valueOf(condition.get("key"));
            Object valueObj = condition.get("value");
            String conditionStr = condition.containsKey(CONDITION) && !ObjectUtils.isEmpty(condition.get(CONDITION)) ? String.valueOf(condition.get(CONDITION)) : "eq";

            if (ObjectUtils.isEmpty(keyStr)) {
                throw new MyBatisApiException("recieving incomplete @where key[" + keyStr + "]");
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
                    throw new MyBatisApiException("recieving incomplete @where condition between values invalid");
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
                    throw new MyBatisApiException("recieving incomplete @where condition in values invalid");
                }
            } else if (conditionStr.equalsIgnoreCase(MatchParams.NULL.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTNULL.name())) {
                sql.WHERE(getKeyStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value());
            }
        });
    }

    private void parsePage(SQL sql, Object page) {
        Map<String, Object> p = (Map<String, Object>) page;
        int pageSize = p.containsKey(Constant.PAGE_SIZE) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_SIZE)) ? (int) p.get(Constant.PAGE_SIZE) : 10;
        int pageIndex = p.containsKey(Constant.PAGE_INDEX) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_INDEX)) ? (int) p.get(Constant.PAGE_INDEX) : 0;
        sql.OFFSET((long) pageSize * pageIndex).LIMIT(pageSize);
    }

    private void parseGroup(SQL sql, Object group) {
        List<String> p = (List<String>) group;
        sql.GROUP_BY(p.toArray(new String[0]));
    }

    private void parseOrder(SQL sql, Object order) {
        List<String> p = (List<String>) order;
        sql.ORDER_BY(p.toArray(new String[0]));
    }

    private Optional<Map.Entry<String, Object>> findAny(Map<String, Object> params, String key) {
        return params.entrySet().stream().filter(entry -> entry.getKey().equals(key)).findAny();
    }

    private String getValueStr(String key, Object valueObj) {
        return getValueStr(key, valueObj, Boolean.FALSE, Boolean.FALSE);
    }

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

    private String getKeyStr(String key) {
        return key.replace("(G)", "");
    }

    private String upperValueStr(String keyStr) {
        return "upper(" + keyStr + ")";
    }
}
