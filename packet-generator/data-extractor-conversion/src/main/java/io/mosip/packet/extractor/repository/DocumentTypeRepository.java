package io.mosip.packet.extractor.repository;

import java.util.List;

import io.mosip.packet.extractor.entity.DocumentType;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * Interface for {@link DocumentType}
 * 
 * @author Brahmananda Reddy
 *
 */

public interface DocumentTypeRepository extends BaseRepository<DocumentType, String> {

	List<DocumentType> findByIsActiveTrueAndLangCodeAndCodeIn(String langCode, List<String> docCode);

	List<DocumentType> findByIsActiveTrueAndName(String docTypeName);
	
	List<DocumentType> findAllByIsActiveTrue();

	DocumentType findByIsActiveTrueAndLangCodeAndCode(String langCode, String docCode);
}
