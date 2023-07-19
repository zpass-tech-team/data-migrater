package io.mosip.packet.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.mosip.packet.core.dto.RequestWrapper;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.dto.dbimport.DBImportRequest;
import io.mosip.packet.core.dto.dbimport.DBImportResponse;
import io.mosip.packet.core.dto.dbimport.PacketCreatorResponse;
import io.mosip.packet.extractor.service.DataExtractionService;
import io.mosip.packet.extractor.util.ConfigUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import io.mosip.packet.extractor.util.Reprocessor;
import io.mosip.packet.manager.util.mock.sbi.devicehelper.MockDeviceUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;


@SpringBootApplication(scanBasePackages = { "io.mosip.packet.*", "${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.clientcrypto.*", "io.mosip.kernel.dataaccess", "io.mosip.kernel.keymanagerservice.*"}, exclude = {SecurityAutoConfiguration.class, HibernateDaoConfig.class, HibernateJpaAutoConfiguration.class})
@EntityScan(basePackages = {"io.mosip.packet.core.entity", "io.mosip.kernel.idgenerator.rid.entity", "io.mosip.kernel.keymanagerservice.entity"})
@EnableJpaRepositories(basePackages = {"io.mosip.packet.core.repository", "io.mosip.kernel.idgenerator.rid.repository", "io.mosip.kernel.keymanagerservice.repository"} , repositoryBaseClass = HibernateRepositoryImpl.class)
public class DataProcessApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DataProcessApplication.class, args);
        try {
            context.getBean(MockDeviceUtil.class).resetDevices();
            context.getBean(MockDeviceUtil.class).initDeviceHelpers();
            context.getBean(ConfigUtil.class).loadConfigDetails();
            context.getBean(Reprocessor.class).reprocess();
            boolean internal = Boolean.parseBoolean(context.getEnvironment().getProperty("mosip.packet.creator.refer.internal.json.file"));
            if(internal) {
                FileInputStream io = new FileInputStream("./ApiRequest.json");
                String requestJson = new String(io.readAllBytes(), StandardCharsets.UTF_8);
                ObjectMapper mapper = new ObjectMapper();
                RequestWrapper<DBImportRequest> request = mapper.readValue(requestJson, new TypeReference<RequestWrapper<DBImportRequest>>() {});
                System.out.println("Request : " + (new Gson()).toJson(request));
                PacketCreatorResponse response =  context.getBean(DataExtractionService.class).createPacketFromDataBase(request.getRequest(), false);
                System.out.println("Response : " + (new Gson()).toJson(response));
                System.exit(0);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
