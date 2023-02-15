package io.mosip.packet.manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;


/**
 * Spring Configuration class for Registration-Service Module
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */



@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
		".*IdObjectCompositeValidator",
		".*IdObjectMasterDataValidator",
		".*PacketDecryptorImpl",
		".*IdSchemaUtils",
		"io\\.mosip\\.kernel\\.signature\\..*",
		".*OnlinePacketCryptoServiceImpl"}),
		basePackages = {"io.mosip.kernel.partnercertservice.service", "io.mosip.kernel.partnercertservice.helper", "io.mosip.packet.manager.*", "io.mosip.commons.packet" })
@PropertySource(value = { "classpath:bootstrap.properties", "classpath:application-local.properties" })
@Configuration
@EnableRetry
public class AppConfig {

	@Bean
	@Primary
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public RestTemplate selfTokenRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public ObjectMapper mapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return mapper;
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("entities");
	}
}
