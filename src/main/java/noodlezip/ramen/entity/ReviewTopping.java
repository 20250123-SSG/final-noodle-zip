package noodlezip.ramen.entity;

import jakarta.persistence.*;
import lombok.*;
import noodlezip.store.entity.StoreExtraTopping;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "tbl_review_topping")
public class ReviewTopping {

    @EmbeddedId
    private ReviewToppingId id;

    @MapsId("ramenReviewId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_review_id")
    private RamenReview ramenReview;

    @MapsId("storeExtraToppingId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_extra_topping_id")
    private StoreExtraTopping storeExtraTopping;

}