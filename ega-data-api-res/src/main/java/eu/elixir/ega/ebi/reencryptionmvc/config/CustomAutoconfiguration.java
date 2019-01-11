package eu.elixir.ega.ebi.reencryptionmvc.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CustomAutoconfiguration {

  @Profile("db-repo-logger")
  @Configuration
  @EnableAutoConfiguration
  public class DbAutoconfiguration {

  }

  @Profile("!db-repo-logger")
  @Configuration
  @EnableAutoConfiguration(exclude = {
      DataSourceAutoConfiguration.class,
      DataSourceTransactionManagerAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class})
  public class NoDbAutoconfiguration {

  }

}
