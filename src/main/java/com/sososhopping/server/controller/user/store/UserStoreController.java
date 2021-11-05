package com.sososhopping.server.controller.user.store;

import com.sososhopping.server.common.dto.user.request.store.ToggleStoreLikeDto;
import com.sososhopping.server.common.dto.user.response.store.StoreListDto;
import com.sososhopping.server.common.error.Api404Exception;
import com.sososhopping.server.entity.member.InterestStore;
import com.sososhopping.server.entity.store.Store;
import com.sososhopping.server.common.dto.ApiResponse;
import com.sososhopping.server.common.dto.user.response.store.StoreInfoDto;
import com.sososhopping.server.repository.store.InterestStoreRepository;
import com.sososhopping.server.repository.store.StoreRepository;
import com.sososhopping.server.service.user.store.UserStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
public class UserStoreController {

    private final UserStoreService userStoreService;
    private final StoreRepository storeRepository;
    private final InterestStoreRepository interestStoreRepository;

    @GetMapping("/api/v1/stores/{storeId}")
    public ApiResponse<StoreInfoDto> getStore(@PathVariable Long storeId) {
        Store findStore = storeRepository
                .findById(storeId)
                .orElseThrow(() -> new Api404Exception("존재하지 않는 점포입니다"));

        StoreInfoDto storeInfoDto = new StoreInfoDto(findStore);
        return new ApiResponse<StoreInfoDto>(storeInfoDto);
    }

    @PostMapping("/api/v1/users/my/interest_store")
    public ResponseEntity toggleStore(
            Authentication authentication,
            @RequestBody @Valid ToggleStoreLikeDto dto
    ) {

        Long userId = Long.parseLong(authentication.getName());
        Long storeId = dto.getStoreId();

        userStoreService.toggleStoreLike(userId, storeId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(null);
    }

    @GetMapping("/api/v1/users/my/interest_store")
    public ApiResponse<StoreListDto> getInterestStores(Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());

        List<StoreListDto> dtos = interestStoreRepository.findAllByUserId(userId)
                .stream()
                .map((interestStore) -> new StoreListDto(interestStore))
                .collect(Collectors.toList());

        return new ApiResponse<StoreListDto>(dtos);
    }
}
