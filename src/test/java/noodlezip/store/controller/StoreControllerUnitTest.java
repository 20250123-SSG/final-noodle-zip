package noodlezip.store.controller;

import noodlezip.common.util.PageUtil;
import noodlezip.store.dto.MenuDetailDto;
import noodlezip.store.dto.StoreReviewDto;
import noodlezip.store.service.StoreService;
import noodlezip.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StoreControllerUnitTest {

    private MockMvc mockMvc;
    private StoreService storeService;
    private UserRepository userRepository;
    private PageUtil pageUtil;

    @BeforeEach
    void setUp() {
        storeService = Mockito.mock(StoreService.class);
        userRepository = Mockito.mock(UserRepository.class);
        pageUtil = new PageUtil();
        StoreController storeController = new StoreController(
                storeService,
                userRepository,
                pageUtil
        );  // 생성자 주입
        mockMvc = MockMvcBuilders.standaloneSetup(storeController).build();

        // Pageable을 바인딩해주는 Resolver 등록
        mockMvc = MockMvcBuilders.standaloneSetup(storeController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void 메뉴_디테일_테스트() throws Exception {
        Long storeId = 1L;
        List<MenuDetailDto> menuList = List.of(
                MenuDetailDto.builder()
                        .menuId(1L)
                        .menuName("쇼유라멘")
                        .price(9000)
                        .menuDescription("기본 쇼유 국물")
                        .menuImageUrl("https://example.com/image.jpg")
                        .categoryName("쇼유")
                        .soupName("쇼유")
                        .toppingNames(List.of("계란", "차슈"))
                        .build()
        );

        given(storeService.getMenus(storeId)).willReturn(menuList);

        mockMvc.perform(get("/store/detail/menuList").param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menuName").value("쇼유라멘"))
                .andExpect(jsonPath("$[0].toppingNames[0]").value("계란"))
                .andExpect(jsonPath("$[0].toppingNames[1]").value("차슈"));
    }

    @Test
    void 매장_리뷰_페이징_정상응답() throws Exception {
        // given
        Long storeId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        StoreReviewDto review = StoreReviewDto.builder()
                .id(100L)
                .communityId(10L)
                .menuId(5L)
                .menuName("카라미소라멘")
                .noodleThickness(3)
                .noodleTexture(7)
                .noodleBoilLevel(5)
                .soupTemperature(8)
                .soupSaltiness(9)
                .soupSpicinessLevel(7)
                .soupOiliness(3)
                .soupFlavorKeywords("진함, 담백함")
                .content("진짜 맛있어요!")
                .reviewImageUrl("http://img.com/review.jpg")
                .isReceiptReview(true)
                .toppingNames(List.of("차슈", "계란"))
                .build();

        List<StoreReviewDto> reviewList = List.of(review);
        Page<StoreReviewDto> reviewPage = new PageImpl<>(reviewList, pageable, 1);

        Map<String, Object> expectedMap = pageUtil.getPageInfo(reviewPage, 10);

        // mock 설정
        given(storeService.getReviews(eq(storeId), any(Pageable.class)))
                .willReturn(reviewPage);

        // when & then
        mockMvc.perform(get("/store/detail/review")
                        .param("storeId", storeId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(expectedMap.get("page")))
                .andExpect(jsonPath("$.pagination.totalCount").value(expectedMap.get("totalCount")))
                .andExpect(jsonPath("$.pagination.pagePerBlock").value(expectedMap.get("pagePerBlock")))
                .andExpect(jsonPath("$.pagination.beginPage").value(expectedMap.get("beginPage")))
                .andExpect(jsonPath("$.pagination.endPage").value(expectedMap.get("endPage")))
                .andExpect(jsonPath("$.pagination.isFirst").value(expectedMap.get("isFirst")))
                .andExpect(jsonPath("$.pagination.isLast").value(expectedMap.get("isLast")))
                .andExpect(jsonPath("$.reviews[0].menuName").value("카라미소라멘"))
                .andExpect(jsonPath("$.reviews[0].toppingNames[0]").value("차슈"));
    }

}
