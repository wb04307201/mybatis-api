package cn.wubo.mybatis.api.core;

public class MyBatisApiException extends RuntimeException {
    public MyBatisApiException(String message) {
        super(message);
    }

    public MyBatisApiException(Throwable cause) {
        super(cause);
    }

    public MyBatisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
