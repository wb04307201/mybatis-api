package cn.wubo.mybatis.api.core;

import cn.wubo.mybatis.api.config.MyBatisApiProperties;
import cn.wubo.mybatis.api.core.id.IDService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.wubo.mybatis.api.core.Constant.VALUE;

public class MyBatisApiService {

    private final MyBatisApiProperties myBatisApiProperties;

    private final IDService<?> idService;

    public MyBatisApiService(MyBatisApiProperties myBatisApiProperties, IDService<?> idService) {
        this.myBatisApiProperties = myBatisApiProperties;
        this.idService = idService;
    }

    @Resource
    MyBatisApiMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public Object parse(String method, String tableName, String context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Object res = "";
            switch (method) {
                case "select":
                    res = mapper.select(tableName, objectMapper.readValue(context, Map.class));
                    break;
                case "insert":
                    res = insertParse(objectMapper, tableName, context);
                    break;
                case "update":
                    res = updateParse(objectMapper, tableName, context);
                    break;
                case "insertOrUpdate":
                    res = insertOrUpdateParse(objectMapper, tableName, context);
                    break;
                case "delete":
                    res = deleteParse(objectMapper, tableName, context);
                    break;
                default:
                    throw new MyBatisApiException("param method value not valid");
            }
            return res;
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e.getMessage(), e);
        }
    }

    private Object deleteParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().mapToInt(row -> mapper.delete(tableName, row)).sum();
            } else {
                return mapper.delete(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    private Object insertParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().map(row -> doInsert(tableName, row)).collect(Collectors.toList());
            } else {
                return doInsert(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    private Map<String, Object> doInsert(String tableName, Map<String, Object> params) {
        Object id;
        if (!params.containsKey(myBatisApiProperties.getId()) || "".equals(params.get(myBatisApiProperties.getId())))
            id = idService.generalID();
        else id = String.valueOf(params.get(myBatisApiProperties.getId()));
        params.put(myBatisApiProperties.getId(), id);
        mapper.insert(tableName, params);
        Map<String, Object> query = new HashMap<>();
        query.put("key", myBatisApiProperties.getId());
        query.put("value", id);
        return mapper.select(tableName, Collections.singletonMap(Constant.WHERE, Collections.singletonList(query))).get(0);
    }

    private Object updateParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().map(row -> doUpdate(tableName, row)).collect(Collectors.toList());
            } else {
                return doUpdate(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    private List<Map<String, Object>> doUpdate(String tableName, Map<String, Object> params) {
        mapper.update(tableName, params);
        Map<String, Object> query = new HashMap<>();
        query.put(Constant.WHERE, params.get(Constant.WHERE));
        return mapper.select(tableName, query);
    }

    public Object insertOrUpdateParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().map(row -> insertOrUpdate(tableName, row)).collect(Collectors.toList());
            } else {
                return insertOrUpdate(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    public Map<String, Object> insertOrUpdate(String tableName, Map<String, Object> params) {
        Object id;
        if (!params.containsKey(myBatisApiProperties.getId()) || "".equals(params.get(myBatisApiProperties.getId()))) {
            id = idService.generalID();
            params.put(myBatisApiProperties.getId(), id);
            mapper.insert(tableName, params);
        } else {
            id = String.valueOf(params.get(myBatisApiProperties.getId()));
            params.remove(myBatisApiProperties.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("key", myBatisApiProperties.getId());
            map.put(VALUE, id);
            params.put(Constant.WHERE, Collections.singletonList(map));
            mapper.update(tableName, params);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("key", myBatisApiProperties.getId());
        query.put(VALUE, id);
        return mapper.select(tableName, Collections.singletonMap(Constant.WHERE, Collections.singletonList(query))).get(0);
    }
}
