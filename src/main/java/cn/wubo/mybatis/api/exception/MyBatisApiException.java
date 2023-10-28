package cn.wubo.mybatis.api.exception;

public class MyBatisApiException extends RuntimeException {
    public MyBatisApiException(String message) {
        super(message);
    }

    public MyBatisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
