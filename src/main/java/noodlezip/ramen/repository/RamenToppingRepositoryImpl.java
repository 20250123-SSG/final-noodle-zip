package noodlezip.ramen.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import noodlezip.ramen.entity.QRamenTopping;
import noodlezip.ramen.entity.QTopping;
import noodlezip.store.entity.QMenu;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RamenToppingRepositoryImpl implements RamenToppingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, List<String>> findToppingNamesByStoreGroupedByMenuId(Long storeId) {
        QRamenTopping ramenTopping = QRamenTopping.ramenTopping;
        QMenu menu = QMenu.menu;
        QTopping topping = QTopping.topping;

        List<Tuple> result = queryFactory
                .select(ramenTopping.menu.id, topping.toppingName)
                .from(ramenTopping)
                .join(ramenTopping.menu, menu)
                .join(ramenTopping.topping, topping)
                .where(menu.store.id.eq(storeId))
                .fetch();

        Map<Long, List<String>> toppingMap = new HashMap<>();

        for (Tuple tuple : result) {
            Long menuId = tuple.get(ramenTopping.menu.id);
            String toppingName = tuple.get(topping.toppingName);

            toppingMap.computeIfAbsent(menuId, k -> new ArrayList<>()).add(toppingName);
        }

        return toppingMap;
    }
}
