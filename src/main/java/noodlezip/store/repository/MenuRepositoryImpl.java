package noodlezip.store.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import noodlezip.ramen.entity.QCategory;
import noodlezip.ramen.entity.QRamenSoup;
import noodlezip.store.dto.MenuDetailDto;
import noodlezip.store.entity.QMenu;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class MenuRepositoryImpl implements MenuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MenuDetailDto> findMenuDetailByStoreId(Long storeId) {
        QMenu menu = QMenu.menu;
        QCategory category = QCategory.category;
        QRamenSoup soup = QRamenSoup.ramenSoup;

        return queryFactory
                .select(Projections.constructor(MenuDetailDto.class,
                        menu.id,
                        menu.menuName,
                        menu.price,
                        menu.menuDescription,
                        menu.menuImageUrl,
                        category.categoryName,
                        soup.soupName
                ))
                .from(menu)
                .join(menu.category, category)
                .join(menu.ramenSoup, soup)
                .where(menu.store.id.eq(storeId))
                .fetch();
    }

}
