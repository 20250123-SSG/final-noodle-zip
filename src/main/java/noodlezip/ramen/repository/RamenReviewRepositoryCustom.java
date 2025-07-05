package noodlezip.ramen.repository;

import noodlezip.store.dto.StoreReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RamenReviewRepositoryCustom {

    Page<StoreReviewDto> findReviewsByStoreId(Long storeId, Pageable pageable);

}
