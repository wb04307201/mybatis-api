package cn.wubo.mybatis.api.builder;

import java.util.Map;

public interface IBuilder {

    String select(String tableName, Map<String, Object> params);
    String insert(String tableName, Map<String, Object> params);
    String update(String tableName, Map<String, Object> params);
    String delete(String tableName, Map<String, Object> params);


}
