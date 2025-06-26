package noodlezip.store.service;

import lombok.RequiredArgsConstructor;
import noodlezip.store.dto.*;
import noodlezip.store.entity.*;
import noodlezip.store.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreScheduleRepository scheduleRepository;
    private final MenuRepository menuRepository;

    @Transactional
    public Long registerStore(StoreRequestDto dto) {
        Store store = Store.builder()
                .storeName(dto.getStoreName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .isLocalCard(dto.getIsLocalCard())
                .isChildAllowed(dto.getIsChildAllowed())
                .hasParking(dto.getHasParking())
                .ownerComment(dto.getOwnerComment())
                .storeMainImageUrl(dto.getStoreMainImageUrl())
                .xAxis(dto.getXAxis())
                .yAxis(dto.getYAxis())
                .build();

        Store savedStore = storeRepository.save(store);

        // 요일별 영업시간 저장
        if (dto.getWeekSchedule() != null && !dto.getWeekSchedule().isEmpty()) {
            List<StoreSchedule> schedules = dto.getWeekSchedule().stream()
                    .map(s -> StoreSchedule.builder()
                            .store(savedStore)
                            .dayOfWeek(s.getDayOfWeek())
                            .openingAt(LocalDateTime.of(LocalDate.now(), s.getOpeningAt()))
                            .closingAt(LocalDateTime.of(LocalDate.now(), s.getClosingAt()))
                            .isClosedDay(s.getIsClosedDay())
                            .build())
                    .collect(Collectors.toList());
            scheduleRepository.saveAll(schedules);
        }

// 메뉴 저장
        if (dto.getMenus() != null && !dto.getMenus().isEmpty()) {
            List<Menu> menus = dto.getMenus().stream()
                    .map(m -> Menu.builder()
                            .store(savedStore)
                            .menuName(m.getMenuName())
                            .price(m.getPrice())
                            .menuDescription(m.getMenuDescription())
                            .menuImageUrl(m.getMenuImageUrl())
                            .ramenCategoryId(m.getRamenCategoryId())
                            .ramenSoupId(m.getRamenSoupId())
                            .build())
                    .collect(Collectors.toList());
            menuRepository.saveAll(menus);
        }
        return savedStore.getId();
    }
}
