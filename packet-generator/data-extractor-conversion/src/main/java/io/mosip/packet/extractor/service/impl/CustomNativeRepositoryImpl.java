package io.mosip.packet.extractor.service.impl;

import io.mosip.packet.extractor.service.CustomNativeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
public class CustomNativeRepositoryImpl implements CustomNativeRepository {

    @Autowired
    EntityManager entityManager;

    @Override
    public Object runNativeQuery(String query) {
        return entityManager.createNativeQuery(query).getResultList();
    }
}
