package noodlezip.mypage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.common.auth.MyUserDetails;
import noodlezip.community.dto.CategoryCountDto;
import noodlezip.community.service.BoardService;
import noodlezip.community.status.CommunityType;
import noodlezip.mypage.constant.MyCommunityType;
import noodlezip.mypage.dto.UserAccessInfo;
import noodlezip.mypage.dto.response.MyBoardListPageResponse;
import noodlezip.mypage.dto.response.MyCommentListPageResponse;
import noodlezip.mypage.service.MyCommunityService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
@Controller
public class MyCommunityController extends MyBaseController {

    private final MyCommunityService myCommunityService;
    private final BoardService boardService;


    @Operation(
            summary = "사용자가 작성한 게시글 목록 조회",
            description = "사용자 마이페이지에서 해당 사용자가 작성한 게시글을 카테고리별로 조회합니다."
    )
    @Tag(name = "마이페이지", description = "미이페이지 연동 API")
    @GetMapping({
            "/{userId}/boards",
            "/{userId}/boards/{communityType}",
    })
    public String getMyPostBoards(
            @AuthenticationPrincipal MyUserDetails myUserDetails,
            @PathVariable Long userId,
            @PathVariable(required = false) String communityType,
            @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        pageable = pageable.withPage(pageable.getPageNumber() <= 0 ? 0 : pageable.getPageNumber() - 1);

        CommunityType community = CommunityType.fromValue(communityType);
        if (community == null) {
            community = CommunityType.COMMUNITY;
        }
        UserAccessInfo userAccessInfo = resolveUserAccess(myUserDetails, userId);
        MyBoardListPageResponse boardList = myCommunityService.getMyPostBoardListPage(userId, communityType, pageable);
        List<CategoryCountDto> communityTypeList = boardList.getCommunityTypeList();

        model.addAttribute("boardList", boardList);
        model.addAttribute("communityTypeList", communityTypeList);
        model.addAttribute("userAccessInfo", userAccessInfo);
        model.addAttribute("path", MyCommunityType.POST_BOARD.getPath());
        model.addAttribute("communityType", communityType);
        model.addAttribute("pathDisplayName", MyCommunityType.POST_BOARD.getDisplayName());
        model.addAttribute("communityTypeDisplayName", community.getDisplayName());

        return "mypage/myBoard";
    }


    @Operation(
            summary = "사용자가 좋아요한 게시글 목록 조회",
            description = "사용자 마이페이지에서 해당 사용자가 좋아요한 카테고리별로 조회합니다."
    )
    @Tag(name = "마이페이지", description = "미이페이지 연동 API")
    @GetMapping({
            "/{userId}/liked-boards",
            "/{userId}/liked-boards/{communityType}"
    })
    public String getMyLikedBoards(
            @AuthenticationPrincipal MyUserDetails myUserDetails,
            @PathVariable Long userId,
            @PathVariable(required = false) String communityType,
            @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        pageable = pageable.withPage(pageable.getPageNumber() <= 0 ? 0 : pageable.getPageNumber() - 1);

        CommunityType community = CommunityType.fromValue(communityType);
        if (community == null) {
            community = CommunityType.COMMUNITY;
        }
        UserAccessInfo userAccessInfo = resolveUserAccess(myUserDetails, userId);
        MyBoardListPageResponse boardList = myCommunityService.getMyLikedBoardListPage(userId, communityType, pageable);
        List<CategoryCountDto> communityTypeList = boardList.getCommunityTypeList();

        model.addAttribute("boardList", boardList);
        model.addAttribute("communityTypeList", communityTypeList);
        model.addAttribute("userAccessInfo", userAccessInfo);
        model.addAttribute("path", MyCommunityType.LIKED_BOARD.getPath());
        model.addAttribute("communityType", communityType);
        model.addAttribute("pathDisplayName", MyCommunityType.LIKED_BOARD.getDisplayName());
        model.addAttribute("communityTypeDisplayName", community.getDisplayName());

        return "mypage/myBoard";
    }


    @Operation(
            summary = "사용자가 작성한 댓글 목록 조회",
            description = "사용자 마이페이지에서 해당 사용자가 작성한 댓글을 페이지 단위로 조회합니다."
    )
    @GetMapping("{userId}/comments")
    public String getMyComments(
            @AuthenticationPrincipal MyUserDetails myUserDetails,
            @PathVariable Long userId,
            @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        pageable = pageable.withPage(pageable.getPageNumber() <= 0 ? 0 : pageable.getPageNumber() - 1);
        UserAccessInfo userAccessInfo = resolveUserAccess(myUserDetails, userId);
        MyCommentListPageResponse commentList = myCommunityService.getPostCommentListPage(userId, pageable);

        model.addAttribute("commentList", commentList);
        model.addAttribute("userAccessInfo", userAccessInfo);
        model.addAttribute("pathDisplayName", MyCommunityType.POST_COMMENT.getDisplayName());

        return "mypage/myComment";
    }


    @Operation(
            summary = "내가 작성한 게시글 페이지에서 게시글 바로 삭제",
            description = "내가 작성한 게시글 조회 목록에서 삭제합니다. 게시글 작성자만 삭제할 수 있습니다.",
            method = "POST")
    @Parameters({
            @Parameter(name = "boardId", description = "삭제할 게시글의 ID", required = true, example = "1"),
            @Parameter(name = "user", description = "현재 로그인된 사용자 정보 (Spring Security에서 주입)", hidden = true)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/my/{boardId}/delete")
    @ResponseBody
    public void deleteBoardFromMyPage(@PathVariable("boardId") Long boardId,
                                      @AuthenticationPrincipal MyUserDetails user) {
        boardService.deleteBoard(boardId, user.getUser().getId());
    }

}
