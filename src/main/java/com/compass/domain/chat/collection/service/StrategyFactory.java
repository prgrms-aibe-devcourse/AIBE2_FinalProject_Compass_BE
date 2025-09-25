package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.collection.service.strategy.FollowUpStrategy;
import com.compass.domain.chat.collection.service.strategy.MissingFieldStrategy;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StrategyFactory {

    private final MissingFieldStrategy missingFieldStrategy;

    public Optional<FollowUpStrategy> getStrategy(TravelFormSubmitRequest info) {
        if (missingFieldStrategy.findNextQuestion(info).isPresent()) {
            return Optional.of(missingFieldStrategy);
        }
        return Optional.empty();
    }
}
