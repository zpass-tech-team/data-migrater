package io.mosip.data.repository;

import java.util.List;

import io.mosip.data.entity.DocumentCategory;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * Interface for {@link DocumentCategory}
 * 
 * @author Brahmananda Reddy
 *
 */
public interface DocumentCategoryRepository extends BaseRepository<DocumentCategory, String> {

	List<DocumentCategory> findByIsActiveTrueAndLangCode(String langCode);

	List<DocumentCategory> findAllByIsActiveTrue();

	DocumentCategory findByIsActiveTrueAndCodeAndLangCode(String docCategeoryCode, String langCode);
	
	DocumentCategory findByIsActiveTrueAndCodeAndNameAndLangCode(String docCategeoryCode,String docName, String langCode);

}
