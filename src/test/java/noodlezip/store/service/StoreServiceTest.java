package noodlezip.store.service;

import jakarta.persistence.EntityManager;
import noodlezip.common.util.FileUtil;
import noodlezip.common.util.PageUtil;
import noodlezip.ramen.repository.RamenReviewRepository;
import noodlezip.ramen.repository.RamenToppingRepository;
import noodlezip.ramen.repository.ReviewToppingRepository;
import noodlezip.ramen.repository.ToppingRepository;
import noodlezip.ramen.service.RamenService;
import noodlezip.store.dto.MenuDetailDto;
import noodlezip.store.dto.StoreReviewDto;
import noodlezip.store.repository.MenuRepository;
import noodlezip.store.repository.StoreRepository;
import noodlezip.store.repository.StoreWeekScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
                reviewToppingRepository
        );
    }

    @Test
    void 매장_메뉴_조회() {
        // given
        Long storeId = 1L;
        List<MenuDetailDto> menus = List.of(
                MenuDetailDto.builder()
                        .menuId(10L)
                        .menuName("쇼유라멘")
                        .build()
        );

        Map<Long, List<String>> toppingMap = Map.of(10L, List.of("계란", "차슈"));

        given(menuRepository.findMenuDetailByStoreId(storeId)).willReturn(menus);
        given(ramenToppingRepository.findToppingNamesByStoreGroupedByMenuId(storeId)).willReturn(toppingMap);

        // when
        List<MenuDetailDto> result = storeService.getMenus(storeId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getToppingNames()).containsExactly("계란", "차슈");
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
}