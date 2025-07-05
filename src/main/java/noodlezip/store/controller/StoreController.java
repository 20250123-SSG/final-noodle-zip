package noodlezip.store.controller;

import lombok.RequiredArgsConstructor;
import noodlezip.common.auth.MyUserDetails;
import noodlezip.common.util.PageUtil;
import noodlezip.store.dto.*;
import noodlezip.store.service.StoreService;
import noodlezip.user.entity.User;
import noodlezip.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/store")
@Controller
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;
    private final PageUtil pageUtil;

    // 등록 폼 페이지 진입
    @GetMapping("/regist")
    public String showRegistPage(Model model) {
        // 카테고리와 토핑 목록을 서비스에서 가져와서 model에 담기
        model.addAttribute("categories", storeService.getRamenCategories());
        model.addAttribute("toppings", storeService.getRamenToppings());

        return "store/regist";
    }

    @PostMapping("/regist")
    public String registerStore(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                @ModelAttribute StoreRequestDto dto,
                                @RequestParam("storeMainImage") MultipartFile storeMainImage) {
        // 로그인한 유저 객체 얻기
        User user = myUserDetails.getUser();

        // 가게 등록 처리
        Long storeId = storeService.registerStore(dto, storeMainImage, user);

        return "redirect:/store/detail/" + storeId;

    }

    // 매장 상세페이지 진입
    @GetMapping("/detail.page")
    public String showDetailPage(Long no, Model model) {
        StoreDto store = storeService.getStore(no);
        model.addAttribute("store", store);
        return "store/detail";
    }

    // 매장 메뉴 조회 (비동기)
    @GetMapping("/detail/menuList")
    @ResponseBody
    public List<MenuDetailDto> showDetailMenuList(Long storeId) {
        return storeService.getMenus(storeId);
    }


    // 매장 리뷰 조회
    @GetMapping("/detail/review")
    @ResponseBody
    public StoreReviewResponseDto showStoreReviewList(
            @RequestParam Long storeId,
            Pageable pageable
    ) {

        Page<StoreReviewDto> reviews = storeService.getReviews(storeId, pageable);
        return StoreReviewResponseDto.builder()
                .reviews(reviews.getContent())
                .pagination(pageUtil.getPageInfo(reviews, 10))
                .build();
    }

}