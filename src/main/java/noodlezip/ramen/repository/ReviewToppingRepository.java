package noodlezip.ramen.repository;

import noodlezip.ramen.entity.ReviewTopping;
import noodlezip.ramen.entity.ReviewToppingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewToppingRepository extends JpaRepository<ReviewTopping, ReviewToppingId>, ReviewToppingRepositoryCustom {
}