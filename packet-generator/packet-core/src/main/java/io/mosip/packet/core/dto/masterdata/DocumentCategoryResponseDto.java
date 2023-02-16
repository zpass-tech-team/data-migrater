package io.mosip.packet.core.dto.masterdata;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Neha
 * @since 1.0.0
 */
@Data
@Setter
@Getter
public class DocumentCategoryResponseDto {
	private List<DocumentCategoryDto> documentcategories;
}
