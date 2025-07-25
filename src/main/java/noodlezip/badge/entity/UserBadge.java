package noodlezip.badge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noodlezip.badge.constants.PostStatusType;
import noodlezip.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder

@Entity
@Table(name = "tbl_user_badge")
public class UserBadge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_badge_id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "current_value", nullable = false)
    private Integer currentValue;

    @Column(name = "accumulative_value")
    private Integer accumulativeValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status", nullable = false, length = 30)
    private PostStatusType postStatus;

    @Column(name = "obtained_at")
    private LocalDateTime obtainedAt;


    public void obtain() {
        obtainedAt = LocalDateTime.now();
    }

    public void updateCurrentValueByOne() {
        if (!isOverCompletionValue()) {
            currentValue++;
        }
    }

    public void updateAccumulativeValueByOne() {
        accumulativeValue++;
    }

    public boolean isOverCompletionValue() {
        return badge.isOverCompletionValue(currentValue);
    }

    public boolean isFinalBadgeAlreadyObtained() {
        return obtainedAt != null && badge.hasNothingNextBadge();
    }

    public boolean isNextBadgeUpgradable() {
        return isOverCompletionValue() && !badge.hasNothingNextBadge();
    }

}
