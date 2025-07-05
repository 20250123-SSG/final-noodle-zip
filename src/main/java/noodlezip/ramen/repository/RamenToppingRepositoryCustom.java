package noodlezip.ramen.repository;

import java.util.List;
import java.util.Map;

public interface RamenToppingRepositoryCustom {

    Map<Long, List<String>> findToppingNamesByStoreGroupedByMenuId(Long storeId);

}
