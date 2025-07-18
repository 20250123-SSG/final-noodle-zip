package noodlezip.ramen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
public class ReviewToppingId implements Serializable {

    @NotNull
    @Column(name = "ramen_review_id", nullable = false)
    private Long ramenReviewId;

    @NotNull
    @Column(name = "store_extra_topping_id", nullable = false)
    private Long storeExtraToppingId;

}