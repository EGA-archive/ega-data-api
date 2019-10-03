package eu.elixir.ega.ebi.keyproviderservice.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Profile("db")
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "keyEntityManagerFactory",
    transactionManagerRef = "encryptionKey_transactionManager", basePackages = {"eu.elixir.ega.ebi.keyproviderservice.domain.key.repository"})
public class EncryptionKeyConfig {
  
  @Bean("encryptionKey_datasource_properties")
  @ConfigurationProperties("datasource.encryptionKey")
  public DataSourceProperties encryptionKeyDataSourceProperties() {
      return new DataSourceProperties();
  }

  @Bean(name = "keyDataSource")
  @ConfigurationProperties("datasource.encryptionKey.hikari")
  public DataSource encryptionKeyDataSource() {
      return encryptionKeyDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }
  
  @Bean(name = "keyEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean keyEntityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("keyDataSource") DataSource dataSource) {
    return builder.dataSource(dataSource).packages("eu.elixir.ega.ebi.keyproviderservice.domain.key.entity").persistenceUnit("encryptionKey")
        .build();
  }

  @Bean("encryptionKey_transactionManager")
  public JpaTransactionManager encryptionKeyTransactionManager(@Qualifier("keyEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
  }

}