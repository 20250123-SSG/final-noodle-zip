package noodlezip.ramen.repository;

import java.util.List;
import java.util.Map;

public interface ReviewToppingRepositoryCustom {

    Map<Long, List<String>> findToppingNamesByReviewIds(List<Long> reviewIds);

}
