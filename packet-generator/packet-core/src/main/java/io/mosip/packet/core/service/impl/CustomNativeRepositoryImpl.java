package io.mosip.packet.core.service.impl;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.packet.core.constant.FieldCategory;
import io.mosip.packet.core.service.CustomNativeRepository;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomNativeRepositoryImpl implements CustomNativeRepository {

    @Autowired
    EntityManager entityManager;

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactoryBean;

    @Override
    public Object runNativeQuery(String query) {
        return entityManager.createNativeQuery(query).getResultList();
    }

    @Override
    public void getPacketTrackerData(List<String> status, PacketTrackerInterface processor) throws SQLException, IOException, ClassNotFoundException {
        EntityManager entityManager1 = entityManagerFactoryBean.getObject().createEntityManager();
        Session session = entityManager1.unwrap(Session.class);
        session.doWork(new Work() {

            @SneakyThrows
            @Override
            public void execute(Connection con) throws SQLException {
                Statement st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                ResultSet resultSet = st.executeQuery("select request from Packet_Tracker where status in ('QUEUED')");

                while(resultSet.next()) {
                    Clob clob = resultSet.getClob(1);
                    ByteArrayInputStream bis = new ByteArrayInputStream(clientCryptoFacade.getClientSecurity().isTPMInstance() ? clientCryptoFacade.decrypt(Base64.getDecoder().decode(clob.getSubString(1, (int)clob.length()).toString())) : clientCryptoFacade.getClientSecurity().asymmetricDecrypt(Base64.getDecoder().decode(clob.getSubString(1, (int)clob.length()).toString())));
                    ObjectInputStream is = new ObjectInputStream(bis);
                    processor.processData((Map<FieldCategory, LinkedHashMap<String, Object>>) is.readObject());
                }
            }
        });
        session.close();
    }

    public interface PacketTrackerInterface {
        public void processData(Map<FieldCategory, LinkedHashMap<String, Object>> dataHashMap) throws Exception;
    }
}
