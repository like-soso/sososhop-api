package com.sososhopping.server.repository.store;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sososhopping.server.entity.member.InterestStore;
import com.sososhopping.server.entity.member.User;

import javax.persistence.EntityManager;
import java.util.List;

import static com.sososhopping.server.entity.member.QInterestStore.*;
import static com.sososhopping.server.entity.store.QStore.*;

public class UserInterestStoreRepositoryImpl implements UserInterestStoreRepository {

    private final JPAQueryFactory queryFactory;

    public UserInterestStoreRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<InterestStore> findAllByUserId(Long userId) {
        return queryFactory
                .select(interestStore)
                .from(interestStore)
                .join(interestStore.store, store).fetchJoin()
                .where(userIdEq(userId))
                .fetch();
    }

    private BooleanExpression userIdEq(Long userId) {
        if (userId == null) {
            return null;
        }
        return interestStore.user.id.eq(userId);
    }
}
