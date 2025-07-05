package noodlezip.search.controller;

import jakarta.transaction.Transactional;
import noodlezip.ramen.entity.*;
import noodlezip.ramen.repository.CategoryRepository;
import noodlezip.ramen.repository.RamenSoupRepository;
import noodlezip.ramen.repository.RamenToppingRepository;
import noodlezip.ramen.repository.ToppingRepository;
import noodlezip.store.entity.Menu;
import noodlezip.store.entity.Store;
import noodlezip.store.repository.MenuRepository;
import noodlezip.store.repository.StoreRepository;
import noodlezip.store.status.ApprovalStatus;
import noodlezip.store.status.OperationStatus;
import noodlezip.store.status.ParkingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private RamenSoupRepository ramenSoupRepository;
    @Autowired
    private RamenToppingRepository ramenToppingRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ToppingRepository toppingRepository;

    @Transactional
    @Test
    void 검색필터_적용_매장조회_성공() throws Exception {
        // given
         // ─────── 1. 라멘 카테고리 저장 ───────
        Category category = categoryRepository.save(
                Category.builder()
                        .categoryName("돈코츠")
                        .build()
        );

        // ─────── 2. 라멘 국물 저장 ───────
        RamenSoup soup = ramenSoupRepository.save(
                RamenSoup.builder()
                        .soupName("진한국물")
                        .build()
        );

        // ─────── 5. 토핑 저장 ───────
        Topping saveTopping = toppingRepository.save(
                Topping.builder()
                        .toppingName("계란")
                        .isActive(true)
                        .build()
        );

        // ─────── 3. 가게 저장 ───────
        Store store = Store.builder()
                .userId(1L)
                .storeName("테스트 라멘집")
                .address("서울시 테스트구")
                .phone("010-1234-5655")
                .isLocalCard(true)
                .isChildAllowed(true)
                .hasParking(ParkingType.FREE)
                .operationStatus(OperationStatus.OPEN)
                .approvalStatus(ApprovalStatus.APPROVED)
                .ownerComment("테스트용 가게입니다.")
                .storeMainImageUrl("https://example.com/image.jpg")
                .storeLat(37.5665)
                .storeLng(126.9780)
                .storeLegalCode(12401293)
                .build();
        storeRepository.save(store);

        // ─────── 4. 메뉴 저장 ───────
        Menu menu = menuRepository.save(
                Menu.builder()
                        .store(store)
                        .menuName("기본라멘")
                        .price(9000)
                        .category(category)
                        .ramenSoup(soup)
                        .build()
        );

        // ─────── 5. 메뉴-토핑 연결 ───────
        RamenTopping ramenTopping = new RamenTopping();
        ramenTopping.setMenu(menu);
        ramenTopping.setTopping(saveTopping);
        ramenToppingRepository.save(ramenTopping);


        String lat = "37.5665";
        String lng = "126.9780";
        String page = "1";
        String size = "10";

        // 필터: 돈코츠, 소유, 계란, 강남구
        String ramenCategory = "돈코츠";
        String ramenSoup = "진한국물";
        String topping = "계란";
        String region = "테스트구";

        // when & then
        mockMvc.perform(get("/search/filter")
                        .param("lat", lat)
                        .param("lng", lng)
                        .param("page", page)
                        .param("size", size)
                        .param("ramenCategory", ramenCategory)
                        .param("ramenSoup", ramenSoup)
                        .param("topping", topping)
                        .param("region", region))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.content[0].storeName").exists())
                .andExpect(jsonPath("$.content[0].distance").isNumber())
                .andDo(print());
    }

    @Transactional
    @Test
    void 대기중_매장조회_실패() throws Exception {
        // given
        // ─────── 1. 가게 저장 ───────
        Store store = Store.builder()
                .userId(1L)
                .storeName("테스트 라멘집")
                .address("서울시 테스트구")
                .phone("010-1234-5655")
                .isLocalCard(true)
                .isChildAllowed(true)
                .hasParking(ParkingType.FREE)
                .operationStatus(OperationStatus.OPEN)
                .ownerComment("테스트용 가게입니다.")
                .storeMainImageUrl("https://example.com/image.jpg")
                .storeLat(37.5665)
                .storeLng(126.9780)
                .approvalStatus(ApprovalStatus.WAITING)
                .storeLegalCode(12401293)
                .build();
        storeRepository.save(store);

        String lat = "37.5665";
        String lng = "126.9780";
        String page = "1";
        String size = "10";

        // when & then
        mockMvc.perform(get("/search/filter")
                        .param("lat", lat)
                        .param("lng", lng)
                        .param("page", page)
                        .param("size", size))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andDo(print());
    }

    @Test
    @Transactional
    void 존재하지_않는_필터조건으로_조회하면_빈리스트반환() throws Exception {
        // given
        // (더미 매장 저장은 생략 - storeId 등 없어도 되는 조회만 확인)
        // 데이터가 없는 조건으로 필터링
        String lat = "37.5665";
        String lng = "126.9780";
        String ramenCategory = "없는카테고리";
        String ramenSoup = "없는국물";
        String topping = "없는토핑";
        String region = "없는지역";

        // when & then
        mockMvc.perform(get("/search/filter")
                        .param("lat", lat)
                        .param("lng", lng)
                        .param("ramenCategory", ramenCategory)
                        .param("ramenSoup", ramenSoup)
                        .param("topping", topping)
                        .param("region", region)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andDo(print());
    }

    private Store createDummyStore() {
        return storeRepository.save(Store.builder()
                .userId(1L)
                .storeName("더미 라멘집")
                .address("서울시 강남구")
                .phone("010-1111-1111")
                .isLocalCard(true)
                .isChildAllowed(true)
                .hasParking(ParkingType.FREE)
                .operationStatus(OperationStatus.OPEN)
                .ownerComment("맛집")
                .storeMainImageUrl("url")
                .storeLat(37.5665)
                .storeLng(126.9780)
                .approvalStatus(ApprovalStatus.APPROVED)
                .storeLegalCode(11110)
                .build());
    }

    private void createDummyMenuWith(Store store, String categoryName, String soupName, String toppingName, boolean toppingActive) {
        Category category = categoryRepository.save(Category.builder().categoryName(categoryName).build());
        RamenSoup soup = ramenSoupRepository.save(RamenSoup.builder().soupName(soupName).build());
        Menu menu = menuRepository.save(Menu.builder()
                .store(store)
                .menuName("라멘")
                .price(8000)
                .category(category)
                .ramenSoup(soup)
                .build());
        Topping topping = toppingRepository.save(Topping.builder().toppingName(toppingName).isActive(toppingActive).build());
        RamenTopping ramenTopping = new RamenTopping();
        ramenTopping.setMenu(menu);
        ramenTopping.setTopping(topping);
        ramenToppingRepository.save(ramenTopping);
    }

    @Test @Transactional
    void 전체_필터X_조회() throws Exception {
        createDummyMenuWith(createDummyStore(), "돈코츠", "진한국물", "차슈", true);
        mockMvc.perform(get("/search/filter")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThan(0)))
                .andDo(print());
    }

    @Test @Transactional
    void 국물만_필터() throws Exception {
        createDummyMenuWith(createDummyStore(), "쇼유", "맑은국물", "계란", true);
        mockMvc.perform(get("/search/filter")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("ramenSoup", "맑은국물")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeName").exists())
                .andDo(print());
    }

    @Test @Transactional
    void 필터_일치X_빈결과() throws Exception {
        mockMvc.perform(get("/search/filter")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("ramenSoup", "없는국물")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andDo(print());
    }

    @Test @Transactional
    void 지역명_포함검색() throws Exception {
        createDummyMenuWith(createDummyStore(), "미소", "깔끔", "버터", true);
        mockMvc.perform(get("/search/filter")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("region", "강남")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeName").exists())
                .andDo(print());
    }

    @Test @Transactional
    void 비활성_토핑_검색시_제외됨() throws Exception {
        createDummyMenuWith(createDummyStore(), "쇼유", "진한국물", "비활성토핑", false);
        mockMvc.perform(get("/search/filter")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("topping", "비활성토핑")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andDo(print());
    }

    @Test @Transactional
    void 다중_토핑_필터_적용() throws Exception {
        Store store = createDummyStore();
        createDummyMenuWith(store, "돈코츠", "진한", "계란", true);
        createDummyMenuWith(store, "돈코츠", "진한", "차슈", true);
        mockMvc.perform(get("/search/filter")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("topping", "계란")
                        .param("topping", "차슈")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andDo(print());
    }

}