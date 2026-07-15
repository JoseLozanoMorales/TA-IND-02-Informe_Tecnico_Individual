package ec.edu.uteq.bancoaustro.config;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
    @Bean(name = "dsCuenca")
    DataSource dsCuenca(@Value("${datasources.cuenca.url}") String url,
                        @Value("${datasources.cuenca.username}") String username,
                        @Value("${datasources.cuenca.password}") String password) {
        return construir(url, username, password);
    }

    @Bean(name = "dsQuito")
    DataSource dsQuito(@Value("${datasources.quito.url}") String url,
                       @Value("${datasources.quito.username}") String username,
                       @Value("${datasources.quito.password}") String password) {
        return construir(url, username, password);
    }

    @Bean(name = "dsGuayaquil")
    DataSource dsGuayaquil(@Value("${datasources.guayaquil.url}") String url,
                           @Value("${datasources.guayaquil.username}") String username,
                           @Value("${datasources.guayaquil.password}") String password) {
        return construir(url, username, password);
    }

    private DataSource construir(String url, String username, String password) {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
        dataSource.setConnectionTimeout(3000);
        return dataSource;
    }
}
