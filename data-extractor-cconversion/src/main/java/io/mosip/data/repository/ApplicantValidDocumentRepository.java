package io.mosip.data.repository;

import java.util.List;

import io.mosip.data.entity.ApplicantValidDocument;
import io.mosip.data.entity.id.ApplicantValidDocumentID;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * Interface for {@link ApplicantValidDocument} 
 * 
 * @author Brahmananda Reddy
 *
 */

public interface ApplicantValidDocumentRepository extends BaseRepository<ApplicantValidDocument, ApplicantValidDocumentID> {

	List<ApplicantValidDocument> findByIsActiveTrueAndDocumentCategoryCodeAndDocumentCategoryLangCode(String docCategoryCode,
																									  String langCode);

	List<ApplicantValidDocument> findByIsActiveTrueAndValidDocumentAppTypeCodeAndValidDocumentDocCatCode(
			String applicantType, String docCategoryCode);
	
}
