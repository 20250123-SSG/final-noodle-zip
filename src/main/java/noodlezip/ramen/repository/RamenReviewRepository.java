package noodlezip.ramen.repository;

import noodlezip.ramen.entity.RamenReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RamenReviewRepository extends JpaRepository<RamenReview, Long>, RamenReviewRepositoryCustom {
}