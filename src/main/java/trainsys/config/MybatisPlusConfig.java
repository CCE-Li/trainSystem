package trainsys.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("trainsys.dao.mapper")
public class MybatisPlusConfig {
}
