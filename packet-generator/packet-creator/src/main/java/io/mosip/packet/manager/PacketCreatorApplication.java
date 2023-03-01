package io.mosip.packet.manager;


import io.mosip.packet.manager.util.mock.sbi.devicehelper.MockDeviceUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class PacketCreatorApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PacketCreatorApplication.class, args);

        try {
            context.getBean(MockDeviceUtil.class).initDeviceHelpers();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
