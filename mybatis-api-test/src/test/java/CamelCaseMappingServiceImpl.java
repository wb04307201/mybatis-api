import cn.wubo.mybatis.api.service.mapping.IMappingService;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CamelCaseMappingServiceImpl implements IMappingService {
    @Override
    public String parseKey(String field) {
        String[] words = field.split("[-_]");
        return Arrays.stream(words, 1, words.length).map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase()).reduce(words[0].toLowerCase(), String::concat);
    }
}
