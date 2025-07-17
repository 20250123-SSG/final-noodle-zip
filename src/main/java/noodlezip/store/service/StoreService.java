package noodlezip.store.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.admin.dto.RegistListDto;
import noodlezip.common.exception.CustomException;
import noodlezip.common.status.ErrorStatus;
import noodlezip.common.util.FileUtil;
import noodlezip.common.util.PageUtil;
import noodlezip.ramen.dto.CategoryResponseDto;
import noodlezip.ramen.dto.RamenSoupResponseDto;
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
import noodlezip.store.status.OperationStatus;
import noodlezip.user.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import noodlezip.store.status.StoreErrorCode;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
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

    // ❗ 매장 등록 ❗
    public Long registerStore(StoreRequestDto dto, User user) {

        log.debug("DTO: {}", dto);
        log.debug("WeekSchedule: {}", dto.getWeekSchedule());
        if (dto.getWeekSchedule() != null) {
            for (StoreScheduleRequestDto s : dto.getWeekSchedule()) {
                log.debug(" - Day: {}, Open: {}, Close: {}, Closed: {}", s.getDayOfWeek(), s.getOpeningAt(), s.getClosingAt(), s.getIsClosedDay());
            }
        }

        List<String> defaultToppingNames = toppingRepository.findAll().stream()
                .filter(Topping::getIsActive)
                .map(Topping::getToppingName)
                .toList();

        String storeMainImageUrl = null;
        MultipartFile storeMainImage = dto.getStoreMainImage();

        // ① 대표 이미지 업로드 및 유효성 검사
        if (storeMainImage != null && !storeMainImage.isEmpty()) {
            try {
                Map<String, String> uploadResult = fileUtil.fileupload("storeRegist/0708", storeMainImage);
                storeMainImageUrl = uploadResult.get("fileUrl");
            } catch (CustomException ce) {
                throw ce;
            } catch (Exception e) {
                throw new CustomException(ErrorStatus._FILE_UPLOAD_FAILED);
            }
        } else {
            throw new CustomException(ErrorStatus._FILE_REQUIRED);
        }

        // ② Store 엔티티 생성 및 저장
        String fullAddress = dto.getAddress();
        if (dto.getStoreDetailInput() != null && !dto.getStoreDetailInput().isEmpty()) {
            fullAddress += "," + dto.getStoreDetailInput();
        }

        Store store = Store.builder()
                .storeName(dto.getStoreName())
                .address(fullAddress)
                .phone(formatPhoneNumber(dto.getPhone()))
                .bizNum(dto.getBizNum())
                .isLocalCard(dto.getIsLocalCard())
                .isChildAllowed(dto.getIsChildAllowed())
                .hasParking(dto.getHasParking())
                .ownerComment(dto.getOwnerComment())
                .storeMainImageUrl(storeMainImageUrl)
                .storeLat(dto.getStoreLat())
                .storeLng(dto.getStoreLng())
                .approvalStatus(ApprovalStatus.WAITING)
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
                            schedule.setOpeningAt(LocalTime.MIN); // 00:00
                            schedule.setClosingAt(LocalTime.MIN); // 00:00
                        } else {
                            schedule.setOpeningAt(s.getOpeningAt());
                            schedule.setClosingAt(s.getClosingAt());
                        }

                        schedule.setStore(savedStore);

                        return schedule;
                    }).collect(Collectors.toList());

            scheduleRepository.saveAll(schedules);
        }

        // ④ 메뉴 및 기본 토핑, 추가 토핑 저장
        if (dto.getMenus() != null) {
            for (MenuRequestDto m : dto.getMenus()) {
                // 메뉴 이름 중복 체크 (해당 매장(store) 내에 중복 이름 있는지 검사)
                boolean exists = menuRepository.existsByStoreIdAndMenuName(savedStore.getId(), m.getMenuName());
                if (exists) {
                    throw new CustomException(StoreErrorCode._DUPLICATE_MENU_NAME);
                }
                String menuImageUrl = null;
                MultipartFile menuImageFile = m.getMenuImageFile();

                // 메뉴 이미지 업로드
                if (menuImageFile != null && !menuImageFile.isEmpty()) {
                    try {
                        Map<String, String> uploadResult = fileUtil.fileupload("storeRegist/0708", menuImageFile);
                        menuImageUrl = uploadResult.get("fileUrl"); // 전체 URL 저장
                    } catch (CustomException ce) {
                        throw ce;
                    } catch (Exception e) {
                        throw new CustomException(ErrorStatus._INTERNAL_SERVER_ERROR);
                    }
                }

                // 클라이언트에서 직접 URL 보낸 경우 사용
                if (menuImageUrl == null) {
                    throw new CustomException(ErrorStatus._FILE_REQUIRED);
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

                // 기본 토핑 저장 (메뉴 단위)
                if (m.getDefaultToppingIds() != null) {
                    for (Long toppingId : m.getDefaultToppingIds()) {
                        Topping topping = toppingRepository.getReferenceById(toppingId);
                        RamenTopping ramenTopping = new RamenTopping();
                        ramenTopping.setMenu(savedMenu);
                        ramenTopping.setTopping(topping);
                        ramenToppingRepository.save(ramenTopping);
                    }
                }
            }
        }

        // ⑤ 매장 단위 추가 토핑 저장 (dto.getExtraToppings)
        if (dto.getExtraToppings() != null && !dto.getExtraToppings().isEmpty()) {
            for (ExtraToppingRequestDto toppingDto : dto.getExtraToppings()) {
                if (toppingDto == null || toppingDto.getName() == null || toppingDto.getName().isBlank()) continue;

                String toppingName = toppingDto.getName();
                Integer price = toppingDto.getPrice() != null ? toppingDto.getPrice() : 0;

                if (defaultToppingNames.contains(toppingName)) {
                    throw new CustomException(StoreErrorCode._CANNOT_USE_DEFAULT_TOPPING);
                }

                Topping topping = toppingRepository.findByToppingName(toppingName)
                        .orElseGet(() -> toppingRepository.save(
                                Topping.builder()
                                        .toppingName(toppingName)
                                        .isActive(false)
                                        .build()
                        ));

                if (topping == null) {
                    throw new CustomException(StoreErrorCode._UNKNOWN_TOPPING_NAME);
                }

                StoreExtraTopping storeExtraTopping = new StoreExtraTopping();
                storeExtraTopping.setStore(savedStore);
                storeExtraTopping.setTopping(topping);
                storeExtraTopping.setPrice(price);
                storeExtraToppingRepository.save(storeExtraTopping);
            }
        }

        return savedStore.getId();
    }

    // ❗ 매장 수정 ❗

    @Transactional(rollbackFor = Exception.class)
    public void updateStore(Long storeId, StoreRequestDto dto, MultipartFile storeMainImage, List<MultipartFile> menuImageFiles, User user) {           Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(StoreErrorCode._STORE_NOT_FOUND));
        log.debug("storemain: {}", storeMainImage);
        log.debug("기존: {}", dto.getStoreMainImageUrl());
        if (!store.getUserId().equals(user.getId())) {
            throw new CustomException(ErrorStatus._UNAUTHORIZED);
        }

//        // 최종적으로 설정된 이미지 URL이 없으면 에러
//        if (storeMainImage == null || dto.getStoreMainImageUrl().isBlank()) {
//            throw new CustomException(ErrorStatus._FILE_REQUIRED);
//        }

        // 대표 이미지 처리
        String newStoreMainImageUrl = null;

        if (!storeMainImage.isEmpty()) {
            // 새로운 파일이 업로드된 경우
            Map<String, String> uploadResult = fileUtil.fileupload("storeUpdate", storeMainImage);
            newStoreMainImageUrl = uploadResult.get("fileUrl");
        }

        if (dto.getStoreMainImageUrl() != null
                && !dto.getStoreMainImageUrl().isBlank()
                && !dto.getStoreMainImageUrl().startsWith("data:image/")) {
            // 기존 URL 사용 (base64 data URL이 아닌 경우만)
            newStoreMainImageUrl = dto.getStoreMainImageUrl();
        }

        store.setStoreMainImageUrl(newStoreMainImageUrl);

        // 필드 업데이트
        String fullAddress = dto.getAddress();
        if (dto.getStoreDetailInput() != null && !dto.getStoreDetailInput().isEmpty()) {
            fullAddress += "," + dto.getStoreDetailInput();
        }
        store.setAddress(fullAddress);
        store.setStoreName(dto.getStoreName());
        store.setPhone(formatPhoneNumber(dto.getPhone()));
        store.setBizNum(dto.getBizNum());
        store.setIsLocalCard(dto.getIsLocalCard());
        store.setIsChildAllowed(dto.getIsChildAllowed());
        store.setHasParking(dto.getHasParking());
        store.setOwnerComment(dto.getOwnerComment());
        store.setStoreLat(dto.getStoreLat());
        store.setStoreLng(dto.getStoreLng());
        store.setOperationStatus(dto.getOperationStatus());
        store.setStoreLegalCode(dto.getStoreLegalCode() != null ? dto.getStoreLegalCode().longValue() : null);

        // 기존 스케줄 삭제 후 재저장
        scheduleRepository.deleteByStore(store);
        if (dto.getWeekSchedule() != null) {
            List<StoreWeekSchedule> schedules = dto.getWeekSchedule().stream()
                    .map(s -> {
                        StoreWeekScheduleId id = new StoreWeekScheduleId(store.getId(), s.getDayOfWeek());
                        StoreWeekSchedule schedule = new StoreWeekSchedule();
                        schedule.setId(id);
                        schedule.setIsClosedDay(s.getIsClosedDay());
                        if (Boolean.TRUE.equals(s.getIsClosedDay())) {
                            schedule.setOpeningAt(LocalTime.MIN);
                            schedule.setClosingAt(LocalTime.MIN);
                        } else {
                            schedule.setOpeningAt(s.getOpeningAt());
                            schedule.setClosingAt(s.getClosingAt());
                        }
                        schedule.setStore(store);
                        return schedule;
                    }).collect(Collectors.toList());
            scheduleRepository.saveAll(schedules);
        }

        // 메뉴 처리
        if (dto.getMenus() != null) {
            List<Long> dtoMenuIds = dto.getMenus().stream()
                    .map(MenuRequestDto::getId)
                    .filter(id -> id != null && id != 0)
                    .collect(Collectors.toList());

            List<Menu> existingMenus = menuRepository.findByStore(store);
            List<Menu> menusToRemove = existingMenus.stream()
                    .filter(menu -> !dtoMenuIds.contains(menu.getId()))
                    .collect(Collectors.toList());

            for (Menu menu : menusToRemove) {
                ramenToppingRepository.deleteByMenu(menu);
                // 기존 메뉴 이미지 파일 삭제 (있으면)
                if (menu.getMenuImageUrl() != null && !menu.getMenuImageUrl().isBlank()) {
                    try {
                        fileUtil.deleteFileFromS3(menu.getMenuImageUrl());
                    } catch (Exception e) {
                        log.warn("Failed to delete menu image file: " + menu.getMenuImageUrl(), e);
                    }
                }
                menuRepository.delete(menu);
            }

            for (int i = 0; i < dto.getMenus().size(); i++) {
                MenuRequestDto m = dto.getMenus().get(i);
                MultipartFile menuImageFile = null;
                if (menuImageFiles != null && i < menuImageFiles.size()) {
                    menuImageFile = menuImageFiles.get(i);
                }

                Menu menu;
                if (m.getId() != null && m.getId() != 0) {
                    menu = menuRepository.findById(m.getId())
                            .orElseThrow(() -> new CustomException(StoreErrorCode._MENU_NOT_FOUND));
                } else {
                    menu = new Menu();
                    menu.setStore(store);
                }

                String finalMenuImageUrl = null;
                String existingMenuImageUrl = m.getExistingMenuImageUrl();

                if (menuImageFile != null && !menuImageFile.isEmpty()) {
                    // 새 이미지가 있으면 기존 이미지 삭제 후 업로드
                    if (menu.getMenuImageUrl() != null && !menu.getMenuImageUrl().isBlank()) {
                        try {
                            fileUtil.deleteFileFromS3(menu.getMenuImageUrl());
                        } catch (Exception e) {
                            log.warn("Failed to delete existing menu image: " + menu.getMenuImageUrl(), e);
                        }
                    }
                    Map<String, String> uploadResult = fileUtil.fileupload("storeUpdate", menuImageFile);
                    finalMenuImageUrl = uploadResult.get("fileUrl");
                } else if (existingMenuImageUrl != null && !existingMenuImageUrl.isBlank()) {
                    // 새 이미지 없으면 기존 URL 유지
                    finalMenuImageUrl = existingMenuImageUrl;
                } else {
                    throw new CustomException(ErrorStatus._FILE_REQUIRED);
                }

                menu.setMenuName(m.getMenuName());
                menu.setPrice(m.getPrice());
                menu.setMenuDescription(m.getMenuDescription());
                menu.setMenuImageUrl(finalMenuImageUrl);

                Category category = new Category();
                category.setId(m.getRamenCategoryId());
                menu.setCategory(category);

                RamenSoup soup = new RamenSoup();
                soup.setId(m.getRamenSoupId());
                menu.setRamenSoup(soup);

                Menu savedMenu = menuRepository.save(menu);

                ramenToppingRepository.deleteByMenu(savedMenu);
                if (m.getDefaultToppingIds() != null) {
                    for (Long toppingId : m.getDefaultToppingIds()) {
                        Topping topping = toppingRepository.getReferenceById(toppingId);
                        RamenTopping ramenTopping = new RamenTopping();
                        ramenTopping.setMenu(savedMenu);
                        ramenTopping.setTopping(topping);
                        ramenToppingRepository.save(ramenTopping);
                    }
                }
            }
        }

        // 기존 추가 토핑 삭제 후 재저장
        storeExtraToppingRepository.deleteByStore(store);

        if (dto.getExtraToppings() != null && !dto.getExtraToppings().isEmpty()) {
            List<String> defaultToppingNames = toppingRepository.findAll().stream()
                    .filter(Topping::getIsActive)
                    .map(Topping::getToppingName)
                    .toList();

            for (ExtraToppingRequestDto toppingDto : dto.getExtraToppings()) {
                if (toppingDto == null || toppingDto.getName() == null || toppingDto.getName().isBlank()) continue;

                String toppingName = toppingDto.getName();
                Integer price = toppingDto.getPrice() != null ? toppingDto.getPrice() : 0;

                if (defaultToppingNames.contains(toppingName)) {
                    throw new CustomException(StoreErrorCode._CANNOT_USE_DEFAULT_TOPPING);
                }

                Topping topping = toppingRepository.findByToppingName(toppingName)
                        .orElseGet(() -> toppingRepository.save(
                                Topping.builder()
                                        .toppingName(toppingName)
                                        .isActive(false)
                                        .build()
                        ));

                StoreExtraTopping storeExtraTopping = new StoreExtraTopping();
                storeExtraTopping.setStore(store);
                storeExtraTopping.setTopping(topping);
                storeExtraTopping.setPrice(price);
                storeExtraToppingRepository.save(storeExtraTopping);
            }
        }

        storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public StoreRequestDto getStoreRequestDto(Long storeId, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(StoreErrorCode._STORE_NOT_FOUND));

        if (!store.getUserId().equals(user.getId())) {
            throw new CustomException(StoreErrorCode._FORBIDDEN);
        }

        List<StoreWeekSchedule> schedules = scheduleRepository.findByStoreId(storeId);
        List<StoreScheduleRequestDto> weekSchedules = schedules.stream()
                .map(StoreScheduleRequestDto::fromEntity)
                .toList();

        List<StoreExtraTopping> extraToppings = storeExtraToppingRepository.findStoreExtraToppingByStore(store);
        List<ExtraToppingRequestDto> extraToppingDtos = extraToppings.stream()
                .map(ExtraToppingRequestDto::fromEntity)
                .toList();

        // 메뉴 리스트 가져오기
        List<Menu> menus = menuRepository.findByStore(store);

        // 메뉴별 기본 토핑 ID 조회를 위한 메뉴 ID 리스트
        List<Long> menuIds = menus.stream()
                .map(Menu::getId)
                .toList();

        Map<Long, List<Long>> toppingMap = findToppingsMapByMenuIds(menuIds);

        List<MenuRequestDto> menuDtos = menus.stream()
                .map(menu -> MenuRequestDto.builder()
                        .id(menu.getId())
                        .menuName(menu.getMenuName())
                        .price(menu.getPrice())
                        .menuDescription(menu.getMenuDescription())
                        .menuImageUrl(menu.getMenuImageUrl())
                        .ramenCategoryId(menu.getCategory().getId())
                        .ramenSoupId(menu.getRamenSoup().getId())
                        .defaultToppingIds(toppingMap.getOrDefault(menu.getId(), List.of()))
                        .build())
                .toList();

        String[] addressParts = store.getAddress().split(",", 2);
        String mainAddress = addressParts[0];
        String detailAddress = addressParts.length > 1 ? addressParts[1] : null;

        return StoreRequestDto.builder()
                .id(store.getId())
                .userId(store.getUserId())
                .storeName(store.getStoreName())
                .address(mainAddress)
                .storeDetailInput(detailAddress)
                .phone(store.getPhone())
                .bizNum(store.getBizNum())
                .isLocalCard(store.getIsLocalCard())
                .isChildAllowed(store.getIsChildAllowed())
                .hasParking(store.getHasParking())
                .ownerComment(store.getOwnerComment())
                .storeMainImageUrl(store.getStoreMainImageUrl())
                .storeLat(store.getStoreLat())
                .storeLng(store.getStoreLng())
                .approvalStatus(store.getApprovalStatus())
                .operationStatus(store.getOperationStatus())
                .storeLegalCode(store.getStoreLegalCode())
                .menus(menuDtos)
                .weekSchedule(weekSchedules)
                .extraToppings(extraToppingDtos)
                .build();
    }

    // 사용자 검증 + 삭제
    @Transactional
    public void deleteStore(Long storeId, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(StoreErrorCode._STORE_NOT_FOUND));

        // 본인 확인
        if (!store.getUserId().equals(user.getId())) {
            throw new CustomException(ErrorStatus._UNAUTHORIZED);
        }

        // 연관 엔티티 삭제
        ramenToppingRepository.deleteByMenu_Store(store);
        menuRepository.deleteByStore(store);
        scheduleRepository.deleteByStore(store);
        storeExtraToppingRepository.deleteByStore(store);

        storeRepository.delete(store);
    }

    // 라멘 육수 목록 조회
    public List<RamenSoupResponseDto> getAllSoups() {
        return ramenService.getAllSoups();
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
        Map<String, Object> map = pageUtil.getPageInfo(resultPage, 5);
        map.put("registList", resultPage.getContent());
        return map;
    }

    // 등록 요청 매장 상태 변경
    public void changeStatus(Long id, ApprovalStatus status) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorStatus._DATA_NOT_FOUND));
        store.setApprovalStatus(status);
    }

    // ID로 활성화 된 매장 찾기
    public StoreDto getStore(Long storeId, Long requesterUserId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NoSuchElementException("해당 매장을 찾을 수 없습니다."));

        // 승인되지 않았고, 등록한 사용자가 아닐 경우만 막기
        if (!ApprovalStatus.APPROVED.equals(store.getApprovalStatus()) &&
                (!store.getUserId().equals(requesterUserId))) {
            throw new IllegalStateException("승인되지 않은 매장은 조회할 수 없습니다.");
        }

        return StoreDto.toDto(store);
    }

    // 매장에서 메뉴 조회
    public MenuDetailResponseDto getMenuDetail(Long storeId) {

        List<MenuDetailDto> menuList = menuRepository.findMenuDetailByStoreId(storeId);
        Map<Long, List<String>> toppingMap = ramenToppingRepository.findToppingNamesByStoreGroupedByMenuId(storeId);

        // 메뉴에 라멘토핑 맵핑
        for (MenuDetailDto dto : menuList) {
            List<String> toppings = toppingMap.getOrDefault(dto.getMenuId(), List.of());
            dto.setToppingNames(toppings);
        }

        List<String> categories = ramenService.extractUniqueCategories(menuList);
        List<String> soups = ramenService.extractUniqueSoups(menuList);
        List<String> toppings = ramenService.extractUniqueToppings(menuList);

        return MenuDetailResponseDto.builder()
                .menus(menuList)
                .categoryNames(categories)
                .soupNames(soups)
                .toppingNames(toppings)
                .build();
    }

    // ❗ 매장 삭제 ❗
    public List<StoreDto> getStoresByUserId(Long userId) {
        List<Store> stores = storeRepository.findByUserIdAndOperationStatusNot(userId, OperationStatus.CLOSED);
        return stores.stream()
                .map(StoreDto::toDto)
                .collect(Collectors.toList());
    }


    public void markStoreAsClosed(Long storeId, Long userId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(StoreErrorCode._STORE_NOT_FOUND));

        if (!store.getUserId().equals(userId)) {
            throw new CustomException(StoreErrorCode._FORBIDDEN);
        }

        store.setOperationStatus(OperationStatus.CLOSED);

        storeRepository.save(store);
    }

    public StoreRequestDto findById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 매장을 찾을 수 없습니다. id=" + id));

        return StoreRequestDto.fromEntity(store);
    }

    // 메뉴별 토핑 ID 조회
    public Map<Long, List<Long>> findToppingsMapByMenuIds(List<Long> menuIds) {
        List<Object[]> results = ramenToppingRepository.findMenuIdAndToppingIdByMenuIds(menuIds);

        Map<Long, List<Long>> toppingMap = new HashMap<>();

        for (Object[] row : results) {
            Long menuId = (Long) row[0];
            Long toppingId = (Long) row[1];

            toppingMap.computeIfAbsent(menuId, k -> new ArrayList<>()).add(toppingId);
        }

        return toppingMap;
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

    // 매장 토핑 조회
    public List<ToppingResponseDto> getStoreToppings(Long storeId) {
        Store store = em.getReference(Store.class, storeId);
        List<ToppingResponseDto> result = new ArrayList<>();
        storeExtraToppingRepository.findStoreExtraToppingByStore(store)
                .forEach(topping -> {
                    try {
                        result.add(new ToppingResponseDto(
                                        topping.getId(),
                                        topping.getTopping().getToppingName(),
                                        topping.getPrice()
                                )
                        );
                    } catch (NullPointerException e) {
                        throw new CustomException(ErrorStatus._INTERNAL_SERVER_ERROR);
                    }
                });
        return result;
    }

    public OcrToReviewDto findStoreWithMenusByBizNum(Long bizNum) {
        List<StoreIdNameDto> storeInfo = storeRepository.findIdNameByBizNum(bizNum);
        List<MenuDetailDto> menuList = menuRepository.findMenuDetailByStoreId(storeInfo.get(0).getId());
        List<ToppingResponseDto> toppingList = getStoreToppings(storeInfo.get(0).getId());
        return OcrToReviewDto.builder()
                .storeId(storeInfo.get(0).getId())
                .storeName(storeInfo.get(0).getName())
                .menuList(menuList)
                .toppingList(toppingList)
                .build();
    }

    public Long findStoreIdByBizNum(Long bizNum) {
        return storeRepository.getStoreByBizNum(bizNum);
    }

    private String formatPhoneNumber(String rawPhone) {
        if (rawPhone == null) return null;
        String digits = rawPhone.replaceAll("[^\\d]", "");
        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
        } else if (digits.length() == 10) {
            return digits.replaceFirst("(\\d{2,3})(\\d{3,4})(\\d{4})", "$1-$2-$3");
        } else {
            return rawPhone;
        }
    }

    public Long getStoreLegalCodeById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(StoreErrorCode._STORE_NOT_FOUND));

        return store.getStoreLegalCode();
    }

}