package io.mosip.packet.manager;


import io.mosip.kernel.partnercertservice.controller.PartnerCertManagerController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class PacketCreatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PacketCreatorApplication.class, args);
    }
}
