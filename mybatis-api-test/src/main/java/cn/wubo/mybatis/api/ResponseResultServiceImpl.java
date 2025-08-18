package cn.wubo.mybatis.api;

import cn.wubo.mybatis.api.service.result.IResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseResultServiceImpl implements IResultService<ResponseEntity<?>> {
    @Override
    public ResponseEntity<?> generalResult(Object o) {
        return ResponseEntity.ok(o);
    }
}
