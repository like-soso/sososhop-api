package com.sososhopping.server.controller.user.store;

import com.sososhopping.server.common.error.Api404Exception;
import com.sososhopping.server.entity.store.Store;
import com.sososhopping.server.common.dto.ApiResponse;
import com.sososhopping.server.common.dto.user.response.store.StoreInfoDto;
import com.sososhopping.server.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class UserStoreController {

    private final StoreRepository storeRepository;

    @GetMapping("/api/v1/stores/{storeId}")
    public ApiResponse<StoreInfoDto> getStore(@PathVariable Long storeId) {
        Store findStore = storeRepository
                .findById(storeId)
                .orElseThrow(() -> new Api404Exception("존재하지 않는 점포입니다"));

        StoreInfoDto storeInfoDto = new StoreInfoDto(findStore);
        return new ApiResponse<StoreInfoDto>(storeInfoDto);
    }
}
