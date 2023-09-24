package cn.wubo.mybatis.api.core;

import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Builder {

    public String select(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().FROM(tableName);
        findAny(params, Constant.JOIN).ifPresent(join -> parseJoin(sql, join.getValue()));
        Optional<Map.Entry<String, Object>> select = findAny(params, Constant.COLUMN);
        if (select.isPresent()) sql.SELECT(String.valueOf(select.get().getValue()).split(","));
        else sql.SELECT("*");
        findAny(params, Constant.WHERE).ifPresent(where -> parseWhere(sql, where.getValue()));
        findAny(params, Constant.PAGE).ifPresent(page -> parsePage(sql, page.getValue()));
        findAny(params, Constant.GROUP).ifPresent(group -> parseGroup(sql, group.getValue()));
        return sql.toString();
    }

    public String insert(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().INSERT_INTO(tableName);
        params.entrySet().stream().filter(entry -> !entry.getKey().startsWith(Constant.AT)).forEach(entry -> sql.VALUES(entry.getKey(), getValueStr(entry.getValue())));
        return sql.toString();
    }

    public String update(String tableName, Map<String, Object> params) {
        SQL sql = new SQL().UPDATE(tableName);
        params.entrySet().stream().filter(entry -> !entry.getKey().startsWith(Constant.AT)).forEach(entry -> sql.SET(entry.getKey() + " = " + getUpdateValueStr(entry.getValue())));
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
            String conditionStr = condition.containsKey("condition") && !ObjectUtils.isEmpty(condition.get("condition")) ? String.valueOf(condition.get("condition")) : "eq";

            if (ObjectUtils.isEmpty(keyStr)) {
                throw new MyBatisApiException("recieving incomplete @where key[" + keyStr + "]");
            }

            if (conditionStr.equalsIgnoreCase(MatchParams.EQ.name()) || conditionStr.equalsIgnoreCase(MatchParams.UEQ.name())) {
                sql.WHERE(keyStr + MatchParams.search(conditionStr.toUpperCase()).value() + getValueStr(valueObj));
            } else if (conditionStr.equalsIgnoreCase(MatchParams.LIKE.name()) || conditionStr.equalsIgnoreCase(MatchParams.ULIKE.name())) {
                sql.WHERE(concatUpperStr(keyStr) + MatchParams.search(conditionStr.toUpperCase()).value() + "'%" + String.valueOf(valueObj).toUpperCase() + "%'");
            } else if (conditionStr.equalsIgnoreCase(MatchParams.LLIKE.name())) {
                sql.WHERE(concatUpperStr(keyStr) + MatchParams.LLIKE.value() + "'%" + String.valueOf(valueObj).toUpperCase() + "'");
            } else if (conditionStr.equalsIgnoreCase(MatchParams.RLIKE.name())) {
                sql.WHERE(concatUpperStr(keyStr) + MatchParams.RLIKE.value() + "'" + String.valueOf(valueObj).toUpperCase() + "%'");
            } else if (conditionStr.equalsIgnoreCase(MatchParams.GT.name()) || conditionStr.equalsIgnoreCase(MatchParams.LT.name()) || conditionStr.equalsIgnoreCase(MatchParams.GTEQ.name()) || conditionStr.equalsIgnoreCase(MatchParams.LTEQ.name())) {
                sql.WHERE(keyStr + MatchParams.search(conditionStr.toUpperCase()).value() + getValueStr(valueObj));
            } else if (conditionStr.equalsIgnoreCase(MatchParams.BETWEEN.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTBETWEEN.name())) {
                if (valueObj instanceof List && ((List<Object>) valueObj).size() == 2) {
                    List<Object> valueObjs = (List<Object>) valueObj;
                    sql.WHERE(keyStr + MatchParams.search(conditionStr.toUpperCase()).value() + "(" + getValueStr(valueObjs.get(0)) + "," + getValueStr(valueObjs.get(1)) + ")");
                } else {
                    throw new MyBatisApiException("recieving incomplete @where condition between values invalid");
                }
            } else if (conditionStr.equalsIgnoreCase(MatchParams.IN.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTIN.name())) {
                if (valueObj instanceof List) {
                    List<Object> valueObjs = (List<Object>) valueObj;
                    if (valueObjs.isEmpty()) {
                        sql.WHERE("1 = 2");
                    } else {
                        sql.WHERE(keyStr + MatchParams.search(conditionStr.toUpperCase()).value() + "(" + valueObjs.stream().map(this::getValueStr).collect(Collectors.joining(",")) + ")");
                    }
                } else {
                    throw new MyBatisApiException("recieving incomplete @where condition in values invalid");
                }
            } else if (conditionStr.equalsIgnoreCase(MatchParams.NULL.name()) || conditionStr.equalsIgnoreCase(MatchParams.NOTNULL.name())) {
                sql.WHERE(keyStr + MatchParams.search(conditionStr.toUpperCase()).value());
            }
        });
    }

    private void parsePage(SQL sql, Object page) {
        Map<String, Object> p = (Map<String, Object>) page;
        if (p.containsKey(Constant.PAGE_SIZE) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_SIZE))) {
            Integer pageSize = (Integer) p.get(Constant.PAGE_SIZE);
            Integer pageIndex = p.containsKey(Constant.PAGE_INDEX) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_INDEX)) ? (Integer) p.get(Constant.PAGE_INDEX) : 0;
            sql.OFFSET((long) pageSize * pageIndex).LIMIT(pageSize);
        }
    }

    private void parseGroup(SQL sql, Object group) {
        List<String> p = (List<String>) group;
        sql.GROUP_BY(p.toArray(new String[0]));
    }

    private Optional<Map.Entry<String, Object>> findAny(Map<String, Object> params, String key) {
        return params.entrySet().stream().filter(entry -> entry.getKey().equals(key)).findAny();
    }

    private String getUpdateValueStr(Object valueObj) {
        String str = getValueStr(valueObj);
        if ("'=null'".equals(valueObj)) return "null";
        else return str;
    }

    private String getValueStr(Object valueObj) {
        return valueObj instanceof String ? Constant.QUOTATION + valueObj + Constant.QUOTATION : String.valueOf(valueObj);
    }

    private String concatUpperStr(String keyStr) {
        return "upper(" + keyStr + ")";
    }
}
