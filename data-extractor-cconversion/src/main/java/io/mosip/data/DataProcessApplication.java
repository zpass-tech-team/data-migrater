package io.mosip.data;

import io.mosip.data.util.ConfigUtil;
import io.mosip.data.util.MvelUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.net.UnknownHostException;


@SpringBootApplication(scanBasePackages = { "io.mosip.data.*", "${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.clientcrypto.*", "io.mosip.kernel.dataaccess.hibernate"}, exclude = {SecurityAutoConfiguration.class, HibernateDaoConfig.class})
@EntityScan(basePackages = {"io.mosip.data.entity", "io.mosip.kernel.idgenerator.rid.entity"})
@EnableJpaRepositories(basePackages = {"io.mosip.data.repository", "io.mosip.kernel.idgenerator.rid.repository"} , repositoryBaseClass = HibernateRepositoryImpl.class)
public class DataProcessApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DataProcessApplication.class, args);
        try {
            context.getBean(ConfigUtil.class).loadConfigDetails();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
