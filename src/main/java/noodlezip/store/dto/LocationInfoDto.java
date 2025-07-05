package noodlezip.store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfoDto {
    private Double storeLat;
    private Double storeLng;
    private Long storeLegalCode;
}
// 서버 응답용