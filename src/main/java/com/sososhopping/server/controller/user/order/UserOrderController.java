package com.sososhopping.server.controller.user.order;

import com.sososhopping.server.common.dto.ApiListResponse;
import com.sososhopping.server.common.dto.user.request.order.ChangeOrderStatusDto;
import com.sososhopping.server.common.dto.user.request.order.OrderCreateDto;
import com.sososhopping.server.common.dto.user.response.order.OrderDetailDto;
import com.sososhopping.server.common.dto.user.response.order.OrderListDto;
import com.sososhopping.server.common.error.Api400Exception;
import com.sososhopping.server.common.error.Api401Exception;
import com.sososhopping.server.entity.member.User;
import com.sososhopping.server.entity.orders.Order;
import com.sososhopping.server.entity.orders.OrderStatus;
import com.sososhopping.server.repository.member.UserRepository;
import com.sososhopping.server.service.user.order.UserOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static com.sososhopping.server.entity.orders.OrderStatus.*;

@RestController
@RequiredArgsConstructor
public class UserOrderController {

    private final UserOrderService userOrderService;
    private final UserRepository userRepository;

    @PostMapping("/api/v1/users/orders")
    public ResponseEntity createOrder(
            Authentication authentication,
            @RequestBody @Valid OrderCreateDto orderCreateDto
    ) {

        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Api401Exception("Invalid Token"));

        userOrderService.createOrder(user, orderCreateDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(null);
    }

    @PostMapping("/api/v1/users/orders/{orderId}")
    public void changeOrderStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody @Valid ChangeOrderStatusDto dto
    ) {
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Api401Exception("Invalid Token"));

        OrderStatus action = dto.getAction();

        if (action != CANCEL && action != DONE) {
            throw new Api400Exception("알 수 없는 요청입니다");
        }

        if (action == CANCEL) {
            userOrderService.cancelOrder(user, orderId);
        } else if (action == DONE) {
            userOrderService.confirmOrder(user, orderId);
        }
    }

    @GetMapping("/api/v1/users/my/orders")
    public ApiListResponse<OrderListDto> getOrders(
            Authentication authentication,
            @RequestParam OrderStatus status
    ) {
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Api401Exception("Invalid Token"));

        List<OrderListDto> dtos = userOrderService.getOrders(user, status)
                .stream()
                .map(order -> new OrderListDto((order)))
                .collect(Collectors.toList());

        return new ApiListResponse<OrderListDto>(dtos);
    }

    @GetMapping("/api/v1/users/my/orders/{orderId}")
    public OrderDetailDto getOrderDetail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Api401Exception("Invalid Token"));

        Order order = userOrderService.getOrderDetail(user, orderId);
        return new OrderDetailDto(order);
    }
}
