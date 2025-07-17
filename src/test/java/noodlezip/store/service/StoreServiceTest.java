package noodlezip.store.service;

import jakarta.persistence.EntityManager;
import noodlezip.common.exception.CustomException;
import noodlezip.common.util.FileUtil;
import noodlezip.common.util.PageUtil;
import noodlezip.ramen.dto.ToppingResponseDto;
import noodlezip.ramen.entity.Topping;
import noodlezip.ramen.repository.RamenReviewRepository;
import noodlezip.ramen.repository.RamenToppingRepository;
import noodlezip.ramen.repository.ReviewToppingRepository;
import noodlezip.ramen.repository.ToppingRepository;
import noodlezip.ramen.service.RamenService;
import noodlezip.store.dto.MenuRequestDto;
import noodlezip.store.dto.StoreRequestDto;
import noodlezip.store.dto.StoreReviewDto;
import noodlezip.store.entity.Menu;
import noodlezip.store.entity.Store;
import noodlezip.store.entity.StoreExtraTopping;
import noodlezip.store.repository.MenuRepository;
import noodlezip.store.repository.StoreExtraToppingRepository;
import noodlezip.store.repository.StoreRepository;
import noodlezip.store.repository.StoreWeekScheduleRepository;
import noodlezip.store.status.OperationStatus;
import noodlezip.store.status.ParkingType;
import noodlezip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock private StoreWeekScheduleRepository scheduleRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private RamenToppingRepository ramenToppingRepository;
    @Mock private RamenService ramenService;
    @Mock private ModelMapper modelMapper;
    @Mock private PageUtil pageUtil;
    @Mock private ToppingRepository toppingRepository;
    @Mock private FileUtil fileUtil;
    @Mock private EntityManager em;
    @Mock private RamenReviewRepository ramenReviewRepository;
    @Mock private ReviewToppingRepository reviewToppingRepository;
    @Mock private StoreExtraToppingRepository storeExtraToppingRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(
                storeRepository,
                scheduleRepository,
                menuRepository,
                ramenToppingRepository,
                ramenService,
                modelMapper,
                pageUtil,
                toppingRepository,
                fileUtil,
                em,
                ramenReviewRepository,
                reviewToppingRepository,
                storeExtraToppingRepository
        );
    }

    @Test
    void getReviews_정상조회_토핑까지포함() {
        // given
        Long storeId = 1L;
        Pageable pageable = PageRequest.of(0, 5);

        // 리뷰 DTO 예시 데이터
        StoreReviewDto review1 = StoreReviewDto.builder()
                .id(100L)
                .communityId(1L)
                .menuId(10L)
                .menuName("카라미소라멘")
                .noodleThickness(3)
                .noodleTexture(7)
                .noodleBoilLevel(5)
                .soupTemperature(8)
                .soupSaltiness(9)
                .soupSpicinessLevel(7)
                .soupOiliness(3)
                .soupFlavorKeywords("진한, 깔끔")
                .content("맛있어요!")
                .reviewImageUrl("http://image.com/review1.jpg")
                .isReceiptReview(true)
                .build();

        List<StoreReviewDto> reviews = List.of(review1);
        Page<StoreReviewDto> pageResult = new PageImpl<>(reviews, pageable, 1);

        // 리뷰ID → 토핑 이름 매핑
        Map<Long, List<String>> toppingMap = Map.of(
                100L, List.of("차슈", "계란")
        );

        // mocking
        given(ramenReviewRepository.findReviewsByStoreId(storeId, pageable))
                .willReturn(pageResult);

        given(reviewToppingRepository.findToppingNamesByReviewIds(List.of(100L)))
                .willReturn(toppingMap);

        // when
        Page<StoreReviewDto> result = storeService.getReviews(storeId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        StoreReviewDto resultDto = result.getContent().get(0);
        assertThat(resultDto.getId()).isEqualTo(100L);
        assertThat(resultDto.getToppingNames()).containsExactly("차슈", "계란");
        assertThat(resultDto.getMenuName()).isEqualTo("카라미소라멘");
        assertThat(resultDto.getIsReceiptReview()).isTrue();
    }

    @Test
    void getStoreToppings_정상동작() throws Exception {
        // given
        Long storeId = 1L;
        Store store = new Store();
        when(em.getReference(Store.class, storeId)).thenReturn(store);

        StoreExtraTopping toppingEntity = mock(StoreExtraTopping.class);
        Topping topping = mock(Topping.class);
        when(toppingEntity.getId()).thenReturn(10L);
        when(toppingEntity.getTopping()).thenReturn(topping);
        when(topping.getToppingName()).thenReturn("계란");
        when(toppingEntity.getPrice()).thenReturn(2000);

        when(storeExtraToppingRepository.findStoreExtraToppingByStore(store))
                .thenReturn(java.util.List.of(toppingEntity));

        // when
        java.util.List<ToppingResponseDto> result = storeService.getStoreToppings(storeId);

        // then
        assertEquals(1, result.size());
        assertEquals("계란", result.get(0).getName());
        assertEquals(2000, result.get(0).getPrice());
    }

    @Test
    void getStoreToppings_NPE발생시_CustomException() throws Exception {
        // given
        Long storeId = 1L;
        Store store = new Store();
        when(em.getReference(Store.class, storeId)).thenReturn(store);

        StoreExtraTopping toppingEntity = mock(StoreExtraTopping.class);
        when(toppingEntity.getId()).thenReturn(10L);
        when(toppingEntity.getTopping()).thenReturn(null); // NPE 유발

        when(storeExtraToppingRepository.findStoreExtraToppingByStore(store))
                .thenReturn(java.util.List.of(toppingEntity));

        // when & then
        assertThrows(CustomException.class, () -> storeService.getStoreToppings(storeId));
    }

    @Test
    void registerStore_정상등록_메뉴1개_이미지포함() {
        // given
        StoreRequestDto dto = new StoreRequestDto();
        dto.setStoreName("테스트라멘");
        dto.setAddress("서울시 중구");
        dto.setPhone("010-1234-5678");
        dto.setBizNum(1234567890L);
        dto.setIsLocalCard(true);
        dto.setIsChildAllowed(true);
        dto.setHasParking(ParkingType.FREE);
        dto.setOwnerComment("사장님 멘트");
        dto.setStoreLat(37.123);
        dto.setStoreLng(127.456);
        dto.setOperationStatus(OperationStatus.OPEN);

        MultipartFile storeMainImage = mock(MultipartFile.class);
        when(storeMainImage.isEmpty()).thenReturn(false);
        dto.setStoreMainImage(storeMainImage);

        when(fileUtil.fileupload(anyString(), eq(storeMainImage)))
                .thenReturn(Map.of("fileUrl", "https://your-bucket.s3.amazonaws.com/store-main.jpg"));

        MenuRequestDto menuDto = new MenuRequestDto();
        menuDto.setMenuName("카라미소라멘");
        menuDto.setPrice(11000);
        menuDto.setMenuDescription("매콤한 미소라멘");
        menuDto.setRamenCategoryId(1);
        menuDto.setRamenSoupId(1);

        MultipartFile menuImage = mock(MultipartFile.class);
        when(menuImage.isEmpty()).thenReturn(false);
        menuDto.setMenuImageFile(menuImage);

        when(fileUtil.fileupload(anyString(), eq(menuImage)))
                .thenReturn(Map.of("fileUrl", "https://your-bucket.s3.amazonaws.com/menu1.jpg"));

        dto.setMenus(List.of(menuDto));

        when(toppingRepository.findAll()).thenReturn(List.of());

        Store savedStore = Store.builder()
                .id(1L)
                .storeName("테스트라멘")
                .userId(10L)
                .build();
        when(storeRepository.save(any())).thenReturn(savedStore);

        Menu savedMenu = Menu.builder()
                .id(100L)
                .store(savedStore)
                .menuName("카라미소라멘")
                .price(11000)
                .build();
        when(menuRepository.save(any())).thenReturn(savedMenu);

        User user = User.builder().id(10L).build();

        // when
        Long result = storeService.registerStore(dto, user);

        // then
        assertThat(result).isEqualTo(1L);
        verify(fileUtil).fileupload(anyString(), eq(storeMainImage));
        verify(fileUtil).fileupload(anyString(), eq(menuImage));
        verify(menuRepository).save(any());
        verify(ramenToppingRepository, never()).save(any());
    }
}