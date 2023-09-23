package cn.wubo.mybatis.api.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

public class MyBatisApiService {

    @Resource
    MyBatisApiMapper mapper;

    @Transactional
    public String parse(String method, String tableName, String context) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String res = "";
            switch (method) {
                case "select":
                    res = objectMapper.writeValueAsString(mapper.select(tableName, objectMapper.readValue(context, Map.class)));
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

    public String deleteParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.writeValueAsString(objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().mapToInt(row -> mapper.delete(tableName, row)).sum());
            } else {
                return objectMapper.writeValueAsString(mapper.delete(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                })));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    public String saveParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            JsonNode rootNode = objectMapper.readValue(context, JsonNode.class);
            if (rootNode.isArray()) {
                return objectMapper.writeValueAsString(objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                }).stream().map(row -> save(tableName, row)).collect(Collectors.toList()));
            } else {
                return objectMapper.writeValueAsString(save(tableName, objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                })));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e);
        }
    }

    public Map<String, Object> save(String tableName, Map<String, Object> params) {
        String id;
        if (!params.containsKey(Constant.ID) || "".equals(params.get(Constant.ID))) {
            id = UUID.randomUUID().toString();
            params.put(Constant.ID, id);
            mapper.insert(tableName, params);
        } else {
            id = String.valueOf(params.get(Constant.ID));
            params.remove(Constant.ID);
            Map<String, String> map = new HashMap<>();
            map.put("key", Constant.ID);
            map.put("value", id);
            params.put(Constant.WHERE, Collections.singletonList(map));
            mapper.update(tableName, params);
        }
        Map<String, String> query = new HashMap<>();
        query.put("key", Constant.ID);
        query.put("value", id);
        return mapper.select(tableName, Collections.singletonMap(Constant.WHERE, Collections.singletonList(query))).get(0);
    }
}
