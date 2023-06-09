package io.mosip.packet.core.repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.packet.core.entity.PacketTracker;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PacketTrackerRepository extends BaseRepository<PacketTracker, String> {

    List<PacketTracker> findByStatusIn(List<String> statusList);
}
