import cn.wubo.mybatis.api.Builder;
import cn.wubo.mybatis.api.MyBatisApiConfiguration;
import cn.wubo.mybatis.api.PageVO;
import cn.wubo.mybatis.api.service.MyBatisApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(classes = {DataSourceConfig.class, MybatisAutoConfiguration.class, MyBatisApiConfiguration.class})
@TestPropertySource(locations = "classpath:application.yml")
public class MybatisApiTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MyBatisApiService myBatisApiService;


    @Test
    public void testBuilder() {
        Builder builder = new Builder();

        String sql = builder.insert("person", Map.of("code", "11111", "name", "11111"));
        log.info("新增单条数据 {}", sql);
        assertTrue("INSERT INTO person\n (code, name)\nVALUES ('11111', '11111')".equals(sql) || "INSERT INTO person\n (name, code)\nVALUES ('11111', '11111')".equals(sql));

        sql = builder.update("person", Map.of("code", "33333", "name", "33333", "@where", List.of(Map.of("key", "id", "condition", "eq", "value", "11111"))));
        log.info("修改单查询数据 {}", sql);
        assertTrue("UPDATE person\nSET code = '33333', name = '33333'\nWHERE (id = '11111')".equals(sql) || "UPDATE person\nSET name = '33333', code = '33333'\nWHERE (id = '11111')".equals(sql));

        sql = builder.select("person", Map.of("@column", "id,name", "@where", List.of(Map.of("key", "id", "condition", "notnull"))));
        log.info("单表查询 {}", sql);
        assertEquals("SELECT id, name\n" + "FROM person\n" + "WHERE (id is not null )", sql);

        sql = builder.select("person", Map.of("@where", List.of(Map.of("key", "id", "condition", "notnull")), "@page", Map.of("pageIndex", 0, "pageSize", 10)));
        log.info("分页查询 {}", sql);
        assertEquals("SELECT *\n" + "FROM person\n" + "WHERE (id is not null ) LIMIT 10 OFFSET 0", sql);

        sql = builder.select("person", Map.of("@column", "person.id,person.name,person.deptcode,dept.name", "@where", List.of(Map.of("key", "person.id", "condition", "notnull")), "@join", Map.of("join", "dept on person.deptcode = dept.code")));
        log.info("多表关联查询 {}", sql);
        assertEquals("SELECT person.id, person.name, person.deptcode, dept.name\n" + "FROM person\n" + "JOIN dept on person.deptcode = dept.code\n" + "WHERE (person.id is not null )", sql);
    }

    @Test
    public void testApi() throws Exception {
        Connection connection = dataSource.getConnection();

        connection.createStatement().execute("DROP TABLE IF EXISTS person");

        connection.createStatement().execute("CREATE TABLE person (id VARCHAR(255), code VARCHAR(255), name VARCHAR(255), deptcode VARCHAR(255))");

        connection.close();

        ObjectMapper objectMapper = new ObjectMapper();

        //新增单条数据
        Object result = myBatisApiService.parse("insert", "person", objectMapper.writeValueAsString(Map.of("id", "11111", "code", "11111", "name", "11111")));
        log.info("新增单条数据 {}", result);
        assertEquals(1, result);


        //新增批量数据
        result = myBatisApiService.parse("insert", "person", objectMapper.writeValueAsString(List.of(Map.of("id", "22222", "code", "22222", "name", "22222"), Map.of("id", "33333", "code", "33333", "name", "33333"))));
        log.info("新增批量数据 {}", result);
        assertEquals(List.of(1, 1), result);


        //新增后按照查询返回数据
        result = myBatisApiService.parse("insert", "person", objectMapper.writeValueAsString(Map.of("id", "44444", "code", "44444", "name", "44444", "@with_select", Map.of("@column", "id,code,name", "@where", List.of(Map.of("key", "id", "value", "44444"))))));
        log.info("新增后按照查询返回数据 {}", result);
        assertEquals(List.of(Map.of("code", "44444", "name", "44444", "id", "44444")), result);


        //按照一个查询结果修改数据
        result = myBatisApiService.parse("update", "person", objectMapper.writeValueAsString(Map.of("name", "11111+", "@where", List.of(Map.of("key", "id", "condition", "eq", "value", "11111")))));
        log.info("按照一个查询结果修改数据 {}", result);
        assertEquals(1, result);


        //按照多个查询结果修改数据
        result = myBatisApiService.parse("update", "person", objectMapper.writeValueAsString(List.of(Map.of("name", "22222+", "@where", List.of(Map.of("key", "id", "condition", "eq", "value", "22222"))), Map.of("name", "33333+", "@where", List.of(Map.of("key", "id", "condition", "eq", "value", "33333"))))));
        log.info("按照多个查询结果修改数据 {}", result);
        assertEquals(List.of(1, 1), result);


        //修改后按照查询返回数据
        result = myBatisApiService.parse("update", "person", objectMapper.writeValueAsString(Map.of("name", "44444+", "@where", List.of(Map.of("key", "id", "condition", "eq", "value", "44444")), "@with_select", Map.of("@column", "id,code,name", "@where", List.of(Map.of("key", "id", "value", "44444"))))));
        log.info("修改后按照查询返回数据 {}", result);
        assertEquals(List.of(Map.of("code", "44444", "name", "44444+", "id", "44444")), result);

        //新增或修改单条数据
        result = myBatisApiService.parse("insertOrUpdate", "person", objectMapper.writeValueAsString(Map.of("code", "55555", "name", "55555")));
        log.info("新增或修改单条数据 {}", result);
        assertEquals("55555", ((Map<String, Object>) result).get("name"));

        //新增或修改批量数据
        result = myBatisApiService.parse("insertOrUpdate", "person", objectMapper.writeValueAsString(List.of(Map.of("code", "66666", "name", "66666"), Map.of("name", "44444++", "id", "44444"))));
        log.info("新增或修改批量数据 {}", result);
        assertEquals(2, ((List<?>) result).size());

        //查询
        result = myBatisApiService.parse("select", "person", objectMapper.writeValueAsString(Map.of("@column", "id,name", "@where", List.of(Map.of("key", "id", "condition", "notnull")))));
        log.info("查询 {}", result);
        assertEquals(6, ((List<?>) result).size());

        //分页查询
        result = myBatisApiService.parse("select", "person", objectMapper.writeValueAsString(Map.of("@where", List.of(Map.of("key", "id", "condition", "notnull")), "@page", Map.of("pageIndex", 0, "pageSize", 2))));
        log.info("查询 {}", result);
        assertEquals(2, ((PageVO) result).getRecords().size());


        connection = dataSource.getConnection();

        connection.createStatement().execute("DROP TABLE IF EXISTS dept");

        connection.createStatement().execute("CREATE TABLE dept (id VARCHAR(255), code VARCHAR(255), name VARCHAR(255))");

        connection.close();

        myBatisApiService.parse("update", "person", objectMapper.writeValueAsString(Map.of("deptcode", "11111", "@where", List.of(Map.of("key", "id", "condition", "notnull")))));
        myBatisApiService.parse("insert", "dept", objectMapper.writeValueAsString(Map.of("id", "11111", "code", "11111", "name", "11111")));

        //多表关联查询
        result = myBatisApiService.parse("select", "person", objectMapper.writeValueAsString(Map.of("@column", "person.id,person.name,person.deptcode,dept.name as deptname", "@where", List.of(Map.of("key", "person.id", "condition", "notnull")), "@join", Map.of("join", "dept on person.deptcode = dept.code"))));
        log.info("多表关联查询 {}", result);
        assertEquals(6, ((List<?>) result).size());

        //分组查询
        result = myBatisApiService.parse("select", "person", objectMapper.writeValueAsString(Map.of("@column", "deptcode,count(1) as personcount", "@where", List.of(Map.of("key", "id", "condition", "notnull")), "@group", List.of("deptcode"))));
        log.info("分组查询 {}", result);
        assertEquals(6L, ((Map<String, Object>) ((List<?>) result).get(0)).get("personcount"));

        myBatisApiService.parse("insert", "person", objectMapper.writeValueAsString(Map.of("id", "77777", "code", "11111", "name", "11111+")));

        //去重和排序
        result = myBatisApiService.parse("select", "person", objectMapper.writeValueAsString(Map.of("@column", "name,code", "@where", List.of(Map.of("key", "id", "condition", "notnull")), "@distinct", true, "@order", List.of("code desc"))));
        log.info("去重和排序 {}", result);
        assertEquals(6, ((List<?>) result).size());

        //删除
        result = myBatisApiService.parse("delete", "person", objectMapper.writeValueAsString(Map.of("@where", List.of(Map.of("key", "id", "condition", "in", "value", List.of("11111", "22222"))))));
        log.info("删除 {}", result);
        assertEquals(2, result);

        //groovy
        result = myBatisApiService.parse("insertOrUpdate", "person", objectMapper.writeValueAsString(Map.of("code(G)", "import java.time.LocalDateTime;import java.time.format.DateTimeFormatter;return LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))", "name", "88888")));
        log.info("groovy {}", result);
        assertEquals(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), ((Map<String, Object>) result).get("code"));

    }

}
