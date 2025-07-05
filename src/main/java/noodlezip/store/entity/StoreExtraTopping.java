package noodlezip.store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import noodlezip.ramen.entity.Topping;

@Getter
@Setter
@Entity
@Table(name = "tbl_store_extra_topping")
public class StoreExtraTopping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_extra_topping_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id", nullable = false)
    private Topping topping;

    @Column(name = "price")
    private Integer price;
}