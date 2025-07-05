package noodlezip.ramen.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import noodlezip.ramen.entity.QRamenReview;
import noodlezip.store.dto.StoreReviewDto;
import noodlezip.store.entity.QMenu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RamenReviewRepositoryImpl implements RamenReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StoreReviewDto> findReviewsByStoreId(Long storeId, Pageable pageable) {
        QRamenReview review = QRamenReview.ramenReview;
        QMenu menu = QMenu.menu;

        List<StoreReviewDto> content = queryFactory
                .select(Projections.constructor(
                        StoreReviewDto.class,
                        review.id,
                        review.communityId,
                        menu.id,
                        menu.menuName,
                        review.noodleThickness,
                        review.noodleTexture,
                        review.noodleBoilLevel,
                        review.soupTemperature,
                        review.soupSaltiness,
                        review.soupSpicinessLevel,
                        review.soupOiliness,
                        review.soupFlavorKeywords,
                        review.content,
                        review.reviewImageUrl,
                        review.isReceiptReview
                ))
                .from(review)
                .join(review.menu, menu)
                .where(menu.store.id.eq(storeId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리
        Long count = queryFactory
                .select(review.count())
                .from(review)
                .join(review.menu, menu)
                .where(menu.store.id.eq(storeId))
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }
}
