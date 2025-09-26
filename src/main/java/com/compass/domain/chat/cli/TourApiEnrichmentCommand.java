package com.compass.domain.chat.cli;

import com.compass.domain.chat.service.TourApiEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * CLI 용도로 Tour API 보강을 실행하는 커맨드
 * 실행 시점: --cli.tour-enrich=true 프로퍼티를 전달한 경우
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cli.tour-enrich", havingValue = "true")
public class TourApiEnrichmentCommand implements CommandLineRunner {

    private final TourApiEnrichmentService tourApiEnrichmentService;
    private final ApplicationContext applicationContext;

    @Value("${cli.tour-enrich.mode:all}")
    private String mode;

    @Override
    public void run(String... args) {
        log.info("Tour API CLI 보강 실행 시작 - mode={}", mode);

        try {
            int updated = switch (mode) {
                case "attractions" -> tourApiEnrichmentService.enrichTouristAttractions();
                case "all" -> tourApiEnrichmentService.enrichAllWithTourApi();
                default -> {
                    log.warn("지원하지 않는 모드: {} (기본값 all을 사용합니다)", mode);
                    yield tourApiEnrichmentService.enrichAllWithTourApi();
                }
            };

            log.info("Tour API 보강 완료 - 업데이트된 레코드: {}", updated);
        } catch (Exception e) {
            log.error("Tour API 보강 실행 중 오류", e);
            SpringApplication.exit(applicationContext, () -> 1);
            return;
        }

        SpringApplication.exit(applicationContext, () -> 0);
    }
}
