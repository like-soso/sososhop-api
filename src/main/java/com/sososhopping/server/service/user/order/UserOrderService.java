package com.sososhopping.server.service.user.order;

import com.sososhopping.server.common.dto.user.request.order.OrderCreateDto;
import com.sososhopping.server.common.error.Api400Exception;
import com.sososhopping.server.common.error.Api404Exception;
import com.sososhopping.server.entity.coupon.Coupon;
import com.sososhopping.server.entity.coupon.UserCoupon;
import com.sososhopping.server.entity.member.User;
import com.sososhopping.server.entity.member.UserPoint;
import com.sososhopping.server.entity.member.UserPointLog;
import com.sososhopping.server.entity.orders.Order;
import com.sososhopping.server.entity.orders.OrderItem;
import com.sososhopping.server.entity.orders.OrderStatus;
import com.sososhopping.server.entity.orders.OrderType;
import com.sososhopping.server.entity.store.Item;
import com.sososhopping.server.entity.store.Store;
import com.sososhopping.server.repository.coupon.CouponRepository;
import com.sososhopping.server.repository.coupon.UserCouponRepository;
import com.sososhopping.server.repository.member.UserPointRepository;
import com.sososhopping.server.repository.order.OrderRepository;
import com.sososhopping.server.repository.store.ItemRepository;
import com.sososhopping.server.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserOrderService {

    private final StoreRepository storeRepository;
    private final CouponRepository couponRepository;
    private final UserPointRepository userPointRepository;
    private final UserCouponRepository userCouponRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final EntityManager em;

    @Transactional
    public void createOrder(User user, OrderCreateDto dto) {

        Store findStore = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new Api404Exception("존재하지 않는 점포입니다"));

        // 물품 검증
        List<Long> itemIds = dto.getOrderItems()
                .stream()
                .map(orderItemDto -> orderItemDto.getItemId())
                .collect(Collectors.toList());

        List<Item> requestedItems = itemRepository.findByIdIn(itemIds);

        boolean areValidItemIds = (itemIds.size() == requestedItems.size());
        boolean canBeProvidedBy = requestedItems.stream()
                .allMatch(item -> item.canBeProvidedBy(findStore));

        if (!areValidItemIds || !canBeProvidedBy) {
            throw new Api400Exception("구매할 수 없는 물품이 포함되어있습니다");
        }

        // 주문 가격
        int orderPrice = dto.getOrderItems()
                .stream()
                .mapToInt(orderItemDto ->
                        em.find(Item.class, orderItemDto.getItemId()).getPrice() *
                                orderItemDto.getQuantity()
                ).sum();

        // 유저 포인트 검증
        UserPoint userPoint = null;

        Integer usedPoint = Optional
                .ofNullable(dto.getUsedPoint())
                .orElse(0);
        if (usedPoint > 0) {
            userPoint = userPointRepository
                    .findByUserAndStore(user, findStore)
                    .orElseThrow(() -> new Api400Exception("포인트가 없습니다"));

            if (!userPoint.hasMoreThan(usedPoint)) {
                throw new Api400Exception("포인트가 부족합니다");
            }

            userPoint.usePoint(usedPoint);
        }

        // 쿠폰 검증
        Integer couponDiscountPrice = 0;
        Coupon findCoupon = null;
        if (dto.getCouponId() != null) {
            findCoupon = couponRepository
                    .findById(dto.getCouponId())
                    .orElseThrow(() -> new Api404Exception("존재하지 않는 쿠폰입니다"));

            if (!findCoupon.belongsTo(findStore)) {
                throw new Api400Exception("해당 점포의 쿠폰이 아닙니다");
            }

            if (findCoupon.minimumPriceGreaterThan(orderPrice)) {
                throw new Api400Exception("주문 금액이 쿠폰 최소주문금액보다 적습니다");
            }

            couponDiscountPrice = findCoupon.getDiscountPrice(orderPrice);

            UserCoupon userCoupon = userCouponRepository
                    .findByUserAndCoupon(user, findCoupon)
                    .orElseThrow(() -> new Api404Exception("유저에게 쿠폰이 없습니다"));
            userCoupon.useCoupon();
        }

        // 최종 가격 계산
        int finalPrice = orderPrice - usedPoint - couponDiscountPrice;

        if (finalPrice < 0) {
            throw new Api400Exception("최종금액은 0원 이상이어야 합니다");
        }

        // 주문 완료
        Order order = Order.builder()
                .user(user)
                .ordererName(dto.getOrdererName())
                .ordererPhone(dto.getOrdererPhone())
                .orderType(dto.getOrderType())
                .visitDate(dto.getVisitDate())
                .store(findStore)
                .storeName(findStore.getName())
                .deliveryStreetAddress(dto.getDeliveryStreetAddress())
                .deliveryDetailedAddress(dto.getDeliveryDetailedAddress())
                .paymentType(dto.getPaymentType())
                .orderPrice(orderPrice)
                .usedPoint(usedPoint)
                .coupon(findCoupon)
                .finalPrice(finalPrice)
                .orderStatus(OrderStatus.PENDING)
                .build();

        dto.getOrderItems()
                .forEach(orderItemDto -> {
                    Item item = em.find(Item.class, orderItemDto.getItemId());
                    OrderItem orderItem = OrderItem.builder()
                            .order(order)
                            .item(item)
                            .quantity(orderItemDto.getQuantity())
                            .totalPrice(item.getPrice() * orderItemDto.getQuantity())
                            .build();

                    orderItem.setOrder(order);
                });

        em.persist(order);

        // TODO: 포인트 추가 (사장 족으로 이동)
//        if (findStore.hasPointPolicy()) {
//            userPoint = userPointRepository.findByUserAndStore(user, findStore)
//                    .orElse(new UserPoint(user, findStore, 0));
//
//            int savedPoint = (int)(findStore.getSaveRate().doubleValue() / 100 * finalPrice);
//            userPoint.savePoint(savedPoint);
//            em.persist(userPoint);
//        }

    }

    public List<Order> getOrders(User user, OrderStatus status) {
        return orderRepository.findOrderListByUserAndOrderStatus(user, status);
    }
}
