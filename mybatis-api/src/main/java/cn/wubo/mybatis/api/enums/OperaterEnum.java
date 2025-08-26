package cn.wubo.mybatis.api.enums;

public enum OperaterEnum {

    EQ(" = "),
    UEQ(" <> "),
    LIKE(" like "),
    ULIKE(" not like "),
    LLIKE(" like "),
    RLIKE(" like "),
    GT(" > "),
    LT(" < "),
    GTEQ(" >= "),
    LTEQ(" <= "),
    BETWEEN(" between "),
    NOTBETWEEN(" not between "),
    IN(" in "),
    NOTIN(" not in "),
    NULL(" is null "),
    NOTNULL(" is not null ");

    private String value;

    OperaterEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OperaterEnum search(String name) {
        return OperaterEnum.valueOf(name.toUpperCase());
    }
}
