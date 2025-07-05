package noodlezip.store.repository;

import noodlezip.store.dto.MenuDetailDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepositoryCustom {

    List<MenuDetailDto> findMenuDetailByStoreId(Long storeId);

}
