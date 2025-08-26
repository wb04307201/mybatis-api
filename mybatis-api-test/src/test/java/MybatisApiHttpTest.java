import cn.wubo.mybatis.api.MyBatisApiConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(classes = {TestConfig.class, DataSourceConfig.class, MybatisAutoConfiguration.class, MyBatisApiConfiguration.class, WebMvcAutoConfiguration.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.yml")
class MybatisApiHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;


    @Test
    void testHtttp() throws Exception {
        Connection connection = dataSource.getConnection();

        connection.createStatement().execute("DROP TABLE IF EXISTS person");

        connection.createStatement().execute("CREATE TABLE person (id VARCHAR(255), code VARCHAR(255), name VARCHAR(255), deptcode VARCHAR(255))");

        connection.close();

        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc.perform(post("/api/insertOrUpdate/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "55555", "name", "55555"))))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/select/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("@column", "id,name", "@where", List.of(Map.of("key", "id", "condition", "notnull"))))))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
