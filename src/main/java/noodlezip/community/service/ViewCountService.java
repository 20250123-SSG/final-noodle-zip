package noodlezip.community.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.common.redis.RedisRepository;
import noodlezip.community.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ViewCountService {

    private final RedisRepository redisRepository;
    private final BoardRepository boardRepository;

    // redis 접두사 및 만료 시간 세팅
    private static final String TOTAL_VIEW_KEY_SUFFIX = ":views:";
    private static final String VIEWED_CHECK_KEY_SUFFIX = ":viewed:";
    private static final String BOARD_TOTAL_VIEW_KEY_PREFIX = "board" + TOTAL_VIEW_KEY_SUFFIX;
    private static final Pattern BOARD_ID_REGEX = Pattern.compile(BOARD_TOTAL_VIEW_KEY_PREFIX + "(\\d+)");
    // TTL value
    private static final Integer DUPLICATE_BOARD_VIEW_CHECK_TTL = 1;

    public static String getTotalViewKeySuffix() {
        return TOTAL_VIEW_KEY_SUFFIX;
    }

    public static String getViewedCheckKeySuffix() {
        return VIEWED_CHECK_KEY_SUFFIX;
    }

    public static Integer getDuplicateBoardViewCheckTtl() {
        return DUPLICATE_BOARD_VIEW_CHECK_TTL;
    }

    public void increaseViewCount(TargetType type,Long targetId, String userIdOrIp) {

        String userViewedCheckKey = type.getValue() + VIEWED_CHECK_KEY_SUFFIX + targetId + ":" + userIdOrIp;

        if (redisRepository.setIfAbsent(userViewedCheckKey, "1", DUPLICATE_BOARD_VIEW_CHECK_TTL, TimeUnit.HOURS)) {
            String totalViewCheckKey = type.getValue() + TOTAL_VIEW_KEY_SUFFIX + targetId;
            Long viewedCount = redisRepository.increase(totalViewCheckKey);
            log.debug("viewedCount: {}", viewedCount);
            log.debug("totalViewCheckKey: {}", totalViewCheckKey);
        } else {
            log.debug("User {} viewed check already exists", userIdOrIp);
        }
    }

    public Long getViewCount(TargetType type, Long targetId) {
        String totalViewCheckKey = type.getValue() + TOTAL_VIEW_KEY_SUFFIX + targetId;

        return Long.getLong(redisRepository.get(totalViewCheckKey).orElseThrow(() -> new RuntimeException("totalViewCheckKey not found")));
    }

    public int syncBoardViewCountsToDB() {

        Set<String> viewCountKeys = redisRepository.getKeys(TargetType.BOARD.getValue() + ":views:");

        if (viewCountKeys == null || viewCountKeys.isEmpty()) {
            log.info("Redis에 동기화할 게시글 조회수 데이터가 없습니다.");
            return 0;
        }

        int updatedCount = 0;
        for (String key : viewCountKeys) {
            System.out.println(key);
            try {
                Matcher matcher = BOARD_ID_REGEX.matcher(key);
                if(matcher.find()) {
                    Long boardId = Long.parseLong(matcher.group(1));


                    Optional<String> viewCountDeltaStrOpt = redisRepository.get(key);

                    if (viewCountDeltaStrOpt.isEmpty()) {
                        log.debug("Redis 키 '{}'에 해당하는 값이 이미 없습니다. 스킵합니다.", key);
                        redisRepository.delete(key);
                        continue;
                    }

                    Long viewCountDelta = Long.parseLong(viewCountDeltaStrOpt.get());

                    boardRepository.increaseViewCount(boardId, viewCountDelta);
                    updatedCount++;

                    redisRepository.delete(key);
                }else {
                    log.warn("유효하지 않은 Redis 조회수 키 패턴 발견: {}. Redis에서 삭제합니다.", key);
                    redisRepository.delete(key);
                }

            } catch (NumberFormatException e) {
                log.error("Redis 키 '{}'의 값이 유효한 숫자가 아닙니다. (값: {}) 키를 삭제합니다. 오류: {}", key, redisRepository.get(key).orElse("N/A"), e.getMessage());
                redisRepository.delete(key);
            } catch (Exception e) {
                log.error("Board ID: {} 조회수 DB 반영 중 오류 발생: {}", key, e.getMessage(), e);
            }
        }
        return updatedCount;
    }

}
