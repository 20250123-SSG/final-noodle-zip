package noodlezip.ramen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import noodlezip.community.entity.Board;
import noodlezip.store.entity.Menu;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tbl_ramen_review")
public class RamenReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ramen_review_id", nullable = false)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "noodle_thickness", nullable = false)
    private Integer noodleThickness;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "noodle_texture", nullable = false)
    private Integer noodleTexture;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "noodle_boil_level", nullable = false)
    private Integer noodleBoilLevel;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "soup_density", nullable = false)
    private Integer soupDensity;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "soup_temperature", nullable = false)
    private Integer soupTemperature;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "soup_saltiness", nullable = false)
    private Integer soupSaltiness;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "soup_spiciness_level", nullable = false)
    private Integer soupSpicinessLevel;

    @Min(0)
    @Max(10)
    @NotNull
    @Column(name = "soup_oiliness", nullable = false)
    private Integer soupOiliness;

    @Size(max = 100)
    @Column(name = "soup_flavor_keywords", length = 100)
    private String soupFlavorKeywords;

    @Lob
    @Column(name = "content")
    private String content;

    @Size(max = 500)
    @Column(name = "review_image_url", length = 500)
    private String reviewImageUrl;

    @Column(name = "is_receipt_review")
    private Boolean isReceiptReview;


}