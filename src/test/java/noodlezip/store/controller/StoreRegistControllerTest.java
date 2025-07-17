package noodlezip.store.controller;

import noodlezip.common.auth.MyUserDetails;
import noodlezip.store.dto.MenuRequestDto;
import noodlezip.store.dto.StoreRequestDto;
import noodlezip.store.service.StoreService;
import noodlezip.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreRegistControllerTest {

    @InjectMocks
    private StoreRegistController controller;

    @Mock
    private StoreService storeService;

    @Test
    void registerStore_메뉴와_이미지포함_등록성공() {
        // given
        StoreRequestDto dto = new StoreRequestDto();
        MenuRequestDto menuDto = new MenuRequestDto();
        dto.setMenus(List.of(menuDto));

        MultipartFile storeMainImage = mock(MultipartFile.class);
        MultipartFile menuImageFile = mock(MultipartFile.class);
        List<MultipartFile> menuImageFiles = List.of(menuImageFile);

        User user = User.builder().id(1L).build();
        MyUserDetails userDetails = new MyUserDetails(user);

        when(storeService.registerStore(any(StoreRequestDto.class), any(User.class))).thenReturn(42L);

        // when
        Map<String, Object> result = controller.registerStore(userDetails, dto, storeMainImage, menuImageFiles);

        // then
        assertEquals(42L, result.get("storeId"));

        assertEquals(menuImageFile, dto.getMenus().get(0).getMenuImageFile());
        verify(storeService).registerStore(any(StoreRequestDto.class), eq(user));
    }
}
