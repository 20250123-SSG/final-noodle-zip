package noodlezip.community.service;

import noodlezip.community.dto.CommentReqDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface CommentService {

    Map<String, Object> findCommentList(Long boardId, Long userId, Pageable pageable);

    Map<String, Object> findCommentListByUserId(Long userId, Pageable pageable);

    Map<String, Object> registComment(CommentReqDto commentReqDto);

    Map<String, Object> deleteComment(Long commentId, Long boardId, Long userId);
}
