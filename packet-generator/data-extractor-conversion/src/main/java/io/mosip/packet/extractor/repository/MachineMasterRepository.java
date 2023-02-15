package io.mosip.packet.extractor.repository;
import io.mosip.packet.extractor.entity.MachineMaster;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * The repository interface for {@link MachineMaster} entity
 * @author Yaswanth S
 * @since 1.0.0
 *
 */
public interface MachineMasterRepository extends BaseRepository<MachineMaster, String>{
	
		
	/**
	 * Find machine based on  machine name.
	 * 
	 * @param machineName
	 * @return
	 */
	MachineMaster findByIsActiveTrueAndNameIgnoreCase(String machineName);

	MachineMaster findByNameIgnoreCase(String machineName);
	
}
