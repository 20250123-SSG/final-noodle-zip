package noodlezip.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class RegistListDto {

    private Long id;
    private String loginId;
    private String storeName;
    private String createdAt;
}
