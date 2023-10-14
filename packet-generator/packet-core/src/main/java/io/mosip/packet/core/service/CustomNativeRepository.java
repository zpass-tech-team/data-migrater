package io.mosip.packet.core.service;

import io.mosip.packet.core.service.impl.CustomNativeRepositoryImpl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface CustomNativeRepository {

    public Object runNativeQuery(String query);

    public void getPacketTrackerData(List<String> status, CustomNativeRepositoryImpl.PacketTrackerInterface processor) throws SQLException, IOException, ClassNotFoundException;
}
