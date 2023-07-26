package io.mosip.packet.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"io.mosip.kernel.biometrics.*","io.mosip.kernel.cbeffutil.*"})
public class DataExtractorRefImplApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataExtractorRefImplApplication.class, args);
    }
}
