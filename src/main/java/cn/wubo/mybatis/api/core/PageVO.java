package cn.wubo.mybatis.api.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class PageVO {
    private Long total;
    private Long pageSize;
    private Long pageIndex;
    private List<Map<String, Object>> records = new ArrayList<>();
}
