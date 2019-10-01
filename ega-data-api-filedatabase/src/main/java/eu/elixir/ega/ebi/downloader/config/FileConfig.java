package eu.elixir.ega.ebi.downloader.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "fileEntityManagerFactory",
    transactionManagerRef = "fileTransactionManager", basePackages = {"eu.elixir.ega.ebi.downloader.domain.repository"})
public class FileConfig {
  
  @Primary
  @Bean("file_datasource_properties")
  @ConfigurationProperties("datasource.file")
  public DataSourceProperties fileDataSourceProperties() {
      return new DataSourceProperties();
  }

  @Primary
  @Bean(name = "fileDataSource")
  @ConfigurationProperties("datasource.file.hikari")
  public DataSource fileDataSource() {
      return fileDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }
  
  @Primary
  @Bean(name = "fileEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean fileEntityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("fileDataSource") DataSource dataSource) {
    return builder.dataSource(dataSource).packages("eu.elixir.ega.ebi.downloader.domain.entity").persistenceUnit("file")
        .build();
  }

  @Primary
  @Bean("fileTransactionManager")
  public JpaTransactionManager fileTransactionManager(@Qualifier("fileEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
  }

}