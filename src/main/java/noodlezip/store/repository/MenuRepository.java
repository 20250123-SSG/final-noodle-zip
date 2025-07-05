package noodlezip.store.repository;

import noodlezip.store.entity.Menu;
import noodlezip.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long>, MenuRepositoryCustom {

    List<Menu> findMenuIdByStoreId(Store storeId);

}
