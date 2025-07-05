package noodlezip.store.dto;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
public class StoreReviewDto {

    private Long id;
    private Long communityId;
    private Long menuId;
    private String menuName;
    private Integer noodleThickness;
    private Integer noodleTexture;
    private Integer noodleBoilLevel;
    private Integer soupTemperature;
    private Integer soupSaltiness;
    private Integer soupSpicinessLevel;
    private Integer soupOiliness;
    private String soupFlavorKeywords;
    private String content;
    private String reviewImageUrl;
    private Boolean isReceiptReview;
    private List<String> toppingNames;

    public StoreReviewDto(
            Long id,
            Long communityId,
            Long menuId,
            String menuName,
            Integer noodleThickness,
            Integer noodleTexture,
            Integer noodleBoilLevel,
            Integer soupTemperature,
            Integer soupSaltiness,
            Integer soupSpiciness,
            Integer soupOiliness,
            String soupFlavorKeywords,
            String content,
            String reviewImageUrl,
            Boolean isReceiptReview
    ) {
        this.id = id;
        this.communityId = communityId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.noodleThickness = noodleThickness;
        this.noodleTexture = noodleTexture;
        this.noodleBoilLevel = noodleBoilLevel;
        this.soupTemperature = soupTemperature;
        this.soupSaltiness = soupSaltiness;
        this.soupSpicinessLevel = soupSpiciness;
        this.soupOiliness = soupOiliness;
        this.soupFlavorKeywords = soupFlavorKeywords;
        this.content = content;
        this.reviewImageUrl = reviewImageUrl;
        this.isReceiptReview = isReceiptReview;
    }

}
