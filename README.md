# mybatis-api-spring-boot-starter mybatis通用接口启动器

基于mybatis,提供万能通用接口，零代码进行增删改查

[![](https://jitpack.io/v/com.gitee.wb04307201/mybatis-json-api-spring-boot-starter.svg)](https://jitpack.io/#com.gitee.wb04307201/mybatis-json-api-spring-boot-starter)

* ### [1.如何使用](#1)
* ### [2.示例](#2)
* [2.1 新增](#2.1)
* [2.2 修改](#2.2)
* [2.3 查询](#2.3)
* [2.4 删除](#2.4)

## <h2 id="1">1.如何使用<h2/>

## 第一步 增加 JitPack 仓库
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

## 第二步 引入jar
```xml
    <dependency>
        <groupId>com.gitee.wb04307201</groupId>
        <artifactId>mybatis-api-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
```

## 第三步 在启动类上加上`@EnableFilePreview`注解

```java
@EnableMyBatisApi
@SpringBootApplication
@MapperScan({"cn.wubo.demo"})
public class MybatisApiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisJsonApiDemoApplication.class, args);
    }

}
```

## 第三步 编写代码调用bean

```java
@RestController
@RequestMapping(value = "/api")
public class MybatisApiController {

    @Resource
    MyBatisApiService service;

    @PostMapping(value = "/{method}/{tableName}")
    public String parse(
            @PathVariable String method,
            @PathVariable String tableName,
            @RequestBody String context) {
        return service.parse(method, tableName, context);
    }
}

```

## <h2 id="2">2.示例<h2/>
> <a href="https://gitee.com/wb04307201/mybatis-api-demo">示例代码</a>

### <h3 id="2.1">2.1 新增<h3/>
请求:  
http://localhost:8080/api/save/person
<pre><code class="language-json">[
    {
        "code": "11111",
        "name": "11111"
    },
    {
        "code": "22222",
        "name": "22222"
    }
]
</code></pre>
返回:
<pre><code class="language-json">[
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
</code></pre>
### <h3 id="2.2">2.2 修改<h3/>
修改:  
http://localhost:8080/api/save/person
<pre><code class="language-json">[
    {
        "code": "33333",
        "name": "33333",
        "id":"417f6982-0e16-45cc-b1d4-51aa070c74d8"
    }
]
</code></pre>
返回:
<pre><code class="language-json">[
    {
        "CODE": "33333",
        "ID": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
        "NAME": "33333"
    }
]
</code></pre>
### <h3 id="2.3">2.3 查询<h3/>
单表查询:  
http://localhost:8080/api/select/person
<pre><code class="language-json">{
    "@column": "id,name",
    "@where": [
        {
            "key": "id",
            "condition": "notnull"
        }
    ],
    "@page":{
        "pageIndex":0,
        "pageSize":10
    }
}
</code></pre>
返回:
<pre><code class="language-json">[
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
</code></pre>
多表关联查询:  
http://localhost:8080/api/select/person
<pre><code class="language-json">{
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
    "@join":{
        "join":"dept on person.deptcode = dept.code"
    }
}
</code></pre>
返回:
<pre><code class="language-json">[
    {
        "DEPTCODE": "deptcode1",
        "ID": "001",
        "NAME": "name1"
    }
]
</code></pre>
### <h3 id="2.4">2.4 删除<h3/>
请求:  
http://localhost:8080/api/delete/person
<pre><code class="language-json">{
    "@where": [
        {
            "key": "id",
            "condition": "in",
            "value":["417f6982-0e16-45cc-b1d4-51aa070c74d8","d89cbafc-cf9f-445a-89fa-9cc53f8b55b8"]
        }
    ]
}
</code></pre>
返回:
<pre><code class="language-json">2
</code></pre>