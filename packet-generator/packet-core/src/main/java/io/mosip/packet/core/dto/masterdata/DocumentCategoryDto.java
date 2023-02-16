package io.mosip.packet.core.dto.masterdata;
import lombok.Data;

/**
 * Response dto for Document Category Detail
 * 
 * @author Neha Sinha
 * @author Ritesh Sinha
 * 
 * @since 1.0.0
 *
 */
@Data
public class DocumentCategoryDto {

	private String code;
	private String name;
	private String description;
	private String langCode;
	private Boolean isActive;
}
