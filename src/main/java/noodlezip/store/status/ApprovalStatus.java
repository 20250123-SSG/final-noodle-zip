package noodlezip.store.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ApprovalStatus {
    WAITING("대기"),
    APPROVED("승인"),
    REJECTED("거절");

    private final String value;
}
