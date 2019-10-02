package eu.elixir.ega.ebi.keyproviderservice.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Profile("db")
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactory",
  transactionManagerRef = "fileKey_transactionManager",    basePackages = {"eu.elixir.ega.ebi.keyproviderservice.domain.file.repository"})
public class FileKeyConfig {
  
  @Primary
  @Bean("fileKey_datasource_properties")
  @ConfigurationProperties("datasource.fileKey")
  public DataSourceProperties fileKeyDataSourceProperties() {
      return new DataSourceProperties();
  }

  @Primary
  @Bean(name = "dataSource")
  @ConfigurationProperties("datasource.fileKey.hikari")
  public DataSource fileKeyDataSource() {
      return fileKeyDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }
  
  @Primary
  @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {
    return builder.dataSource(dataSource).packages("eu.elixir.ega.ebi.keyproviderservice.domain.file.entity").persistenceUnit("fileKey")
        .build();
  }

  @Primary
  @Bean("fileKey_transactionManager")
  public JpaTransactionManager fileKeyTransactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
  }
  
}