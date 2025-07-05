package noodlezip.store.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Builder
public class StoreReviewResponseDto {

    private List<StoreReviewDto> reviews;
    private Map<String, Object> pagination;

}
