package noodlezip.savedstore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noodlezip.common.auth.MyUserDetails;
import noodlezip.savedstore.dto.request.SavedStoreCategoryUpdateRequest;
import noodlezip.savedstore.dto.response.SavedStoreApiResponse;
import noodlezip.savedstore.service.SavedStoreCategoryService;
import noodlezip.savedstore.status.SavedStoreSuccessStatus;
import noodlezip.user.entity.User;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
@Controller
public class SaveStoreCategoryController {

    private final SavedStoreCategoryService saveStoreCategoryService;


    @PostMapping(value = "/my/saved-store/categories/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateCategoryList(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                                    @RequestBody @Valid List<@Valid SavedStoreCategoryUpdateRequest> requestList) {
        User user = myUserDetails.getUser();

        saveStoreCategoryService.updateSavedCategoryList(user.getId(), requestList);
    }


    @PostMapping(value = "/my/saved-store/categories/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void deleteCategoryList(@AuthenticationPrincipal MyUserDetails myUserDetails,
                                                    @RequestBody List<Long> categoryIdList) {
        User user = myUserDetails.getUser();

        saveStoreCategoryService.deleteSavedCategoryList(user.getId(), categoryIdList);
    }

}
