package cn.wubo.mybatis.api;

public enum MatchParams {

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

    MatchParams(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MatchParams search(String name) {
        return MatchParams.valueOf(name.toUpperCase());
    }
}
