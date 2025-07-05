package noodlezip.ramen.repository;

import noodlezip.ramen.entity.RamenTopping;
import noodlezip.ramen.entity.RamenToppingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RamenToppingRepository extends JpaRepository<RamenTopping, RamenToppingId>, RamenToppingRepositoryCustom {
}