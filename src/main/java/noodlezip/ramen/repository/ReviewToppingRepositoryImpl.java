package noodlezip.ramen.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import noodlezip.ramen.entity.QReviewTopping;
import noodlezip.ramen.entity.QTopping;
import noodlezip.store.entity.QStoreExtraTopping;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class ReviewToppingRepositoryImpl implements ReviewToppingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, List<String>> findToppingNamesByReviewIds(List<Long> reviewIds) {
        QReviewTopping reviewTopping = QReviewTopping.reviewTopping;
        QStoreExtraTopping extraTopping = QStoreExtraTopping.storeExtraTopping;
        QTopping topping = QTopping.topping;

        return queryFactory
                .select(
                        reviewTopping.ramenReview.id,
                        topping.toppingName
                )
                .from(reviewTopping)
                .join(reviewTopping.storeExtraTopping, extraTopping)
                .join(extraTopping.topping, topping)
                .where(reviewTopping.ramenReview.id.in(reviewIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(reviewTopping.ramenReview.id),
                        Collectors.mapping(tuple -> tuple.get(topping.toppingName), Collectors.toList())
                ));
    }
}
