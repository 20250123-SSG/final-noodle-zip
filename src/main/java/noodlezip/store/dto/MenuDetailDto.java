package noodlezip.store.dto;

import lombok.*;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class MenuDetailDto {
    private Long menuId;
    private String menuName;
    private Integer price;
    private String menuDescription;
    private String menuImageUrl;
    private String categoryName;
    private String soupName;
    private List<String> toppingNames;

    // 메뉴 디테일 검색시 사용 (findMenuDetailByStoreId)
    public MenuDetailDto(Long menuId,
                         String menuName,
                         Integer price,
                         String menuDescription,
                         String menuImageUrl,
                         String categoryName,
                         String soupName) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.price = price;
        this.menuDescription = menuDescription;
        this.menuImageUrl = menuImageUrl;
        this.categoryName = categoryName;
        this.soupName = soupName;
    }

}