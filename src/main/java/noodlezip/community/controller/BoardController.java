package noodlezip.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.badge.entity.Badge;
import noodlezip.badge.publisher.BadgeEventPublisher;
import noodlezip.common.auth.MyUserDetails;
import noodlezip.common.exception.CustomException;
import noodlezip.common.status.ErrorStatus;
import noodlezip.common.util.CookieUtil;
import noodlezip.common.util.RequestParserUtil;
import noodlezip.common.validation.ValidationGroups;
import noodlezip.community.dto.BoardReqDto;
import noodlezip.community.dto.BoardRespDto;
import noodlezip.community.dto.LikeResponseDto;
import noodlezip.community.dto.*;
import noodlezip.community.entity.BoardUserId;
import noodlezip.community.service.BoardService;
import noodlezip.community.status.BoardErrorStatus;
import noodlezip.community.status.BoardSuccessStatus;
import noodlezip.community.status.CommunityType;
import noodlezip.store.dto.OcrToReviewDto;
import noodlezip.store.service.StoreService;
import noodlezip.user.status.UserErrorStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import noodlezip.community.dto.CategoryCountDto;
import noodlezip.community.dto.PopularTagDto;

@RequestMapping("/board")
@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "게시판 관리", description = "게시글 조회, 등록, 상세 보기 관련 API")
public class BoardController {

    private final BoardService boardService;
    private final RequestParserUtil requestParserUtil;
    private final StoreService storeService;

    private static final String RECENT_VIEWED_BOARDS = "recentViewedBoards";
    private static final int MAX_RECENT_BOARDS = 3;

//    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{category}/new")
    @Operation(summary = "게시글 작성 페이지", description = "사용자가 게시글 작성할 수 있는 HTML 폼 페이지 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 작성 페이지 반환 성공",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    public String registBoardView(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                  @PathVariable(name = "category") String category,
                                  @ModelAttribute ReviewInitDto reviewInitDto,
                                  Model model
    ) {
        if(myUserDetails == null) {
            throw new CustomException(ErrorStatus._UNAUTHORIZED);
        }
        CommunityType communityType = CommunityType.fromValue(category);
        if(communityType == null) {
            throw new CustomException(ErrorStatus._FORBIDDEN);
        }

        if(communityType == CommunityType.REVIEW) {
            model.addAttribute("category", CommunityType.REVIEW.getValue());
            return "receipt";
        }
        model.addAttribute("category", CommunityType.COMMUNITY.getValue());
        return "board/registBoard";
    }

    // ocr 임의 값 넘기기 위한 post get 동시 사용 베포시 삭제 예정
    @PostMapping(value = "/review/new")
    @Operation(summary = "리뷰 작성 페이지", description = "사용자가 리뷰를 작성할 수 있는 HTML 폼 페이지를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "리뷰 작성 페이지 반환 성공",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    public String review(@ModelAttribute ReviewInitDto reviewInitDto,
                         Model model) {
        if (reviewInitDto.getBizNum().isEmpty()){
            throw new CustomException(ErrorStatus._BAD_REQUEST);
        }

        Long bizNum;
        try {
            bizNum = Long.valueOf(reviewInitDto.getBizNum());
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorStatus._BAD_REQUEST);
        }

        OcrToReviewDto ocrToReviewDto = storeService.findStoreWithMenusByBizNum(bizNum);

        model.addAttribute("storeId", ocrToReviewDto.getStoreId());
        model.addAttribute("storeName", ocrToReviewDto.getStoreName());
        model.addAttribute("menuList", ocrToReviewDto.getMenuList());
        model.addAttribute("toppings", ocrToReviewDto.getToppingList());
        model.addAttribute("reviewInitDto", reviewInitDto);

        return "board/leave-review";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/registReview")
    @ResponseBody
    @Operation(summary = "리뷰 등록", description = "OCR 기반의 리뷰를 등록합니다. 로그인한 사용자만 가능하며, 메뉴별 상세 리뷰를 포함합니다.")
    @Parameters({
            @Parameter(name = "userDetails", description = "현재 로그인된 사용자 정보", hidden = true),
            @Parameter(name = "dto", description = "리뷰 등록 요청 DTO", required = true,
                    schema = @Schema(implementation = ReviewReqDto.class)),
            @Parameter(name = "bindingResult", description = "유효성 검사 결과", hidden = true)
    })
    public ResponseEntity<Map<String, Object>> registReview(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @Valid @RequestBody ReviewReqDto dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            log.error("리뷰 등록 유효성 검사 실패: {}", errorMessage);
            throw new CustomException(ErrorStatus._BAD_REQUEST);
        }

        try {
            Long boardId = boardService.saveReviewJson(dto, userDetails.getUser());
            Map<String, Object> response = new HashMap<>();
            response.put("boardId", boardId);
            return ResponseEntity.ok(response);
        } catch (CustomException e) {
            log.error("리뷰 등록 중 비즈니스 오류 발생: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("리뷰 등록 중 서버 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{category}/new")
    @Operation(summary = "게시글 등록 처리", description = "새로운 게시글을 등록합니다. 로그인한 사용자만 가능하며, 이미지 파일 첨부를 지원합니다.")
    @Parameters({
            @Parameter(name = "category", description = "등록할 게시판 정보", required = true),
            @Parameter(name = "user", description = "현재 로그인된 사용자 정보 (Spring Security에서 주입)", hidden = true),
            @Parameter(name = "boardReqDto", description = "게시글의 제목과 내용을 포함하는 요청 DTO", required = true,
                    schema = @Schema(implementation = BoardReqDto.class)),
            @Parameter(name = "bindingResult", description = "유효성 검사 결과", hidden = true),
            @Parameter(name = "boardImage", description = "게시글에 첨부할 이미지 파일 (선택 사항)", required = false,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    })
    public ResponseEntity<noodlezip.common.dto.ApiResponse<Object>> registBoard(
            @PathVariable(name = "category") String category,
            @AuthenticationPrincipal MyUserDetails user,
            @Validated(ValidationGroups.OnCreate.class) @ModelAttribute BoardReqDto boardReqDto,
            BindingResult bindingResult) {


        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            log.error("게시글 작성 유효성 검사 실패: {}", errorMessage);
            return noodlezip.common.dto.ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        }

        CommunityType communityType = CommunityType.fromValue(category);

        if (communityType == null) {
            return noodlezip.common.dto.ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        }

        boardService.registBoard(boardReqDto, user.getUser(), category);
        return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_BOARD_ADDED);
    }

    @GetMapping({"/list", "/{category}/list"})
    @Operation(summary = "게시글 목록 조회", description = "모든 게시글 또는 특정 카테고리의 게시글을 최신 순으로 페이지네이션하여 조회합니다.")
    @Parameters({
            @Parameter(name = "category", description = "조회할 게시글의 카테고리 (선택 사항). 예: 'community', 'review'", required = false, example = "community"),
            @Parameter(name = "page", description = "조회할 페이지 번호 (기본값: 0, 1부터 요청 시 내부적으로 0으로 변환)", example = "1"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 게시글 개수 (기본값: 6)", example = "6"),
            @Parameter(name = "sort", description = "정렬 기준 (예: id, createdAt). 기본값은 id,desc", example = "id,desc", hidden = true),
            @Parameter(name = "model", description = "View로 데이터를 전달하기 위한 Spring Model 객체", hidden = true)
    })
    public String boardList(
            @PathVariable(value = "category", required = false) String pathCommunityType,
            @RequestParam(value = "search", required = false) String searchKeyword,
            // 태그 검색을 위한 request 추가
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "type", required = false) String type,
            @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        pageable = pageable.withPage(pageable.getPageNumber() <= 0 ? 0 : pageable.getPageNumber() - 1);

        CommunityType communityType = CommunityType.fromValue(pathCommunityType);
        if(communityType == null) {
            communityType = CommunityType.COMMUNITY;
        }
        boolean hasSearchKeyword = searchKeyword != null && !searchKeyword.trim().isEmpty();
        boolean hasCommunityType = pathCommunityType != null && !pathCommunityType.trim().isEmpty();
        boolean hasTag = tag != null && !tag.trim().isEmpty();

        Map<String, Object> map;

        if (hasTag && "review".equals(pathCommunityType)) {
            // 리뷰 카테고리에서 태그로 필터링
            map = boardService.findReviewListByTag(tag, type, pageable);
        } else if (hasCommunityType && hasSearchKeyword) {
            map = boardService.searchBoardsByCommunityTypeAndKeyword(pathCommunityType, searchKeyword, pageable);
        } else if (hasCommunityType) {
            map = boardService.findBoardListByCategory(pathCommunityType, pageable);
        } else if (hasSearchKeyword) {
            map = boardService.searchBoards(searchKeyword, pageable);
        } else {
            map = boardService.findBoardList(pageable);
        }

        model.addAttribute("category", pathCommunityType);
        model.addAttribute("displayCategoryName", communityType.getDisplayName());
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("tag", tag);
        model.addAttribute("type", type);
        model.addAttribute("board", map.get("list"));
        model.addAttribute("page", map.get("page"));
        model.addAttribute("totalPage", map.get("totalPage"));
        model.addAttribute("beginPage", map.get("beginPage"));
        model.addAttribute("endPage", map.get("endPage"));
        model.addAttribute("isFirst", map.get("isFirst"));
        model.addAttribute("isLast", map.get("isLast"));

        return "board/list";
    }

    @GetMapping("/popular/{category}")
    public ResponseEntity<noodlezip.common.dto.ApiResponse<Object>> getPopularBoards(@PathVariable String category) {
        List<BoardRespDto> popularBoards = boardService.getPopularBoards(category);
        return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_GET_BOARD, popularBoards);
    }

    @GetMapping("{category}/detail/{id}")
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 내용을 조회하고 조회수를 증가시킵니다.")
    @Parameters({
            @Parameter(name = "category", description = "조회할 게시글의 카테고리 예: 'community', 'review'", example = "community"),
            @Parameter(name = "id", description = "조회할 게시글의 ID", required = true, example = "1"),
            @Parameter(name = "model", description = "View로 데이터를 전달하기 위한 Spring Model 객체", hidden = true)
    })
    public String getBoardDetail(
            @PathVariable("category") String category,
            @PathVariable("id") Long id,
            @AuthenticationPrincipal MyUserDetails user,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        if (id == null || id <= 0) {
            log.warn("유효하지 않은 게시글 ID로 상세 조회 시도: {}", id);
            throw new CustomException(ErrorStatus._BAD_REQUEST);
        }

        String userIdOrIp;

        if (user != null) {
            userIdOrIp = "user:" + user.getUser().getId();
        } else {
            userIdOrIp = "ip:" + requestParserUtil.getClientIp(request);
        }
        BoardRespDto board = null;
        if (category.equals("review")) {
            board = boardService.findReviewBoardById(id, userIdOrIp);
        } else {
            board = boardService.findBoardById(id, userIdOrIp);
        }
        if (board == null) {
            log.warn("존재하지 않는 게시글 ID로 상세 조회 시도: {}", id);
            throw new CustomException(ErrorStatus._DATA_NOT_FOUND);
        }

        CommunityType communityType = CommunityType.fromValue(board.getCommunityType());
        if(communityType == null) {
            communityType = CommunityType.COMMUNITY;
        }

        CookieUtil.updateRecentViewedItemsCookie(id, RECENT_VIEWED_BOARDS, MAX_RECENT_BOARDS, request, response);
        model.addAttribute("board", board);
        model.addAttribute("categoryDisplayName", communityType.getDisplayName());
        return "board/detail";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/delete/{boardId}")
    @Operation(summary = "게시글 삭제", description = "지정된 ID의 게시글을 삭제합니다. 게시글 작성자만 삭제할 수 있습니다.",
            method = "POST")
    @Parameters({
            @Parameter(name = "boardId", description = "삭제할 게시글의 ID", required = true, example = "1"),
            @Parameter(name = "user", description = "현재 로그인된 사용자 정보 (Spring Security에서 주입)", hidden = true)
    })
    public String deleteBoard(@PathVariable("boardId") Long boardId, @AuthenticationPrincipal MyUserDetails user) {
        boardService.deleteBoard(boardId, user.getUser().getId());
        return "redirect:/board/list";
    }

    @PostMapping("/like/{boardId}")
    @Operation(summary = "게시글 좋아요/취소", description = "지정된 게시글에 좋아요를 추가하거나 취소합니다. 로그인된 사용자만 가능합니다.",
            method = "POST")
    @Parameters({
            @Parameter(name = "boardId", description = "좋아요를 누를 게시글의 ID", required = true, example = "1"),
            @Parameter(name = "user", description = "현재 로그인된 사용자 정보 (Spring Security에서 주입)", hidden = true)
    })
    public ResponseEntity<noodlezip.common.dto.ApiResponse<Object>> toggleLike(@PathVariable("boardId") Long boardId, @AuthenticationPrincipal MyUserDetails user) {

        if(user == null) {
            return noodlezip.common.dto.ApiResponse.onFailure(UserErrorStatus._UNAUTHORIZED_ACCESS);
        }
        Long userId = user.getUser().getId();
        boolean isLiked = boardService.toggleLike(BoardUserId.builder().userId(userId).communityId(boardId).build());

        long totalLikes = boardService.getLikeCount(boardId);
        LikeResponseDto response = LikeResponseDto.builder().isLiked(isLiked).totalLikes(totalLikes).build();

        return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_LIKED_CHANGED, response);
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 본 게시글 목록 조회", description = "사용자 쿠키에 저장된 최근 본 게시글 ID들을 기반으로 게시글 목록을 조회하여 반환합니다.")
    public ResponseEntity<?> getRecentViewedBoards(HttpServletRequest request) {
        List<Long> recentBoardIds = CookieUtil.getRecentViewedItemIds(request, RECENT_VIEWED_BOARDS);
        return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_GET_BOARD, boardService.getBoardsByIds(recentBoardIds));
    }

    @PostMapping(value = "{category}/imageUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 업로드", description = "게시글 작성 시 사용되는 이미지 파일을 서버에 업로드하고, 업로드된 이미지들의 URL 목록을 반환합니다. 다중 파일 업로드를 지원합니다.")
    public ResponseEntity<?> uploadImage(@PathVariable(name = "category") String category, @RequestParam("uploadFiles") List<MultipartFile> uploadFiles) {
        return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_PHOTO_ADDED, boardService.uploadImages(uploadFiles));
    }

    @GetMapping("/sidebar/categories")
    @Operation(summary = "카테고리별 게시글 수 조회", description = "각 카테고리별 게시글 개수를 조회합니다.")
    public ResponseEntity<noodlezip.common.dto.ApiResponse<List<CategoryCountDto>>> getCategoryCounts() {
        try {
            List<CategoryCountDto> categoryCounts = boardService.getCategoryCounts();
            return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_GET_BOARD, categoryCounts);
        } catch (Exception e) {
            log.error("카테고리별 게시글 수 조회 중 오류 발생", e);
            return noodlezip.common.dto.ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/sidebar/popular-tags")
    @Operation(summary = "인기 태그 조회", description = "인기 태그 목록을 조회합니다.")
    public ResponseEntity<noodlezip.common.dto.ApiResponse<List<PopularTagDto>>> getPopularTags() {
        try {
            List<PopularTagDto> popularTags = boardService.getPopularTags();
            return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_GET_BOARD, popularTags);
        } catch (Exception e) {
            log.error("인기 태그 조회 중 오류 발생", e);
            return noodlezip.common.dto.ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/sidebar/widgets")
    @Operation(summary = "사이드바 위젯 데이터 조회", description = "카테고리별 게시글 개수와 인기 태그 데이터를 조회합니다.")
    public ResponseEntity<noodlezip.common.dto.ApiResponse<Map<String, Object>>> getSidebarWidgets() {
        try {
            List<CategoryCountDto> categoryCounts = boardService.getCategoryCounts();
            List<PopularTagDto> popularTags = boardService.getPopularTags();

            Map<String, Object> widgets = Map.of(
                    "categoryCounts", categoryCounts,
                    "popularTags", popularTags
            );

            return noodlezip.common.dto.ApiResponse.onSuccess(BoardSuccessStatus._OK_GET_BOARD, widgets);
        } catch (Exception e) {
            log.error("사이드바 위젯 데이터 조회 중 오류 발생", e);
            return noodlezip.common.dto.ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}