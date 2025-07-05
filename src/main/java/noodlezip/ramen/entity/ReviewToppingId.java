package noodlezip.ramen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReviewToppingId implements Serializable {

    @NotNull
    @Column(name = "ramen_review_id", nullable = false)
    private Long ramenReviewId;

    @NotNull
    @Column(name = "store_extra_topping_id", nullable = false)
    private Long storeExtraToppingId;

}