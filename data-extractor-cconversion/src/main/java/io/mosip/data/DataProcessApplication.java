package io.mosip.data;

import io.mosip.data.util.ConfigUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.UnknownHostException;

@SpringBootApplication(scanBasePackages = { "io.mosip.data.*", "${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.clientcrypto.*"}, exclude = {SecurityAutoConfiguration.class})
public class DataProcessApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DataProcessApplication.class, args);
        try {
            context.getBean(ConfigUtil.class).loadConfigDetails();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
