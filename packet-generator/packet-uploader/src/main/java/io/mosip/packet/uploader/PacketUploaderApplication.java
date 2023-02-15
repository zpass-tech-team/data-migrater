package io.mosip.packet.uploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class PacketUploaderApplication {
        public static void main(String[] args) {
            SpringApplication.run(PacketUploaderApplication.class, args);
        }
}
