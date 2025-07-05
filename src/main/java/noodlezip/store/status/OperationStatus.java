package noodlezip.store.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OperationStatus {
    OPEN("영업중"),
    CLOSED("영업종료"),
    TEMP_CLOSED("휴무일");

    private final String value;
}
