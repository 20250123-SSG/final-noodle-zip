package noodlezip.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_store_week_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 복합키를 피하고 싶다면 이 필드 필요, 아니면 생략 가능

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek;

    @Column(name = "opening_at", nullable = false)
    private LocalDateTime openingAt;

    @Column(name = "closing_at", nullable = false)
    private LocalDateTime closingAt;

    @Column(name = "is_closed_day", nullable = false)
    private Boolean isClosedDay;
}
