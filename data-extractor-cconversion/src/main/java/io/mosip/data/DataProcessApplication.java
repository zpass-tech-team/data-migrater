package io.mosip.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(scanBasePackages = { "io.mosip.data.*", "${mosip.auth.adapter.impl.basepackage}"}, exclude = {SecurityAutoConfiguration.class})
public class DataProcessApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataProcessApplication.class, args);
    }
}
