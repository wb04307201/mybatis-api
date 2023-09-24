# mybatis-api-spring-boot-starter

> 基于mybatis提供万能通用接口，大部分接口不用再写，零码便可进行增删改查

[![](https://jitpack.io/v/com.gitee.wb04307201/mybatis-api-spring-boot-starter.svg)](https://jitpack.io/#com.gitee.wb04307201/mybatis-api-spring-boot-starter)

* ### [1.如何使用](#1)
* ### [2.语法 & 示例](#2)
* [2.1 新增](#2.1)
* [2.2 修改](#2.2)
* [2.3 查询](#2.3)
* [2.4 删除](#2.4)

## <h2 id="1">1.如何使用<h2/>
### 第一步 增加 JitPack 仓库
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

### 第二步 引入jar
```xml
    <dependency>
        <groupId>com.gitee.wb04307201</groupId>
        <artifactId>mybatis-api-spring-boot-starter</artifactId>
        <version>1.0.2</version>
    </dependency>
```

### 第三步 在启动类上加上`@EnableMyBatisApi`注解
```java
@EnableMyBatisApi
@SpringBootApplication
public class MybatisApiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisApiDemoApplication.class, args);
    }

}
```

## <h2 id="2">2.语法 & 示例<h2/>
[示例代码](https://gitee.com/wb04307201/mybatis-api-demo)
### <h3 id="2.1">2.1 新增<h3/>
> 新增时如果不传id值会按照uuid的方式填补  
> 请求地址 http://ip:port/api/insert/person
#### 请求体 单条数据
```json
{
  "code": "11111",
  "name": "11111"
}
```
#### 响应:
```json
{
  "CODE": "22222",
  "ID": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
  "NAME": "22222"
}
```
#### 请求体 批量数据
```json
[
  {
    "code": "11111",
    "name": "11111"
  },
  {
    "code": "22222",
    "name": "22222"
  }
]
```
#### 响应:
```json
[
  {
    "CODE": "11111",
    "ID": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "NAME": "11111"
  },
  {
    "CODE": "22222",
    "ID": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
    "NAME": "22222"
  }
]
```
### <h3 id="2.2">2.2 修改<h3/>
> 根据查询条件修改数据  
> 请求地址 http://ip:port/api/update/person
#### 请求体 单条数据
```json
{
  "code": "33333",
  "name": "33333",
  "@where": [
    {
      "key": "id",
      "condition": "eq",
      "value": "417f6982-0e16-45cc-b1d4-51aa070c74d8"
    }
  ]
}
```
#### 响应:
```json
[
  {
    "CODE": "33333",
    "ID": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "NAME": "33333"
  }
]
```
#### 请求体 批量数据
```json
[
  {
    "code": "33333",
    "name": "33333",
    "@where": [
      {
        "key": "id",
        "condition": "eq",
        "value": "417f6982-0e16-45cc-b1d4-51aa070c74d8"
      }
    ]
  }
]
```
#### 响应:
```json
[
  [
    {
      "CODE": "33333",
      "ID": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
      "NAME": "33333"
    }
  ]
]
```
### <h3 id="2.3">2.3 保存和修改<h3/>
> 请求体包含id则根据id修改数据，不包含id则生成id新增数据    
> 请求地址 http://ip:port/api/inertOrUpdate/person
#### 请求体 单条数据
```json
{
  "code": "11111",
  "name": "11111"
}
```
#### 响应:
```json
{
  "CODE": "22222",
  "ID": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
  "NAME": "22222"
}
```
#### 请求体 批量数据
```json
[
  {
    "code": "11111",
    "name": "11111"
  },
  {
    "name": "33333",
    "id": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8"
  }
]
```
#### 响应:
```json
[
  {
    "CODE": "11111",
    "ID": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "NAME": "11111"
  },
  {
    "CODE": "22222",
    "ID": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
    "NAME": "33333"
  }
]
```
### <h3 id="2.3">2.3 查询<h3/>
> 请求地址 http://ip:port/api/select/person
#### 请求体 单表查询:
```json
{
  "@column": "id,name",
  "@where": [
    {
      "key": "id",
      "condition": "notnull"
    }
  ],
  "@page": {
    "pageIndex": 0,
    "pageSize": 10
  }
}
```
#### 响应:
```json
[
  {
    "ID": "001",
    "NAME": "name1"
  },
  {
    "ID": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "NAME": "33333"
  },
  {
    "ID": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
    "NAME": "22222"
  }
]
```
#### 请求体 多表关联查询:
```json
{
  "@column": "person.id,person.name,person.deptcode,dept.name",
  "@where": [
    {
      "key": "person.id",
      "condition": "notnull"
    }
  ],
  "@page": {
    "pageIndex": 0,
    "pageSize": 10
  },
  "@join": {
    "join": "dept on person.deptcode = dept.code"
  }
}
```
#### 响应:
```json
[
  {
    "DEPTCODE": "deptcode1",
    "ID": "001",
    "NAME": "name1"
  }
]
```
#### 请求体 分组查询:
```json
{
  "@column": "deptcode,count(1) as personcount",
  "@where": [
    {
      "key": "id",
      "condition": "notnull"
    }
  ],
  "@group": [
    "deptcode"
  ]
}
```
#### 响应:
```json
[
  {
    "DEPTCODE": "deptcode1",
    "PERSONCOUNT": "2"
  }
]
```
### <h3 id="2.4">2.4 删除<h3/>
> 请求地址 http://ip:port/api/delete/person
#### 请求体:
```json
{
  "@where": [
    {
      "key": "id",
      "condition": "in",
      "value": [
        "417f6982-0e16-45cc-b1d4-51aa070c74d8",
        "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8"
      ]
    }
  ]
}
```
#### 响应:
```json
2
```