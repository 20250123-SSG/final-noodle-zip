package noodlezip.store.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import noodlezip.store.status.ApprovalStatus;
import noodlezip.store.status.OperationStatus;
import noodlezip.store.status.ParkingType;
import noodlezip.store.entity.Store;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDto implements Serializable {
    private Long id;
    @NotNull
    private Long userId;
    @NotNull
    @Size(max = 100)
    private String storeName;
    @NotNull
    @Size(max = 255)
    private String address;
    @Size(max = 20)
    private String phone;
    @NotNull
    private Boolean isLocalCard;
    @NotNull
    private Boolean isChildAllowed;
    @NotNull
    @Size(max = 30)
    private String hasParking;
    @NotNull
    private String operationStatus;
    @NotNull
    private String approvalStatus;
    @Size(max = 300)
    private String ownerComment;
    @Size(max = 500)
    private String storeMainImageUrl;
    @NotNull
    private Double storeLat;
    @NotNull
    private Double storeLng;
    @NotNull
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer storeLegalCode;

    // Store -> StoreDto
    public static StoreDto toDto(Store store) {
        return StoreDto.builder()
                .id(store.getId())
                .userId(store.getUserId())
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .phone(store.getPhone())
                .isLocalCard(store.getIsLocalCard())
                .isChildAllowed(store.getIsChildAllowed())
                .operationStatus(store.getOperationStatus().getValue())
                .approvalStatus(store.getApprovalStatus().getValue())
                .ownerComment(store.getOwnerComment())
                .storeMainImageUrl(store.getStoreMainImageUrl())
                .storeLat(store.getStoreLat())
                .storeLng(store.getStoreLng())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .storeLegalCode(store.getStoreLegalCode())
                .build();
    }

}