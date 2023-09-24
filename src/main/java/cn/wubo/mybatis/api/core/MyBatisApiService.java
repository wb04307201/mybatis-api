package cn.wubo.mybatis.api.core;

import cn.wubo.mybatis.api.config.MyBatisApiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

public class MyBatisApiService {

    MyBatisApiProperties myBatisApiProperties;

    public MyBatisApiService(MyBatisApiProperties myBatisApiProperties) {
        this.myBatisApiProperties = myBatisApiProperties;
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
                case "save":
                    res = saveParse(objectMapper, tableName, context);
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

    public Object deleteParse(ObjectMapper objectMapper, String tableName, String context) {
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

    public Object saveParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().map(row -> saveOrUpdate(tableName, row)).collect(Collectors.toList());
            } else {
                return saveOrUpdate(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    public Map<String, Object> saveOrUpdate(String tableName, Map<String, Object> params) {
        String id;
        if (!params.containsKey(myBatisApiProperties.getId()) || "".equals(params.get(myBatisApiProperties.getId()))) {
            id = UUID.randomUUID().toString();
            params.put(myBatisApiProperties.getId(), id);
            mapper.insert(tableName, params);
        } else {
            id = String.valueOf(params.get(myBatisApiProperties.getId()));
            params.remove(myBatisApiProperties.getId());
            Map<String, String> map = new HashMap<>();
            map.put("key", myBatisApiProperties.getId());
            map.put("value", id);
            params.put(Constant.WHERE, Collections.singletonList(map));
            mapper.update(tableName, params);
        }
        Map<String, String> query = new HashMap<>();
        query.put("key", myBatisApiProperties.getId());
        query.put("value", id);
        return mapper.select(tableName, Collections.singletonMap(Constant.WHERE, Collections.singletonList(query))).get(0);
    }
}
