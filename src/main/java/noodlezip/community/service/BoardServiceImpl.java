package noodlezip.community.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.badge.publisher.BadgeEventPublisher;
import noodlezip.common.exception.CustomException;
import noodlezip.common.status.ErrorStatus;
import noodlezip.community.dto.BoardReqDto;
import noodlezip.community.dto.BoardRespDto;
import noodlezip.community.dto.MenuReviewDto;
import noodlezip.community.dto.ReviewReqDto;
import noodlezip.community.dto.CategoryCountDto;
import noodlezip.community.dto.PopularTagDto;
import noodlezip.community.entity.Board;
import noodlezip.community.entity.BoardUserId;
import noodlezip.community.entity.CommunityActiveStatus;
import noodlezip.community.entity.Like;
import noodlezip.community.repository.BoardRepository;
import noodlezip.common.util.FileUtil;
import noodlezip.common.util.PageUtil;
import noodlezip.community.repository.LikeRepository;

import noodlezip.ramen.entity.*;
import noodlezip.ramen.repository.RamenReviewRepository;
import noodlezip.ramen.repository.ReviewToppingRepository;
import noodlezip.store.dto.StoreReviewDto;
import noodlezip.store.entity.Menu;
import noodlezip.store.entity.StoreExtraTopping;
import noodlezip.store.repository.MenuRepository;
import noodlezip.store.repository.StoreExtraToppingRepository;
import noodlezip.user.entity.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoardServiceImpl implements BoardService {

    private final BadgeEventPublisher eventPublisher;
    private final BoardRepository boardRepository;
    private final PageUtil pageUtil;
    private final ModelMapper modelMapper;
    private final FileUtil fileUtil;
    private final ViewCountService viewCountService;
    private final LikeRepository likeRepository;
    private final RamenReviewRepository ramenReviewRepository;
    private final ReviewToppingRepository reviewToppingRepository;
    private final StoreExtraToppingRepository storeExtraToppingRepository;
    private final MenuRepository menuRepository;
    private final RamenReviewRepository rareReviewRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findBoardList(Pageable pageable) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardWithPagination(pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);

        map.put("list", boardPage.getContent());

        return map;
    }


    @Override
    public List<Board> findMostLikedBoardList() {
        List<Board> boardList = boardRepository.findTop3ByPostStatusOrderByLikesCountDesc(CommunityActiveStatus.POSTED);

        return boardList;
    }

    @Override
    public List<Board> findMostViewedBoardList() {
        List<Board> boardList = boardRepository.findTop3ByPostStatusOrderByLikesCountDesc(CommunityActiveStatus.POSTED);

        return boardList;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findBoardListByCategory(String category, Pageable pageable) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardByCommunityTypeWithPagination(category, pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);

        map.put("list", boardPage.getContent());
        map.put("category", category);

        return map;
    }


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findBoardLikedByCategory(Long userId,
                                                        List<Long> boardIdList,
                                                        String category,
                                                        Pageable pageable
    ) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardsByIdsAndStatusPostedWithPaging(
                boardIdList, category, pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);
        map.put("list", boardPage.getContent());

        return map;
    }


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findBoardByUser(Long userId,  Pageable pageable) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardByWriterAndCommunityTypeWithPagination(
                userId, null, pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);
        map.put("list", boardPage.getContent());

        return map;
    }


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findBoardByUserByCategory(Long userId, String category, Pageable pageable) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardByWriterAndCommunityTypeWithPagination(
                userId, category, pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);
        map.put("list", boardPage.getContent());

        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchBoardsByCommunityTypeAndKeyword(String category, String keyword, Pageable pageable) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardByCommunityTypeAndKeywordWithPagination(category, keyword,pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);

        map.put("list", boardPage.getContent());
        map.put("category", category);

        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchBoards(String keyword, Pageable pageable) {
        Page<BoardRespDto> boardPage = boardRepository.findBoardByKeywordWithPagination(keyword,pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, 5);

        map.put("list", boardPage.getContent());

        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public BoardRespDto findBoardById(Long id, String userIdOrIp) {

        String[] infoAndIdOrIp = userIdOrIp.split(":");

        boolean isLike = false;

        if (infoAndIdOrIp[0].equals("user")) {
            isLike = likeRepository.existsById(BoardUserId.builder().userId(Long.parseLong(infoAndIdOrIp[1])).communityId(id).build());
        }

        BoardRespDto boardRespDto = boardRepository.findBoardByBoardIdWithUser(id).orElseThrow( () -> new CustomException(ErrorStatus._DATA_NOT_FOUND));

        String sanitizedContentHtml = boardRespDto.getContent();

        Document doc = Jsoup.parse(sanitizedContentHtml);

        sanitizedContentHtml = Jsoup.clean(doc.body().html(), Safelist.relaxed());

        boardRespDto.setContent(sanitizedContentHtml);

        if (boardRespDto == null) {
            throw new CustomException(ErrorStatus._DATA_NOT_FOUND);
        } else {
            boardRespDto.setIsLike(isLike);
        }

        viewCountService.increaseViewCount(TargetType.BOARD, id, userIdOrIp);

        return boardRespDto;
    }

    @Override
    @Transactional(readOnly = true)
    public BoardRespDto findReviewBoardById(Long id, String userIdOrIp) {

        String[] infoAndIdOrIp = userIdOrIp.split(":");

        boolean isLike = false;

        if (infoAndIdOrIp[0].equals("user")) {
            isLike = likeRepository.existsById(BoardUserId.builder().userId(Long.parseLong(infoAndIdOrIp[1])).communityId(id).build());
        }

        BoardRespDto boardRespDto = boardRepository.findBoardByBoardIdWithUser(id).orElseThrow( () -> new CustomException(ErrorStatus._DATA_NOT_FOUND));

        String sanitizedContentHtml = boardRespDto.getContent();

        Document doc = Jsoup.parse(sanitizedContentHtml);

        sanitizedContentHtml = Jsoup.clean(doc.body().html(), Safelist.relaxed());

        boardRespDto.setContent(sanitizedContentHtml);

        List<StoreReviewDto> list = ramenReviewRepository.findReviewsByBoardId(id);
        boardRespDto.setMenuReviews(list);
        boardRespDto.setIsLike(isLike);

        viewCountService.increaseViewCount(TargetType.BOARD, id, userIdOrIp);

        return boardRespDto;
    }

    @Override
    public void registBoard(BoardReqDto boardReqDto, User user, String category) {
        Board board = modelMapper.map(boardReqDto, Board.class);
        board.setCommunityType(category);
        board.setPostStatus(CommunityActiveStatus.POSTED);
        board.setUser(user);
        boardRepository.save(board);
        eventPublisher.publishCommunityPostBadgeEvent(user);
    }

    @Override
    public void deleteBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new CustomException(ErrorStatus._DATA_NOT_FOUND));

        if(!board.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorStatus._FORBIDDEN);
        }
        if ("review".equalsIgnoreCase(board.getCommunityType())) {

            List<Long> ramenReviewIds = ramenReviewRepository.findIdsByBoardId(boardId);

            if (!ramenReviewIds.isEmpty()) {
                reviewToppingRepository.deleteByRamenReviewIdIn(ramenReviewIds);
            }
            ramenReviewRepository.deleteByBoardId(boardId);
        }
        boardRepository.delete(board);
    }

    @Override
    @Retryable(value = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class}, maxAttempts = 5)
    public boolean toggleLike(BoardUserId boardUserId) {
        Board board = boardRepository.findById(boardUserId.getCommunityId())
                .orElseThrow(() -> new CustomException(ErrorStatus._DATA_NOT_FOUND));

        boolean isLiked;
        Optional<Like> existingLike = likeRepository.findById(boardUserId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            board.setLikesCount(board.getLikesCount() - 1);
            isLiked = false;
        } else {
            Like newLike = Like.builder()
                    .id(boardUserId)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(newLike);
            board.setLikesCount(board.getLikesCount() + 1);
            isLiked = true;

            eventPublisher.publishCommunityLikeBadgeEvent(board);
        }

        boardRepository.save(board);
        return isLiked;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getLikeCount(Long boardId) {
        return boardRepository.findById(boardId)
                .map(Board::getLikesCount)
                .orElseThrow(() -> new CustomException(ErrorStatus._DATA_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> getBoardsByIds(List<Long> BoardIds) {
        return boardRepository.findAllById(BoardIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardRespDto> getPopularBoards(String category) {
        List<BoardRespDto> boardRespDtos =  boardRepository.findPopularBoards(category);
        boardRespDtos.forEach(boardRespDto -> {
            String originalContentHtml = boardRespDto.getContent();

            Safelist customSafelist = Safelist.relaxed();

            customSafelist.removeTags("img");

            String sanitizedContentHtml = Jsoup.clean(originalContentHtml, customSafelist);

            boardRespDto.setContent(sanitizedContentHtml);
        });
        return boardRespDtos;
    }


    @Override
    public List<Map<String, String>> uploadImages(List<MultipartFile> uploadFiles) {
        List<Map<String, String>> imageInfos = new ArrayList<>();
        if (uploadFiles != null && !uploadFiles.isEmpty()) {

            for (MultipartFile image : uploadFiles) {
                if (!image.isEmpty() && image.getOriginalFilename() != null && !image.getOriginalFilename().trim().isEmpty()) {
                    Map<String, String> map = fileUtil.fileupload("temp", image);
                    imageInfos.add(map);
                } else {
                    log.error("image is empty");
                }
            }
        } else {
            log.error("upload images is empty");
        }
        return imageInfos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryCountDto> getCategoryCounts() {
        return boardRepository.findCategoryCounts();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryCountDto> getCategoryCountsByUser(Long userId) {
        return boardRepository.findCategoryCountsByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryCountDto> getCategoryCountsByBoardIds(List<Long> boardIdList) {
        return boardRepository.findCategoryCountsByBoardIds(boardIdList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularTagDto> getPopularTags() {
        return boardRepository.findPopularTags();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findReviewListByTag(String tag, String type, Pageable pageable) {
        // Board 엔티티를 기반으로 태그별 리뷰 게시글 조회
        Page<BoardRespDto> boardPage = boardRepository.findReviewBoardsByTag(tag, type, pageable);

        Map<String, Object> map = pageUtil.getPageInfo(boardPage, pageable.getPageSize());
        map.put("list", boardPage.getContent());
        map.put("category", "review");

        return map;
    }

    @Override
    public List<Long> getBoardIdByUserLiked(Long userId) {
        return likeRepository.findCommunityIdsByUserId(userId);
    }


    @Override
    @Transactional
    public Long saveReviewJson(ReviewReqDto dto, User user) {

        if (dto == null) {
            throw new CustomException(ErrorStatus._DATA_NOT_FOUND);
        }

        Board board = new Board();
        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        board.setOcrKeyHash(dto.getOcrKeyHash());
        board.setReviewVisitDate(LocalDate.parse(dto.getVisitDate()));
        board.setUser(user);
        board.setCommunityType("review");
        board.setPostStatus(CommunityActiveStatus.POSTED);
        board.setImageUrl(dto.getImageUrl());
        System.out.println("storeId" + dto.getStoreId());
        board.setReviewStoreId(dto.getStoreId());
        boardRepository.save(board);

        List<Long> reviewIds = new ArrayList<>();

        for (MenuReviewDto r : dto.getReviews()) {
            Menu menu = menuRepository.findById(r.getMenuId())
                    .orElseThrow(() -> new CustomException(ErrorStatus._DATA_NOT_FOUND));

            RamenReview review = new RamenReview();
            review.setCommunityId(board.getId());
            review.setMenu(menu);
            review.setNoodleThickness(r.getNoodleThickness());
            review.setNoodleTexture(r.getNoodleTexture());
            review.setNoodleBoilLevel(r.getNoodleBoiledLevel());
            review.setSoupDensity(r.getSoupThickness());
            review.setSoupTemperature(r.getSoupTemperature());
            review.setSoupSaltiness(r.getSoupSaltiness());
            review.setSoupSpicinessLevel(r.getSoupSpiciness());
            review.setSoupOiliness(r.getSoupOiliness());
            review.setSoupFlavorKeywords(r.getSoupFlavorKeywords());
            review.setContent(r.getContent());
            review.setIsReceiptReview(dto.getIsReceiptReview());
            review.setReviewImageUrl(r.getImageUrl());
            ramenReviewRepository.save(review);

            // 토핑 처리
            if (r.getToppingIds() != null) {
                for (Long toppingId : r.getToppingIds()) {
                    StoreExtraTopping storeExtraTopping = storeExtraToppingRepository.getReferenceById(toppingId);
                    ReviewTopping rTopping = new ReviewTopping();
                    rTopping.setId((new ReviewToppingId(review.getId(),toppingId)));
                    rTopping.setRamenReview(review);
                    rTopping.setStoreExtraTopping(storeExtraTopping);
                    reviewToppingRepository.save(rTopping);
                }
            }

            reviewIds.add(review.getId());
        }

        eventPublisher.publishRamenReviewBadgeEvent(user, dto);

        return board.getId();
    }

}
