package io.mosip.packet.extractor;

import io.mosip.packet.extractor.util.ConfigUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.net.UnknownHostException;


@SpringBootApplication(scanBasePackages = { "io.mosip.packet.extractor.*", "io.mosip.packet.core.*", "io.mosip.packet.manager.*", "${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.clientcrypto.*", "io.mosip.kernel.dataaccess.hibernate", "io.mosip.kernel.keymanagerservice.*"}, exclude = {SecurityAutoConfiguration.class, HibernateDaoConfig.class})
@EntityScan(basePackages = {"io.mosip.packet.core.entity", "io.mosip.kernel.idgenerator.rid.entity", "io.mosip.kernel.keymanagerservice.entity"})
@EnableJpaRepositories(basePackages = {"io.mosip.packet.core.repository", "io.mosip.kernel.idgenerator.rid.repository", "io.mosip.kernel.keymanagerservice.repository"} , repositoryBaseClass = HibernateRepositoryImpl.class)
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
