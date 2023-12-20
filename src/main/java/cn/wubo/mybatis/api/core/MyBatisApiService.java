package cn.wubo.mybatis.api.core;

import cn.wubo.mybatis.api.config.MyBatisApiProperties;
import cn.wubo.mybatis.api.core.id.IDService;
import cn.wubo.mybatis.api.core.mapping.IMappingService;
import cn.wubo.mybatis.api.core.result.IResultService;
import cn.wubo.mybatis.api.exception.MyBatisApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static cn.wubo.mybatis.api.core.Constant.VALUE;

public class MyBatisApiService {

    private final MyBatisApiProperties myBatisApiProperties;
    private final IDService<?> idService;
    private final IMappingService mappingService;
    private final IResultService<?> resultService;

    public MyBatisApiService(MyBatisApiProperties myBatisApiProperties, IDService<?> idService, IMappingService mappingService, IResultService<?> resultService) {
        this.myBatisApiProperties = myBatisApiProperties;
        this.idService = idService;
        this.mappingService = mappingService;
        this.resultService = resultService;
    }

    @Resource
    MyBatisApiMapper mapper;

    /**
     * 解析方法，根据传入的方法名称和参数，执行不同的数据库操作
     *
     * @param method    方法名称
     * @param tableName 表名
     * @param jsonStr   JSON字符串
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Object parse(String method, String tableName, String jsonStr) {
        Object r;
        switch (method) {
            case "select":
                r = selectParse(tableName, jsonStr);
                break;
            case "insert":
                r = insertParse(tableName, jsonStr);
                break;
            case "update":
                r = updateParse(tableName, jsonStr);
                break;
            case "insertOrUpdate":
                r = insertOrUpdateParse(tableName, jsonStr);
                break;
            case "delete":
                r = deleteParse(tableName, jsonStr);
                break;
            default:
                throw new MyBatisApiException(String.format("method [%s] value not valid", method));
        }
        return resultService.generalResult(r);
    }


    /**
     * 查询
     *
     * @param tableName 数据表名
     * @param jsonStr   包含查询参数的JSON字符串
     * @return 查询结果
     */
    public Object selectParse(String tableName, String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 解析JSON字符串
            Map<String, Object> params = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
            });

            // 获取分页参数
            Optional<Map.Entry<String, Object>> param = params.entrySet().stream().filter(entry -> entry.getKey().equals(Constant.PAGE)).findAny();
            if (param.isPresent()) {
                PageVO pageVO = new PageVO();

                Map<String, Object> p = (Map<String, Object>) param.get().getValue();
                // 设置分页参数
                pageVO.setPageIndex((long) (p.containsKey(Constant.PAGE_INDEX) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_INDEX)) ? (int) p.get(Constant.PAGE_INDEX) : 0));
                pageVO.setPageSize((long) (p.containsKey(Constant.PAGE_SIZE) && !ObjectUtils.isEmpty(p.get(Constant.PAGE_SIZE)) ? (int) p.get(Constant.PAGE_SIZE) : 10));

                // 获取总数参数
                Map<String, Object> countParams = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
                });
                countParams.put("@column", "count(1) as TOTAL");
                countParams.remove(Constant.PAGE);
                // 查询总数
                pageVO.setTotal((Long) mapper.select(tableName, countParams).get(0).get("TOTAL"));

                // 查询数据记录
                pageVO.setRecords(mapParse(mapper.select(tableName, params)));
                return pageVO;
            } else {
                // 查询数据记录
                return mapParse(mapper.select(tableName, params));
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e.getMessage(), e);
        }
    }


    /**
     * 删除数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Object deleteParse(String tableName, String jsonStr) {
        return doWhat(tableName, jsonStr, (tn, row) -> {
            if (row.containsKey(Constant.WITH_SELECT)) {
                mapper.delete(tn, row);
                return mapParse(mapper.select(tn, (Map<String, Object>) row.get(Constant.WITH_SELECT)));
            } else {
                return mapper.delete(tn, row);
            }
        });
    }


    /**
     * 插入
     */
    @Transactional(rollbackFor = Exception.class)
    public Object insertParse(String tableName, String jsonStr) {
        return doWhat(tableName, jsonStr, (tn, row) -> {
            if (row.containsKey(Constant.WITH_SELECT)) {
                mapper.insert(tn, row);
                return mapParse(mapper.select(tn, (Map<String, Object>) row.get(Constant.WITH_SELECT)));
            } else {
                return mapper.insert(tn, row);
            }
        });
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public Object updateParse(String tableName, String jsonStr) {
        return doWhat(tableName, jsonStr, (tn, row) -> {
            if (row.containsKey(Constant.WITH_SELECT)) {
                mapper.update(tn, row);
                return mapParse(mapper.select(tn, (Map<String, Object>) row.get(Constant.WITH_SELECT)));
            } else {
                return mapper.update(tn, row);
            }
        });
    }

    /**
     * 根据id插入或更新
     */
    @Transactional(rollbackFor = Exception.class)
    public Object insertOrUpdateParse(String tableName, String jsonStr) {
        return doWhat(tableName, jsonStr, (tn, row) -> {
            Object id;
            if (!row.containsKey(myBatisApiProperties.getId()) || "".equals(row.get(myBatisApiProperties.getId()))) {
                id = idService.generalID();
                row.put(myBatisApiProperties.getId(), id);
                mapper.insert(tableName, row);
            } else {
                id = String.valueOf(row.get(myBatisApiProperties.getId()));
                row.remove(myBatisApiProperties.getId());
                Map<String, Object> map = new HashMap<>();
                map.put("key", myBatisApiProperties.getId());
                map.put(VALUE, id);
                row.put(Constant.WHERE, Collections.singletonList(map));
                if (mapper.update(tableName, row) != 1)
                    throw new MyBatisApiException(String.format("%s value %s not unique!", myBatisApiProperties.getId(), id));
            }
            Map<String, Object> query = new HashMap<>();
            query.put("key", myBatisApiProperties.getId());
            query.put(VALUE, id);
            return mapParse(mapper.select(tableName, Collections.singletonMap(Constant.WHERE, Collections.singletonList(query)))).get(0);
        });
    }

    /**
     * 执行具体的操作
     *
     * @param tableName  表名
     * @param jsonStr    JSON字符串
     * @param biFunction 双函数接口
     * @return 执行结果
     */
    private Object doWhat(String tableName, String jsonStr, BiFunction<String, Map<String, Object>, Object> biFunction) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readValue(jsonStr, JsonNode.class);
            if (rootNode.isArray()) {
                // 如果根节点是一个数组
                List<Map<String, Object>> rows = objectMapper.convertValue(rootNode, new TypeReference<List<Map<String, Object>>>() {
                });
                List<Object> objects = new ArrayList<>();
                for (Map<String, Object> row : rows)
                    objects.add(biFunction.apply(tableName, row));
                return objects;
            } else {
                // 如果根节点是一个对象
                Map<String, Object> row = objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
                });
                return biFunction.apply(tableName, row);
            }
        } catch (JsonProcessingException e) {
            throw new MyBatisApiException(e.getMessage(), e);
        }
    }


    /**
     * 处理返回的结果集
     *
     * @param list 传入的结果集
     * @return 处理后的结果集
     */
    private List<Map<String, Object>> mapParse(List<Map<String, Object>> list) {
        return list.stream().map(map -> {
            Map<String, Object> newMap = new HashMap<>();
            map.entrySet().stream().forEach(enrty -> newMap.put(mappingService.parseKey(enrty.getKey()), enrty.getValue()));
            return newMap;
        }).collect(Collectors.toList());
    }
}
