package noodlezip.community.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import noodlezip.common.entity.BaseTimeEntity;
import noodlezip.ramen.entity.RamenReview;
import noodlezip.user.entity.User;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tbl_community")
@ToString(exclude = "ramenReviews")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
public class Board extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "community_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Size(max = 30)
    @NotNull
    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Size(max = 30)
    @NotNull
    @Column(name = "community_type", nullable = false, length = 30)
    private String communityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status", nullable = false, length = 30)
    private CommunityActiveStatus postStatus;

    @Column(name = "review_store_id")
    private Long reviewStoreId;

    @Column(name = "review_visit_date")
    private LocalDate reviewVisitDate;

    @ColumnDefault("0")
    @Column(name = "likes_count", nullable = false)
    private Integer likesCount;

    @ColumnDefault("0")
    @Column(name = "views_count", nullable = false)
    private Integer viewsCount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "ocr_key_hash")
    private String ocrKeyHash;

    @Version
    @Column(name = "version")
    private Integer version;

}