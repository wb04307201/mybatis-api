package cn.wubo.mybatis.api.core;

import cn.wubo.mybatis.api.config.MyBatisApiProperties;
import cn.wubo.mybatis.api.core.id.IDService;
import cn.wubo.mybatis.api.core.mapping.IMappingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.wubo.mybatis.api.core.Constant.VALUE;

public class MyBatisApiService {

    private final MyBatisApiProperties myBatisApiProperties;
    private final IDService<?> idService;
    private final IMappingService mappingService;

    public MyBatisApiService(MyBatisApiProperties myBatisApiProperties, IDService<?> idService, IMappingService mappingService) {
        this.myBatisApiProperties = myBatisApiProperties;
        this.idService = idService;
        this.mappingService = mappingService;
    }

    @Resource
    MyBatisApiMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public Object parse(String method, String tableName, String context) {
        ObjectMapper objectMapper = new ObjectMapper();
        switch (method) {
            case "select":
                return selectParse(objectMapper, tableName, context);
            case "insert":
                return insertParse(objectMapper, tableName, context);
            case "update":
                return updateParse(objectMapper, tableName, context);
            case "insertOrUpdate":
                return insertOrUpdateParse(objectMapper, tableName, context);
            case "delete":
                return deleteParse(objectMapper, tableName, context);
            default:
                throw new MyBatisApiException(String.format("method [%s] value not valid", method));
        }
    }

    private List<Map<String, Object>> mapParse(List<Map<String, Object>> list) {
        return list.stream().map(map -> {
            Map<String, Object> newMap = new HashMap<>();
            map.entrySet().stream().forEach(enrty -> newMap.put(mappingService.parseKey(enrty.getKey()), enrty.getValue()));
            return newMap;
        }).collect(Collectors.toList());
    }

    private Object selectParse(ObjectMapper objectMapper, String tableName, String context) {
        try {
            Map<String, Object> params = objectMapper.readValue(context, new TypeReference<Map<String, Object>>() {
            });

            Optional<Map.Entry<String, Object>> param = params.entrySet().stream().filter(entry -> entry.getKey().equals(Constant.PAGE)).findAny();
            if (param.isPresent()) {
                PageVO pageVO = new PageVO();

                Map<String, Object> p = (Map<String, Object>) param.get().getValue();
                pageVO.setPageIndex((long) (p.containsKey(Constant.PAGE_INDEX) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_INDEX)) ? (int) p.get(Constant.PAGE_INDEX) : 0));
                pageVO.setPageSize((long) (p.containsKey(Constant.PAGE_SIZE) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_SIZE)) ? (int) p.get(Constant.PAGE_SIZE) : 10));

                Map<String, Object> countParams = objectMapper.readValue(context, new TypeReference<Map<String, Object>>() {
                });
                countParams.put("@column", "count(1) as TOTAL");
                countParams.remove(Constant.PAGE);
                pageVO.setTotal((Long) mapper.select(tableName, countParams).get(0).get("TOTAL"));

                pageVO.setRecords(mapParse(mapper.select(tableName, params)));
                return pageVO;
            } else {
                return mapParse(mapper.select(tableName, params));
            }
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
            throw new MyBatisApiException(e.getMessage(), e);
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
            throw new MyBatisApiException(e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> doInsert(String tableName, Map<String, Object> params) {
        Object id;
        if (!params.containsKey(myBatisApiProperties.getId()) || "".equals(params.get(myBatisApiProperties.getId())))
            id = idService.generalID();
        else id = String.valueOf(params.get(myBatisApiProperties.getId()));
        params.put(myBatisApiProperties.getId(), id);
        return mapParse(mapper.select(tableName, params));
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
            throw new MyBatisApiException(e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> doUpdate(String tableName, Map<String, Object> params) {
        mapper.update(tableName, params);
        Map<String, Object> query = new HashMap<>();
        query.put(Constant.WHERE, params.get(Constant.WHERE));
        return mapParse(mapper.select(tableName, query));
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
            throw new MyBatisApiException(e.getMessage(), e);
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
        return mapParse(mapper.select(tableName, Collections.singletonMap(Constant.WHERE, Collections.singletonList(query)))).get(0);
    }
}
