package io.mosip.packet.extractor.repository;

import java.util.List;

import io.mosip.packet.extractor.entity.ApplicantValidDocument;
import io.mosip.packet.extractor.entity.id.ApplicantValidDocumentID;
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
