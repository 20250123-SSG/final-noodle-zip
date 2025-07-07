package noodlezip.store.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.admin.dto.RegistListDto;
import noodlezip.common.exception.CustomException;
import noodlezip.common.status.ErrorStatus;
import noodlezip.common.util.FileUtil;
import noodlezip.common.util.PageUtil;
import noodlezip.ramen.dto.CategoryResponseDto;
import noodlezip.ramen.dto.ToppingResponseDto;
import noodlezip.ramen.entity.Category;
import noodlezip.ramen.entity.RamenSoup;
import noodlezip.ramen.entity.RamenTopping;
import noodlezip.ramen.entity.Topping;
import noodlezip.ramen.repository.RamenReviewRepository;
import noodlezip.ramen.repository.RamenToppingRepository;
import noodlezip.ramen.repository.ReviewToppingRepository;
import noodlezip.ramen.repository.ToppingRepository;
import noodlezip.ramen.service.RamenService;
import noodlezip.store.dto.*;
import noodlezip.store.entity.*;
import noodlezip.store.repository.MenuRepository;
import noodlezip.store.repository.StoreExtraToppingRepository;
import noodlezip.store.repository.StoreRepository;
import noodlezip.store.repository.StoreWeekScheduleRepository;
import noodlezip.store.status.ApprovalStatus;
import noodlezip.user.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreWeekScheduleRepository scheduleRepository;
    private final MenuRepository menuRepository;
    private final RamenToppingRepository ramenToppingRepository;
    private final RamenService ramenService;
    private final ModelMapper modelMapper;
    private final PageUtil pageUtil;
    private final ToppingRepository toppingRepository;
    private final FileUtil fileUtil;
    private final EntityManager em;
    private final RamenReviewRepository ramenReviewRepository;
    private final ReviewToppingRepository reviewToppingRepository;
    private final StoreExtraToppingRepository storeExtraToppingRepository;


    @Transactional(rollbackFor = Exception.class)
    public Long registerStore(StoreRequestDto dto, MultipartFile storeMainImage, User user) {
        String storeMainImageUrl = null;

        // ① 대표 이미지 업로드 및 유효성 검사
        if (storeMainImage != null && !storeMainImage.isEmpty()) {
            try {
                Map<String, String> uploadResult = fileUtil.fileupload("store", storeMainImage);
                storeMainImageUrl = uploadResult.get("fileUrl"); // 전체 URL 저장
                log.info("Store main image uploaded for user {}: {}", user.getId(), storeMainImageUrl);
            } catch (CustomException ce) {
                throw ce; // 확장자, 용량 등 오류
            } catch (Exception e) {
                log.error("Failed to upload store main image for user {}: {}", user.getId(), e.getMessage(), e);
                throw new CustomException(ErrorStatus._INTERNAL_SERVER_ERROR);
            }
            log.warn("대표 이미지가 첨부되지 않았습니다.");
            throw new CustomException(ErrorStatus._FILE_REQUIRED);
        }

        // ② Store 엔티티 생성 및 저장
        Store store = Store.builder()
                .storeName(dto.getStoreName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .isLocalCard(dto.getIsLocalCard())
                .isChildAllowed(dto.getIsChildAllowed())
                .hasParking(dto.getHasParking())
                .ownerComment(dto.getOwnerComment())
                .storeMainImageUrl(storeMainImageUrl)
                .storeLat(dto.getStoreLat())
                .storeLng(dto.getStoreLng())
                .approvalStatus(dto.getApprovalStatus())
                .operationStatus(dto.getOperationStatus())
                .userId(user.getId())
                .build();
        store.setStoreLegalCode(dto.getStoreLegalCode() != null ? dto.getStoreLegalCode().longValue() : null);

        Store savedStore = storeRepository.save(store);

        // ③ 영업시간 저장
        if (dto.getWeekSchedule() != null) {
            List<StoreWeekSchedule> schedules = dto.getWeekSchedule().stream()
                    .map(s -> {
                        StoreWeekScheduleId id = new StoreWeekScheduleId(savedStore.getId(), s.getDayOfWeek());
                        StoreWeekSchedule schedule = new StoreWeekSchedule();
                        schedule.setId(id);
                        schedule.setIsClosedDay(s.getIsClosedDay());
                        if (Boolean.TRUE.equals(s.getIsClosedDay())) {
                            schedule.setOpeningAt(null);
                            schedule.setClosingAt(null);
                        } else {
                            schedule.setOpeningAt(s.getOpeningAt());
                            schedule.setClosingAt(s.getClosingAt());
                        }
                        return schedule;
                    }).collect(Collectors.toList());
            scheduleRepository.saveAll(schedules);
        }

        // ④ 메뉴 및 기본 토핑, 추가 토핑 저장
        if (dto.getMenus() != null) {
            for (MenuRequestDto m : dto.getMenus()) {
                String menuImageUrl = null;
                MultipartFile menuImageFile = m.getMenuImageFile();

                // 메뉴 이미지 업로드
                if (menuImageFile != null && !menuImageFile.isEmpty()) {
                    try {
                        Map<String, String> uploadResult = fileUtil.fileupload("menu", menuImageFile);
                        menuImageUrl = uploadResult.get("fileUrl"); // 전체 URL 저장
                        log.info("Menu image uploaded for menu {}: {}", m.getMenuName(), menuImageUrl);
                    } catch (CustomException ce) {
                        throw ce;
                    } catch (Exception e) {
                        log.error("Failed to upload menu image for menu {}: {}", m.getMenuName(), e.getMessage(), e);
                        throw new CustomException(ErrorStatus._INTERNAL_SERVER_ERROR);
                    }
                }

                // 클라이언트에서 직접 URL 보낸 경우 사용
                if (menuImageUrl == null) {
                    menuImageUrl = m.getMenuImageUrl();
                }

                // 카테고리/육수 매핑
                Category category = new Category();
                category.setId(m.getRamenCategoryId());

                RamenSoup soup = new RamenSoup();
                soup.setId(m.getRamenSoupId());

                // 메뉴 엔티티 저장
                Menu menu = Menu.builder()
                        .store(savedStore)
                        .menuName(m.getMenuName())
                        .price(m.getPrice())
                        .menuDescription(m.getMenuDescription())
                        .menuImageUrl(menuImageUrl)
                        .category(category)
                        .ramenSoup(soup)
                        .build();

                Menu savedMenu = menuRepository.save(menu);

                // 기본 토핑 저장
                if (m.getDefaultToppingIds() != null) {
                    for (Long toppingId : m.getDefaultToppingIds()) {
                        Topping topping = toppingRepository.getReferenceById(toppingId);
                        RamenTopping ramenTopping = new RamenTopping();
                        ramenTopping.setMenu(savedMenu);
                        ramenTopping.setTopping(topping);
                        ramenToppingRepository.save(ramenTopping);
                    }
                }

                // 추가 토핑 저장
                if (m.getExtraToppings() != null && !m.getExtraToppings().isEmpty()) {
                    for (String toppingName : m.getExtraToppings()) {
                        if (toppingName == null || toppingName.isBlank()) continue;

                        Topping topping = Topping.builder()
                                .toppingName(toppingName)
                                .isActive(true)
                                .build();
                        toppingRepository.save(topping);

                        StoreExtraTopping storeExtraTopping = new StoreExtraTopping();
                        storeExtraTopping.setStore(savedStore);
                        storeExtraTopping.setTopping(topping);
                        storeExtraToppingRepository.save(storeExtraTopping);
                    }
                }
            }
        }

        return savedStore.getId();
    }

    // 라멘 카테고리 목록 조회
    public List<CategoryResponseDto> getRamenCategories() {
        return ramenService.getAllCategories();
    }

    // 라멘 토핑 목록 조회
    public List<ToppingResponseDto> getRamenToppings() {
        return ramenService.getAllToppings();
    }

    // 등록요청매장 조회
    public Map<String, Object> findWaitingStores(Pageable pageable) {
        Page<RegistListDto> resultPage = storeRepository.findWaitingStores(pageable);
        Map<String, Object> map = pageUtil.getPageInfo(resultPage, resultPage.getSize());
        map.put("registList", resultPage.getContent());
        return map;
    }

    // ID로 활성화 된 매장 찾기
    public StoreDto getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NoSuchElementException("해당 매장을 찾을 수 없습니다."));
        if (!ApprovalStatus.APPROVED.equals(store.getApprovalStatus())) {
            throw new IllegalStateException("승인되지 않은 매장은 조회할 수 없습니다.");
        }

        return StoreDto.toDto(store);
    }

    // 매장에서 메뉴 조회
    public List<MenuDetailDto> getMenus(Long storeId) {
        List<MenuDetailDto> menuList = menuRepository.findMenuDetailByStoreId(storeId);
        Map<Long, List<String>> toppingMap = ramenToppingRepository.findToppingNamesByStoreGroupedByMenuId(storeId);

        // 메뉴에 라멘토핑 맵핑
        for (MenuDetailDto dto : menuList) {
            List<String> toppings = toppingMap.getOrDefault(dto.getMenuId(), List.of());
            dto.setToppingNames(toppings);
        }

        return menuList;
    }

    // 매장 리뷰 조회
    public Page<StoreReviewDto> getReviews(Long storeId, Pageable pageable) {
        // 1. 리뷰 + 메뉴 페이지 조회
        Page<StoreReviewDto> page = ramenReviewRepository.findReviewsByStoreId(storeId, pageable);
        List<StoreReviewDto> dtoList = page.getContent();

        // 2. 리뷰 ID 목록 추출
        List<Long> reviewIds = dtoList.stream()
                .map(StoreReviewDto::getId)
                .toList();

        // 3. 토핑 조회
        Map<Long, List<String>> toppingMap = reviewToppingRepository.findToppingNamesByReviewIds(reviewIds);

        // 4. 병합
        dtoList.forEach(dto -> dto.setToppingNames(
                toppingMap.getOrDefault(dto.getId(), List.of())
        ));

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }


}
