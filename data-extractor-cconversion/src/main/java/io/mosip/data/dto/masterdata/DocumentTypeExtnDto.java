package io.mosip.data.dto.masterdata;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO class for document type.
 * 
 * @author Uday Kumar
 * @author Ritesh Sinha
 * @author Neha Sinha
 * 
 * @since 1.0.0
 *
 */
@Data
@Setter
@Getter
public class DocumentTypeExtnDto {
	private String code;
	private String name;
	private String description;
	private String langCode;
	private Boolean isActive;
	private String createdBy;
	private LocalDateTime createdDateTime;
	private String updatedBy;
	private LocalDateTime updatedDateTime;
	private Boolean isDeleted;
	private LocalDateTime deletedDateTime;
}
