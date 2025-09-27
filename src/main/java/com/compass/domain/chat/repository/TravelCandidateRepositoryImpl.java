package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.entity.QTravelCandidate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;

@RequiredArgsConstructor
public class TravelCandidateRepositoryImpl implements TravelCandidateQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TravelCandidate> findTopActiveByRegionAndCategoryKeywords(String region,
                                                                          List<String> categoryKeywords,
                                                                          int limit) {
        QTravelCandidate candidate = QTravelCandidate.travelCandidate;

        BooleanExpression predicate = candidate.isActive.isTrue()
            .and(candidate.region.eq(region));

        BooleanExpression categoryExpression = buildCategoryExpression(candidate, categoryKeywords);
        if (categoryExpression != null) {
            predicate = predicate.and(categoryExpression);
        }

        long fetchLimit = limit > 0 ? limit : Long.MAX_VALUE;

        return queryFactory.selectFrom(candidate)
            .where(predicate)
            .orderBy(candidate.reviewCount.desc().nullsLast(), candidate.rating.desc().nullsLast())
            .limit(fetchLimit)
            .fetch();
    }

    @Override
    public List<TravelCandidate> findActiveByRegionsAndTimeBlock(List<String> regions,
                                                                 TravelCandidate.TimeBlock timeBlock,
                                                                 int limit) {
        QTravelCandidate candidate = QTravelCandidate.travelCandidate;

        BooleanExpression predicate = candidate.isActive.isTrue();

        if (!CollectionUtils.isEmpty(regions)) {
            predicate = predicate.and(candidate.region.in(regions));
        }

        if (timeBlock != null) {
            predicate = predicate.and(candidate.timeBlock.eq(timeBlock));
        }

        long fetchLimit = limit > 0 ? limit : Long.MAX_VALUE;

        return queryFactory.selectFrom(candidate)
            .where(predicate)
            .orderBy(candidate.qualityScore.desc().nullsLast(),
                candidate.reviewCount.desc().nullsLast(),
                candidate.rating.desc().nullsLast())
            .limit(fetchLimit)
            .fetch();
    }

    private BooleanExpression buildCategoryExpression(QTravelCandidate candidate,
                                                       List<String> categoryKeywords) {
        if (CollectionUtils.isEmpty(categoryKeywords)) {
            return null;
        }

        BooleanExpression expression = null;
        for (String keyword : categoryKeywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            BooleanExpression contains = candidate.category.containsIgnoreCase(keyword)
                .or(candidate.category.contains(keyword));
            expression = (expression == null) ? contains : expression.or(contains);
        }
        return expression;
    }
}
