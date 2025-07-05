package noodlezip.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import noodlezip.common.entity.BaseTimeEntity;
import noodlezip.ramen.entity.Category;
import noodlezip.ramen.entity.RamenSoup;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "tbl_menu")
public class Menu extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id", nullable = false)
    private Long id;

    // 여러 메뉴는 하나의 매장(Store)에 속한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Size(max = 30)
    @NotNull
    @Column(name = "menu_name", nullable = false, length = 30)
    private String menuName;

    @NotNull
    @Column(name = "price", nullable = false)
    private Integer price;

    @Size(max = 300)
    @Column(name = "menu_description", length = 300)
    private String menuDescription;

    @Size(max = 500)
    @Column(name = "menu_image_url", length = 500)
    private String menuImageUrl;

    // 메뉴는 카테고리(Category)에 속한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_category_id", nullable = false)
    private Category category;

    // 메뉴가 사용하는 라멘 국물(RamenSoup) 정보와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_soup_id", nullable = false)
    private RamenSoup ramenSoup;
}