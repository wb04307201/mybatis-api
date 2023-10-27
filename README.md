# mybatis-api-spring-boot-starter

> 基于mybatis提供万能通用接口，大部分接口不用再写，零码便可进行增删改查

[![](https://jitpack.io/v/com.gitee.wb04307201/mybatis-api-spring-boot-starter.svg)](https://jitpack.io/#com.gitee.wb04307201/mybatis-api-spring-boot-starter)

* ## [1.如何使用](#1)
* ## [2.语法 & 示例](#2)
* #### [2.1 新增](#2.1)
* ###### [2.1.1 自定义主键名称](#2.1.1)
* ###### [2.1.2 自定义生成主键值](#2.1.2)
* #### [2.2 修改](#2.2)
* #### [2.3 新增和修改](#2.3)
* #### [2.4 查询](#2.4)
* ###### [2.4.1 自定义结果集映射](#2.4.1)
* #### [2.5 删除](#2.5)
* #### [2.6 请求基础地质](#2.6)


## <h2 id="1">1.如何使用<h2/>
#### 第一步 增加 JitPack 仓库
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

#### 第二步 引入jar
```xml
    <dependency>
        <groupId>com.gitee.wb04307201</groupId>
        <artifactId>mybatis-api-spring-boot-starter</artifactId>
        <version>1.0.4</version>
    </dependency>
```

#### 第三步 在启动类上加上`@EnableMyBatisApi`注解
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
#### <h3 id="2.1">2.1 新增<h3/>
> 新增时如果不传id值会按照uuid的方式填补  
> 请求地址 http://ip:port/api/insert/{tableName}
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
  "code": "22222",
  "id": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
  "name": "22222"
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
    "code": "11111",
    "id": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "name": "11111"
  },
  {
    "code": "22222",
    "id": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
    "name": "22222"
  }
]
```
###### <h4 id="2.1.1">2.1.1 自定义主键名称<h3/>
```yaml
mybatis:
  api:
    id: id #数据库主键名称
```

###### <h4 id="2.1.2">2.1.2 自定义生成主键值<h3/>
```yaml
mybatis:
  api:
    idClass: cn.wubo.mybatis.api.demo.SnowflakeIdServiceImpl #主键生成方法
```
继承IDService接口后实现generalID方法，并注册bean
```java
@Component
public class SnowflakeIdServiceImpl implements IDService<Long> {

    // ==============================Fields===========================================
    /**
     * 开始时间截 (2015-01-01)
     */
    private final long twepoch = 1489111610226L;

    /**
     * 机器id所占的位数
     */
    private final long workerIdBits = 5L;

    /**
     * 数据标识id所占的位数
     */
    private final long dataCenterIdBits = 5L;

    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 支持的最大数据标识id，结果是31
     */
    private final long maxDataCenterId = -1L ^ (-1L << dataCenterIdBits);

    /**
     * 序列在id中占的位数
     */
    private final long sequenceBits = 12L;

    /**
     * 机器ID向左移12位
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据标识id向左移17位(12+5)
     */
    private final long dataCenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间截向左移22位(5+5+12)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;

    /**
     * 数据中心ID(0~31)
     */
    private long dataCenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    //==============================Constructors=====================================

    public SnowflakeIdServiceImpl() {
        this.workerId = 0L;
        this.dataCenterId = 0L;
    }

    /**
     * 构造函数
     *
     * @param workerId     工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public SnowflakeIdServiceImpl(long workerId, long dataCenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId can't be greater than %d or less than 0", maxWorkerId));
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("dataCenterId can't be greater than %d or less than 0", maxDataCenterId));
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    // ==============================Methods==========================================

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    @Override
    public synchronized Long generalID() {
        long timestamp = timeGen();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp;

        //移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) //
                | (dataCenterId << dataCenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
```
## <h3 id="2.2">2.2 修改<h3/>
> 根据查询条件修改数据  
> 请求地址 http://ip:port/api/update/{tableName}
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
    "code": "33333",
    "id": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "name": "33333"
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
      "code": "33333",
      "id": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
      "name": "33333"
    }
  ]
]
```
## <h3 id="2.3">2.3 新增和修改<h3/>
> 请求体包含id则根据id修改数据，不包含id则生成id新增数据    
> 请求地址 http://ip:port/api/inertOrUpdate/{tableName}
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
  "code": "22222",
  "id": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
  "name": "22222"
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
    "code": "11111",
    "id": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "name": "11111"
  },
  {
    "code": "22222",
    "id": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
    "name": "33333"
  }
]
```
## <h3 id="2.4">2.4 查询<h3/>
> 请求地址 http://ip:port/api/select/{tableName}
#### 请求体 单表查询:
```json
{
  "@column": "id,name",
  "@where": [
    {
      "key": "id",
      "condition": "notnull"
    }
  ]
}
```
#### 响应:
```json
[
  {
    "id": "001",
    "name": "name1"
  },
  {
    "id": "417f6982-0e16-45cc-b1d4-51aa070c74d8",
    "name": "33333"
  },
  {
    "id": "d89cbafc-cf9f-445a-89fa-9cc53f8b55b8",
    "name": "22222"
  }
]
```
#### 请求体 分页查询:
```json
{
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
{
  "total": 4,
  "pageSize": 10,
  "pageIndex": 0,
  "records": [
    {
      "code": "22",
      "name": "22",
      "person_memo": "1",
      "id": 872320795374780416
    },
    {
      "code": "11111",
      "name": "11111",
      "person_memo": "2",
      "id": 872320851729448960
    },
    {
      "code": "11111",
      "name": "11111",
      "person_memo": "3",
      "id": 872320854896148480
    },
    {
      "code": "11111",
      "name": "11111",
      "person_memo": "4",
      "id": 872320858343866368
    }
  ]
}
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
  "@join": {
    "join": "dept on person.deptcode = dept.code"
  }
}
```
#### 响应:
```json
[
  {
    "deptcode": "deptcode1",
    "id": "001",
    "name": "name1"
  }
]
```
> @join支持join，inner_join，left_outer_join，right_outer_join，outer_join
> @where condition支持eq,ueq,like,ulike,llike,rlike,gt,lt,gteq,lteq,between,notbetween,in,notin,null,notnull
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
    "deptcode": "deptcode1",
    "personcount": "2"
  }
]
```
###### <h4 id="2.4.1">2.4.1 自定义结果集映射<h3/>
默认可将结果集key值转成小写，也可通过配置和编码重写
```yaml
mybatis:
  api:
    mappingClass: cn.wubo.mybatis.api.demo.CamelCaseMappingServiceImpl #自定义结果集映射,驼峰方式
```
集成IMappingService接口并实现parseKey方法
```java
@Component
public class CamelCaseMappingServiceImpl implements IMappingService {
    @Override
    public String parseKey(String field) {
        String[] words = field.split("[-_]");
        return Arrays.stream(words, 1, words.length).map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase()).reduce(words[0].toLowerCase(), String::concat);
    }
}
```

## <h3 id="2.5">2.5 删除<h3/>
> 请求地址 http://ip:port/api/delete/{tableName}
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
## <h3 id="2.6">2.6 请求基础地质<h3/>
```yaml
mybatis:
  api:
    basePath: api #访问接口基础路径
```
