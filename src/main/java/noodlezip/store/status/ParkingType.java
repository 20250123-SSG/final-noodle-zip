package noodlezip.store.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ParkingType {
    FREE("무료주차"),
    PAID("유료주차"),
    NOT_AVAILABLE("주차불가");

    private final String value;
}
