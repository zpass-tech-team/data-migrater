package io.mosip.packet.client.config;

import io.mosip.kernel.logger.logback.factory.Logfactory;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Configuration class for Registration-Service Module
 *
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Configuration
@ComponentScan(basePackages = { "io.mosip.packet.client.*", "io.mosip.packet.core.dto.*"})
@PropertySource(value = { "classpath:application.properties"})
@ImportAutoConfiguration(io.mosip.packet.extractor.DataProcessApplication.class)
//@EnableConfigurationProperties
//@EnableRetry
public class AppConfig {
}

